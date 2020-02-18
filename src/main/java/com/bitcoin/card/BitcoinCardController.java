package com.bitcoin.card;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.apache.commons.io.IOUtils;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.bitcoin.card.error.UnauthorizedException;
import com.bitcoin.card.error.UserNotFoundException;
import com.bitcoin.card.error.WrongFileTypeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.apache.log4j.Logger;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", 
methods = {RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS})
@RestController
public class BitcoinCardController {
	
	private final Logger LOGGER = Logger.getLogger(this.getClass());
	
	TokenHandler th = new TokenHandler();
	
	CognitoHelper helper = new CognitoHelper();

	//private static String url = "jdbc:postgresql://3.136.241.73:5432/bitcoin-card?user=postgres&password=bch_admin&ssl=true&sslmode=verify-ca&sslrootcert=./.postgres/root.crt";

    private static String url = "jdbc:postgresql://bitcoincom-card.cgll0kqdznrn.us-east-2.rds.amazonaws.com:5432/bitcoincard1?user=bch_admin&password=letitsnow890*()&ssl=false";
	private static Connection conn;

    @GetMapping("/cognito")
	public User getUserInfo() {

    	
    	/*AmazonCognitoIdentity identityClient = new AmazonCognitoIdentityClient(new AnonymousAWSCredentials());
		
    	// send a get id request. This only needs to be executed the first time
    	// and the result should be cached.
    	GetIdRequest idRequest = new GetIdRequest();
    	idRequest.setAccountId("7ljdp8s6flj41urv5eohbngef8");
    	idRequest.setIdentityPoolId("us-east-1_G7ighQ8h3");
    	// If you are authenticating your users through an identity provider
    	// then you can set the Map of tokens in the request
    	// Map providerTokens = new HashMap();
    	// providerTokens.put("graph.facebook.com", "facebook session key");
    	// idRequest.setLogins(providerTokens);
    				
    	GetIdResult idResp = identityClient.getId(idRequest);
    				
    	String identityId = idResp.getIdentityId();
    	
    	System.out.println("Identity id is" + identityId);
		 */
    	
    	
	       AWSCognitoIdentityProvider cognitoClient = getAmazonCognitoIdentityClient();             
	       AdminGetUserRequest userRequest = new AdminGetUserRequest()
	                      .withUsername("mcboatface2")
	                      .withUserPoolId("us-east-1_G7ighQ8h3");
	 
	 
	       AdminGetUserResult userResult = cognitoClient.adminGetUser(userRequest);
	 
	       User userResponse = new User();
	       userResponse.setUsername(userResult.getUsername());
	       
	       System.out.println("MFA Settings " + userResult.toString());
	       //userResponse.setUserStatus(userResult.getUserStatus());
	       //userResponse.setUserCreateDate(userResult.getUserCreateDate());
	       //userResponse.setLastModifiedDate(userResult.getUserLastModifiedDate());
	 
	 /*      List userAttributes = userResult.getUserAttributes();
	       for(AttributeTypeattribute: userAttributes) {
	              if(attribute.getName().equals("custom:companyName")) {
	                 userResponse.setCompanyName(attribute.getValue());
	}else if(attribute.getName().equals("custom:companyPosition")) {
	                 userResponse.setCompanyPosition(attribute.getValue());
	              }else if(attribute.getName().equals("email")) {
	                 userResponse.setEmail(attribute.getValue());
	   
	              }
	       }
	 */
	        
	       return userResponse;
	              
	}
	
	public AWSCognitoIdentityProvider getAmazonCognitoIdentityClient() {
	      ClasspathPropertiesFileCredentialsProvider propertiesFileCredentialsProvider = 
	           new ClasspathPropertiesFileCredentialsProvider();
	      
	 
	       return AWSCognitoIdentityProviderClientBuilder.standard()
	                      .withCredentials(propertiesFileCredentialsProvider)
	                             .withRegion("us-east-1")
	                             .build();
	 
	   }
	
    // Login user
    @PostMapping(value = "/login", consumes = "*/*")
    @ResponseStatus(HttpStatus.OK)
    AccessToken loginUser(@RequestBody Login l) 
    {
    	AccessToken a = new AccessToken();
    	String accessToken;
    	
    	accessToken = helper.ValidateUser(l.getUsername(), l.getPassword());
    	
    	a.setAccess_token(accessToken);
    		
    	return a;
    }
    
    // Verify user's access code so user can be confirmed in Cognito
    @PostMapping(value = "/verify", consumes = "*/*")
    @ResponseStatus(HttpStatus.OK)
    boolean verifyAccessCode(@RequestBody VerifyAccessCode v) 
    {
    	boolean result;
    	
    	result = helper.VerifyAccessCode(v.getUsername(), v.getCode());
    	
    	if (! result)
    		throw new UnauthorizedException("Failed to verify access code: " + v.getCode());
    	
    		
    	return result;
    }
	
    // Save
    @PostMapping(value = "/users", consumes = "*/*")
    @ResponseStatus(HttpStatus.CREATED)
    void newUser(@RequestBody User u, @RequestHeader(name = "authorization") Optional<String> authorization) throws SQLException {
    	String sql = "insert into users (first_name, last_name, email, phone_number, date_of_birth, gender, is_active, promotional_consent" +
    	", address_street, address_city, address_postal_code, address_state, address_country, default_currency_id, social_security_number" +
    			", user_name, address_street_2, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())";

    	LOGGER.info("Adding new user to database...");
    	LOGGER.info("User data: \n" + u.toString());

    	//String username = th.decodeVerifyCognitoToken(authorization);
    	
		if (conn == null)
			conn = DriverManager.getConnection(url);
		
    	PreparedStatement stmt = conn.prepareStatement(sql);  
    	stmt.setString(1,u.getFirstName());
    	stmt.setString(2,u.getLastName());
    	stmt.setString(3, u.getEmail());
    	stmt.setString(4, u.getPhoneNumber());
    	stmt.setString(5, u.getDateOfBirth());
    	stmt.setString(6, u.getGender());
    	stmt.setBoolean(7, u.isActive());
    	stmt.setBoolean(8, u.isPromotioanlConsent());
    	stmt.setString(9, u.getAddresStreet());
    	stmt.setString(10, u.getAddressCity());
    	stmt.setString(11, u.getAddressPostalCode());
    	stmt.setString(12, u.getAddressState());
    	stmt.setString(13, u.getAddressCountry());
    	stmt.setString(14, u.getDefaultCurrencyId());
    	stmt.setString(15, u.getSocialSecurityNumber());
    	stmt.setString(16, u.getUsername());
    	stmt.setString(17, u.getAddresStreet2());
    	
    	LOGGER.info("Executing insert statement...");
    	stmt.execute();
    	LOGGER.info("Inserted new user in database.");
    	
    	// Create new user in Cognito
    	LOGGER.info("Creating Cognito user...");
    	boolean cognitoResult = helper.SignUpUser(u.getUsername(), u.getPassword(), u.getEmail(), u.getPhoneNumber());
		
    	if (! cognitoResult)
    	{
    		LOGGER.info("Failed to create Cognito user + " + u.getUsername());
        	stmt = conn.prepareStatement("delete from users where user_name = ?");  
        	stmt.setString(1, u.getUsername());
		    stmt.execute();
		    throw new SQLException("Failed to create Cognito user + " + u.getUsername());
    	}

    }
    
    /*
    @GetMapping("/get-text")
    public @ResponseBody String getText() throws Exception {
    	
		if (conn == null)
			conn = DriverManager.getConnection(url);
    	
        final InputStream in = getClass().getResourceAsStream("/card.jpg");

    	PreparedStatement ps = conn.prepareStatement("INSERT INTO user_documents (user_id, document) VALUES (?, ?)");
    	ps.setInt(1, 2);
    	ps.setBinaryStream(2, in);
    	ps.execute();
    	ps.close();

    	return "Hello world";
    }

    
    @GetMapping("/get-image")
    public @ResponseBody byte[] getImage() throws IOException {
        final InputStream in = getClass().getResourceAsStream("/card.jpg");

        return IOUtils.toByteArray(in);
    }

    @GetMapping(value = "/get-image-with-media-type", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getImageWithMediaType() throws IOException {
        final InputStream in = getClass().getResourceAsStream("/card.jpg");
        return IOUtils.toByteArray(in);
    }
*/
    
    @GetMapping(value = "/get-file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getFile() throws IOException {
    	System.out.println("MEHMED getting file...");
    	
    	File f = new File("/db_backup.sh");
        System.out.println("File path: " + f.getAbsolutePath());
    	
        final InputStream in = getClass().getResourceAsStream("C:\\projects\\bitcoincom-svc-cardapi\\db_backup.sh");
        
        System.out.println("Input stream is " + in);
        return IOUtils.toByteArray(in);
    }
    
    @PostMapping("/users/upload-document")
    public String uploadFile(@RequestParam("file") MultipartFile file, 
			 @RequestParam("id") int id, 
			 @RequestParam("documenttype") String documentType    						 
    		) throws SQLException, IOException {
    	
    	LOGGER.info("Uploading file " + file.getOriginalFilename() + " for user " + id + ".");
    	
    	String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().indexOf('.') + 1);
    	

    	if (! fileExtension.equalsIgnoreCase("PDF") && ! fileExtension.equalsIgnoreCase("JPG") && ! fileExtension.equalsIgnoreCase("PNG"))
    	{
    		System.out.println("Throwing exception!!!");
    	
    		throw new WrongFileTypeException(fileExtension);
    	}
    	
		if (conn == null)
			conn = DriverManager.getConnection(url);
    	
        final InputStream in = file.getInputStream();
        
    	PreparedStatement ps = conn.prepareStatement("INSERT INTO user_documents (user_id, document_name, document_type, document) VALUES (?, ?, ?, ?)");
    	ps.setInt(1, id);
    	ps.setString(2, file.getOriginalFilename());
    	ps.setString(3, documentType.toUpperCase());
    	ps.setBinaryStream(4, in);
    	ps.execute();
    	ps.close();
    	LOGGER.info("Uploaded.");
    
    	return file.getName();
    }
 
    @GetMapping(value = "/users/{id}/user-document/{documentType}")
    public @ResponseBody ResponseEntity<byte[]> getUserDocument(@PathVariable int id, @PathVariable String documentType, @RequestHeader(name = "authorization") Optional<String> authorization) throws Exception {
    	
    	LOGGER.info("Retrieving user document for user " + id);
    	
    	String username = th.decodeVerifyCognitoToken(authorization);

		if (conn == null)
			conn = DriverManager.getConnection(url);
    
    	PreparedStatement ps = conn.prepareStatement("select document_name, document from user_documents where user_id = ? and document_type = ?");
    	ps.setInt(1, id);
    	ps.setString(2, documentType.toUpperCase());
    	ResultSet rs = ps.executeQuery();

    	rs.next();
    	
    	String file = rs.getString(1);
    	
    	String fileExtension = file.substring(file.indexOf('.') + 1);


    	InputStream is = rs.getBinaryStream(2);

        HttpHeaders responseHeaders = new HttpHeaders();
        
        if (fileExtension.equalsIgnoreCase("PDF"))
            responseHeaders.set("Content-Type", "application/pdf");
        else
        	responseHeaders.set("Content-Type", "image/jpeg");
        
    	LOGGER.info("Retrieved.");
    	
        return ResponseEntity.ok()
        	      .headers(responseHeaders)
        	      .body(IOUtils.toByteArray(is));
    
    	//return IOUtils.toByteArray(is);
    }
    
    @GetMapping(value = "/virtual-card", produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[]  getVirtualCardImage(@PathVariable int id, @RequestHeader(name = "authorization") Optional<String> authorization) throws Exception {
    	
    	
    	LOGGER.info("Retrieving user virtual card for user ");

    	String username = th.decodeVerifyCognitoToken(authorization);

    	
    	BitcoinRestClient brClient = new BitcoinRestClient();
    	
    	String ternioImageUrl = brClient.getTernioImageURL();
    	
		URL url = new URL(ternioImageUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
    	LOGGER.info("Retrieved.");

    	
    	return IOUtils.toByteArray(conn.getInputStream());
    }
    
    // Find
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/search")
    User findUser(@RequestParam Optional<String> username, @RequestParam Optional<String> email, @RequestHeader(name = "authorization") Optional<String> authorization) {
   
    	String userN = th.decodeVerifyCognitoToken(authorization);

    	LOGGER.info("Getting user details...");
    	String conditionStr = "";
    	
    	if (username.isPresent())
    		conditionStr = "user_name = '" + username.get();
    	else
    		if (email.isPresent())
    			conditionStr = "email = '" + email.get();
    		else
	    		throw new UserNotFoundException("Invalid parameter, email or username has to be provided.");

    	LOGGER.info("User query parameter : " + conditionStr);
    	
    	User u = new User();
    	
    	try {
    		if (conn == null)
    			conn = DriverManager.getConnection(url);
    		
    		System.out.println("select * from users where " + conditionStr);
    		
    		Statement s = conn.createStatement();
    		ResultSet r = s.executeQuery("select * from users where " + conditionStr + "'");
    		
    		setUserResultParameters(r, u, username.orElse(email.orElse("No username or email")));
    		
    	} catch (SQLException e) {
    		
	    	LOGGER.info("Exception!!!\n" + e.getMessage());

    		e.printStackTrace();
    	}
    	
    	LOGGER.info("Retrieved user data: \n" + u.toString());
    	
         return u;
    }
    
    // Find
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/transactions")
    String findUserLedgerTransactions(@RequestHeader(name = "authorization") Optional<String> authorization) {
    	    	
    	String username = th.decodeVerifyCognitoToken(authorization);
    	    	
    	LOGGER.info("Getting user transactions...");
    
    	BitcoinRestClient brClient = new BitcoinRestClient();
    /*	
    	User u = new User();
    	u.setFirstName("MEHMED");
    	u.setLastName("DURIC");
    	u.setEmail("sssddddff@yahoo.com");
    	u.setAddresStreet("111 Clarke Rd");
    	u.setAddressCity("Richmond");
    	u.setAddressState("VA");
    	u.setAddressPostalCode("23233");
    	u.setAddressCountry("US");
    	u.setPhoneNumber("+1 310 867 5323");
    	u.setUserName("sssdddd4duser");
    	u.setSocialSecurityNumber("012345672");
    	u.setDateOfBirth("1969-12-31");
    	
    	brClient.createTernioUser(u);
	*/
    	return brClient.getTernioLedgerTransactions("");
		
    }
    

    // Save or update
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/users")
    void saveOrUpdate(@RequestBody User u, @RequestHeader(name = "authorization") Optional<String> authorization) throws SQLException {
    	
    	String username = th.decodeVerifyCognitoToken(authorization);
    	
    	LOGGER.info("Updating user data...");

    	LOGGER.info("User data: \n" + u.toString());
    	
    	if (u.getId() == null)
    		throw new UserNotFoundException("Missing user id");

    	
    	String sql = "update users set ";
 
		if (conn == null)
			conn = DriverManager.getConnection(url);
		
		if (u.getFirstName() != null)
			sql += "first_name = '" + u.getFirstName() + "', ";
		if (u.getLastName() != null)
			sql += "last_name = '" + u.getLastName() + "', ";
		if (u.getEmail() != null)
			sql += "email = '" + u.getEmail() + "', ";
		if (u.getPhoneNumber() != null)
			sql += "phone_number = '" + u.getPhoneNumber() + "', ";
		if (u.getDateOfBirth() != null)
			sql += "date_of_birth = '" + u.getDateOfBirth() + "', ";
		if (u.getGender() != null)
			sql += "gender = '" + u.getGender() + "', ";
		if (u.getAddresStreet() != null)
			sql += "address_street = '" + u.getAddresStreet() + "', ";
		if (u.getAddressCity() != null)
			sql += "address_city = '" + u.getAddressCity() + "', ";
		if (u.getAddressPostalCode() != null)
			sql += "address_postal_code = '" + u.getAddressPostalCode() + "', ";
		if (u.getAddressState() != null)
			sql += "address_state = '" + u.getAddressState() + "', ";
		if (u.getAddressCountry() != null)
			sql += "address_country = '" + u.getAddressCountry() + "', ";
		if (u.getDefaultCurrencyId() != null)
			sql += "default_currency_id = '" + u.getDefaultCurrencyId() + "', ";
		if (u.getSocialSecurityNumber() != null)
			sql += "social_security_number = '" + u.getSocialSecurityNumber() + "', ";
		if (u.getAddresStreet2() != null)
			sql += "address_street_2 = '" + u.getAddresStreet2() + "', ";
		
		sql += "updated_at= now() where user_id = " + u.getId();
		
		Statement s = conn.createStatement();
		
		int result = s.executeUpdate(sql);
		
		if (result == 0)
			throw new UserNotFoundException(u.getId().toString());

    }

    @DeleteMapping("/users/{id}")
    void deleteUser(@PathVariable Long id, @RequestHeader(name = "authorization") Optional<String> authorization) throws SQLException {
    	
    	LOGGER.info("Deleting user: " + id);
    	
		if (id == null)
			throw new UserNotFoundException(id.toString());

    	String username = th.decodeVerifyCognitoToken(authorization);

    		if (conn == null)
    			conn = DriverManager.getConnection(url);
    		
    		Statement s = conn.createStatement();
    		
    		s.execute("delete from user_documents where user_id = " + id);
    		
    		s.execute("delete from users where user_id = " + id);
    		
    		if (s.getUpdateCount() == 0)
    			throw new UserNotFoundException("User not found in our system.");
	
        	LOGGER.info("Deleted.");
        
    }
    
    private void setUserResultParameters(ResultSet r, User u, String userIdentifier)
    {
		try {
			
	    	if (r.next() == false)
	    		throw new UserNotFoundException(userIdentifier);
	    	else
	    	{
	    		u.setId(r.getLong("user_id"));
	    		u.setFirstName(r.getString("first_name"));
	    		u.setLastName(r.getString("last_name"));
	    		u.setEmail(r.getString("email"));
	    		u.setPhoneNumber(r.getString("phone_number"));
	    		u.setDateOfBirth(r.getString("date_of_birth"));
	    		u.setGender(r.getString("gender"));
	    		u.setActive(r.getBoolean("is_active"));
	    		u.setPromotioanlConsent(r.getBoolean("promotional_consent"));
	    		u.setAddresStreet(r.getString("address_street"));
	    		u.setAddresStreet2(r.getString("address_street_2"));
	    		u.setAddressCity(r.getString("address_city"));
	    		u.setAddressPostalCode(r.getString("address_postal_code"));
	    		u.setAddressState(r.getString("address_state"));
	    		u.setAddressCountry(r.getString("address_country"));
	    		u.setDefaultCurrencyId(r.getString("default_currency_id"));
	    		u.setSocialSecurityNumber(r.getString("social_security_number"));
	    		u.setUsername(r.getString("user_name"));
	    		u.setCreatedAt(r.getTimestamp("created_at"));
	    		u.setUpdatedAt(r.getTimestamp("updated_at"));
	    	}
		} catch (SQLException e) {
			
	    	LOGGER.info("Exception!!!\n" + e.getMessage());

			e.printStackTrace();
		}
    	
    }

}
