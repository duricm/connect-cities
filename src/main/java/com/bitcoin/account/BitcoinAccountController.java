package com.bitcoin.account;


import org.springframework.http.HttpStatus;


import org.springframework.web.bind.annotation.*;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.bitcoin.account.entity.AccessToken;
import com.bitcoin.account.entity.Login;
import com.bitcoin.account.entity.ResponseMessage;
import com.bitcoin.account.entity.UpdatePassword;
import com.bitcoin.account.entity.User;
import com.bitcoin.account.entity.UsernameOrEmail;
import com.bitcoin.account.entity.VerifyAccessCode;
import com.bitcoin.account.error.BadRequestException;
import com.bitcoin.account.error.UnauthorizedException;
import com.bitcoin.account.error.UserNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.apache.log4j.Logger;

@CrossOrigin(origins = {"http://localhost:3000", "https://card.btctest.net", "https://card.bitcoin.com", 
						"https://card.stage.cloud.bitcoin.com", "https://card.dev.cloud.bitcoin.com"}, allowCredentials = "true", 
methods = {RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS})
@RestController
public class BitcoinAccountController extends BitcoinUtility {
	
	private final Logger LOGGER = Logger.getLogger(this.getClass());
	
	TokenHandler th = new TokenHandler();
	
	CognitoHelper helper = new CognitoHelper();

	//private static String url = "jdbc:postgresql://3.136.241.73:5432/bitcoin-card?user=postgres&password=bch_admin&ssl=true&sslmode=verify-ca&sslrootcert=./.postgres/root.crt";

	private static Connection conn;
	
	public AWSCognitoIdentityProvider getAmazonCognitoIdentityClient() {
	      ClasspathPropertiesFileCredentialsProvider propertiesFileCredentialsProvider = 
	           new ClasspathPropertiesFileCredentialsProvider();
	      
	 
	       return AWSCognitoIdentityProviderClientBuilder.standard()
	                      .withCredentials(propertiesFileCredentialsProvider)
	                             .withRegion("us-east-1")
	                             .build();
	 
	   }
	
    // Reset password will send reset code to user's email. After this update password should be
	// called with new password
    @PostMapping(value = "/reset-password", consumes = "*/*")
    @ResponseStatus(HttpStatus.OK)
    ResponseMessage resetPassword(@RequestBody UsernameOrEmail uoe, @RequestHeader(name = "authorization") Optional<String> authorization) throws SQLException 
    {
    	
    	//String username = th.decodeVerifyCognitoToken(authorization);
    	
    	String tempUoe = "";
    	String username = "";
    	
    	if (uoe.getUsernameOrEmail() != null)
    		tempUoe = uoe.getUsernameOrEmail();
    	else
    		throw new BadRequestException(BitcoinConstants.USERNAME_OR_EMAIL_REQUIRED);
    	
    	// See if parameter is email
    	if (tempUoe.indexOf('@') > 0)
    	{
    		if (conn == null)
    			conn = DriverManager.getConnection(BitcoinConstants.DB_URL);
    		
        	PreparedStatement stmt = conn.prepareStatement("select user_name from users where email = ?");  
        	stmt.setString(1, tempUoe);
        	ResultSet r = stmt.executeQuery();
        	
	    	if (r.next() == false)
	    		throw new UserNotFoundException(tempUoe);
	    	else
	    		username = r.getString(1);

    	}
    	else 
    		username = tempUoe;
    	
    	ResponseMessage response = new ResponseMessage();
    	
    	response.setMessage(helper.resetPassword(username));
    		
    	return response;
    }
    
    @PostMapping(value = "/update-password/me", consumes = "*/*")
    @ResponseStatus(HttpStatus.OK)
    ResponseMessage updatePassword(@RequestBody UpdatePassword u, @RequestHeader(name = "authorization") Optional<String> authorization) 
    {
    	String username = th.decodeVerifyCognitoToken(authorization);
    	
    	LOGGER.info("Updating password for user " + username);

    	if (u.getNewPassword() == null)
    		throw new BadRequestException(BitcoinConstants.PASSWORD_REQUIRED);
    	else 
        	if (u.getCode() == null)
        		throw new BadRequestException(BitcoinConstants.CODE_REQUIRED);

    	ResponseMessage response = new ResponseMessage();
    	
    	response.setMessage(helper.updatePassword(username, u.getNewPassword(), u.getCode()));
    		
    	return response;
    }
    
    // Login user
    @PostMapping(value = "/login", consumes = "*/*")
    @ResponseStatus(HttpStatus.OK)
    AccessToken loginUser(@RequestBody Login l) 
    {
    	
    	if (l.getUsername() == null)
    		throw new BadRequestException(BitcoinConstants.USER_NAME_REQUIRED);
    	else 
        	if (l.getPassword() == null)
        		throw new BadRequestException(BitcoinConstants.PASSWORD_REQUIRED);
    	
    	AccessToken a = new AccessToken();
    	String accessToken;
    	
    	accessToken = helper.validateUser(l.getUsername(), l.getPassword());
    	    	
    	a.setAccess_token(accessToken);
    		
    	return a;
    }
    
    // Find
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/logout")
    void logoutUser(@RequestHeader(name = "authorization") Optional<String> authorization) {
    	
    	String username = th.decodeVerifyCognitoToken(authorization);
    	
    	LOGGER.info("Logging out user " + username);

    	String[] authorizationParts = null;
    	    	
    	if (authorization.isPresent())
    		authorizationParts = authorization.get().split(" ");
    	
    	if (authorization.isEmpty() || authorizationParts.length != 2)
    		throw new UnauthorizedException("Bad token value");
    	
    	String token = authorizationParts[1];
    	
    	helper.signoutUser(token);
    	    	
    }
    
    // Verify user's access code so user can be confirmed in Cognito
    @PostMapping(value = "/verify", consumes = "*/*")
    @ResponseStatus(HttpStatus.OK)
    boolean verifyAccessCode(@RequestBody VerifyAccessCode v) 
    {
    	boolean result;
    	
    	if (v.getUsername() == null)
    		throw new BadRequestException(BitcoinConstants.USER_NAME_REQUIRED);
    	else 
        	if (v.getCode() == null)
        		throw new BadRequestException(BitcoinConstants.VERIFY_CODE_REQUIRED);
    	
    	result = helper.verifyAccessCode(v.getUsername(), v.getCode());
    	
    	if (! result)
    		throw new UnauthorizedException(BitcoinConstants.WRONG_CODE);
    	
    		
    	return result;
    }
	
    // Save
    @PostMapping(value = "/users", consumes = "*/*")
    @ResponseStatus(HttpStatus.CREATED)
    void newUser(@RequestBody User u, @RequestHeader(name = "authorization") Optional<String> authorization) throws SQLException {
    	String sql = "insert into users (first_name, last_name, email, phone_number, date_of_birth, gender, is_active, promotional_consent" +
    	", address_street, address_city, address_postal_code, address_state, address_country, default_currency_id, social_security_number" +
    			", user_name, address_street_2, shipping_address_street, shipping_address_city, shipping_address_postal_code, shipping_address_state, " + 
    	"shipping_address_country, shipping_address_street_2, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())";
    	    	
    	LOGGER.info("Adding new user to database...");
    	LOGGER.info("User data: \n" + u.toString());
    	
    	if (u.getUsername() == null)
    		throw new BadRequestException(BitcoinConstants.USER_NAME_REQUIRED);
    	else
    		if (u.getPassword() == null)
        		throw new BadRequestException(BitcoinConstants.PASSWORD_REQUIRED);
    		else
    			if (u.getEmail() == null)
    	    		throw new BadRequestException(BitcoinConstants.EMAIL_REQUIRED);

    	//String username = th.decodeVerifyCognitoToken(authorization);
    	
		if (conn == null)
			conn = DriverManager.getConnection(BitcoinConstants.DB_URL);
		
    	PreparedStatement stmt = conn.prepareStatement(sql);  
    	stmt.setString(1,u.getFirstName());
    	stmt.setString(2,u.getLastName());
    	stmt.setString(3, u.getEmail());
    	stmt.setString(4, u.getPhoneNumber());
    	stmt.setString(5, u.getDateOfBirth());
    	stmt.setString(6, u.getGender());
    	stmt.setBoolean(7, u.isActive());
    	stmt.setBoolean(8, u.isPromotioanlConsent());
    	stmt.setString(9, u.getAddressStreet());
    	stmt.setString(10, u.getAddressCity());
    	stmt.setString(11, u.getAddressPostalCode());
    	stmt.setString(12, u.getAddressState());
    	stmt.setString(13, u.getAddressCountry());
    	stmt.setString(14, u.getDefaultCurrencyId());
    	stmt.setString(15, u.getSocialSecurityNumber());
    	stmt.setString(16, u.getUsername());
    	stmt.setString(17, u.getAddressStreet2());
    	
    	stmt.setString(18, u.getShippingAddressStreet());
    	stmt.setString(19, u.getShippingAddressCity());
    	stmt.setString(20, u.getShippingAddressPostalCode());
    	stmt.setString(21, u.getShippingAddressState());
    	stmt.setString(22, u.getShippingAddressCountry());
    	stmt.setString(23, u.getShippingAddressStreet2());
    	
    	LOGGER.info("Executing insert statement...");
    	stmt.execute();
    	LOGGER.info("Inserted new user in database.");
    	
    	// Create new user in Cognito
    	LOGGER.info("Creating Cognito user...");
    	boolean cognitoResult = helper.signUpUser(u.getUsername(), u.getPassword(), u.getEmail(), u.getPhoneNumber());
		
    	if (! cognitoResult)
    	{
    		LOGGER.info("Failed to create Cognito user + " + u.getUsername());
        	stmt = conn.prepareStatement("delete from users where user_name = ?");  
        	stmt.setString(1, u.getUsername());
		    stmt.execute();
		    throw new SQLException("Failed to create Cognito user + " + u.getUsername());
    	}
    	

    }
    
    // Find
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/me")
    User findMyUserInfo(@RequestHeader(name = "authorization") Optional<String> authorization) {
   
    	String username = th.decodeVerifyCognitoToken(authorization);

    	LOGGER.info("Getting user details for " + username);
    	
    	User u = new User();
    	
    	try {
    		if (conn == null)
    			conn = DriverManager.getConnection(BitcoinConstants.DB_URL);
    		    		
    		Statement s = conn.createStatement();
    		ResultSet r = s.executeQuery("select * from users where user_name = '" + username + "'");
    		
    		setUserResultParameters(r, u, username);

    		    		
    	} catch (SQLException e) {
    		
	    	LOGGER.info("Exception!!!\n" + e.getMessage());

    		e.printStackTrace();
    	}
    	
    	LOGGER.info("Retrieved user data: \n" + u.toString());
    	
         return u;
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
    			conn = DriverManager.getConnection(BitcoinConstants.DB_URL);
    		    		
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
    

    // Save or update
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(value = "/users/me", consumes = "*/*")
    void updateUser(@RequestBody User u, @RequestHeader(name = "authorization") Optional<String> authorization) throws SQLException {
    	
    	String username = th.decodeVerifyCognitoToken(authorization);
    	u.setUsername(username);
    	
    	LOGGER.info("Updating user data for user: " + username);

    	LOGGER.info("User data: \n" + u.toString());
    	
    	String sql = "update users set ";
 
		if (conn == null)
			conn = DriverManager.getConnection(BitcoinConstants.DB_URL);
		
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
		if (u.getAddressStreet() != null)
			sql += "address_street = '" + u.getAddressStreet() + "', ";
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
		if (u.getAddressStreet2() != null)
			sql += "address_street_2 = '" + u.getAddressStreet2() + "', ";
		
		// Shipping address parameters
		if (u.getShippingAddressStreet() != null)
			sql += "shipping_address_street = '" + u.getShippingAddressStreet() + "', ";
		if (u.getShippingAddressCity() != null)
			sql += "shipping_address_city = '" + u.getShippingAddressCity() + "', ";
		if (u.getShippingAddressPostalCode() != null)
			sql += "shipping_address_postal_code = '" + u.getShippingAddressPostalCode() + "', ";
		if (u.getShippingAddressState() != null)
			sql += "shipping_address_state = '" + u.getShippingAddressState() + "', ";
		if (u.getShippingAddressCountry() != null)
			sql += "shipping_address_country = '" + u.getShippingAddressCountry() + "', ";
		if (u.getShippingAddressStreet2() != null)
			sql += "shipping_address_street_2 = '" + u.getShippingAddressStreet2() + "', ";
		
		sql += "updated_at= now() where user_name = '" + username + "'";
		
		Statement s = conn.createStatement();
		
		int result = s.executeUpdate(sql);
		
		if (result == 0)
			throw new UserNotFoundException(u.getId().toString());
		
    }

    @DeleteMapping("/users/me")
    void deleteUser(@RequestHeader(name = "authorization") Optional<String> authorization) throws SQLException {

    	String username = th.decodeVerifyCognitoToken(authorization);
    	String id = "";
    	
    	LOGGER.info("Deleting user: " + username);

    		if (conn == null)
    			conn = DriverManager.getConnection(BitcoinConstants.DB_URL);
    		
    		id = getUserId(username);
    		
    		Statement s = conn.createStatement();
    		
    		s.execute("delete from user_documents where user_id = " + id);
    		
    		s.execute("delete from users where user_id = " + id);
    			
        	LOGGER.info("Deleted user: " + username);
        
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
	    		u.setAddressStreet(r.getString("address_street"));
	    		u.setAddressStreet2(r.getString("address_street_2"));
	    		u.setAddressCity(r.getString("address_city"));
	    		u.setAddressPostalCode(r.getString("address_postal_code"));
	    		u.setAddressState(r.getString("address_state"));
	    		u.setAddressCountry(r.getString("address_country"));
	    		u.setDefaultCurrencyId(r.getString("default_currency_id"));
	    		u.setSocialSecurityNumber(r.getString("social_security_number"));
	    		u.setUsername(r.getString("user_name"));
	    		u.setCreatedAt(r.getTimestamp("created_at"));
	    		u.setUpdatedAt(r.getTimestamp("updated_at"));
	    		
	    		u.setShippingAddressStreet(r.getString("shipping_address_street"));
	    		u.setShippingAddressStreet2(r.getString("shipping_address_street_2"));
	    		u.setShippingAddressCity(r.getString("shipping_address_city"));
	    		u.setShippingAddressPostalCode(r.getString("shipping_address_postal_code"));
	    		u.setShippingAddressState(r.getString("shipping_address_state"));
	    		u.setShippingAddressCountry(r.getString("shipping_address_country"));
	    	}
		} catch (SQLException e) {
			
	    	LOGGER.info("Exception!!!\n" + e.getMessage());

			e.printStackTrace();
		}
    	
    }

}
