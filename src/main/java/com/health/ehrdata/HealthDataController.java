package com.health.ehrdata;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.health.ehrdata.error.BadRequestException;
import com.health.ehrdata.error.UnauthorizedException;
import com.health.ehrdata.error.UserNotFoundException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true", 
methods = {RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS})
@RestController
public class HealthDataController {
	
	private final Logger LOGGER = Logger.getLogger(this.getClass());
	
	private final String ehrDataEndpoint = "https://dashboard.healthit.gov/api/open-api.php?source=AHA_2008-2015.csv&period=2014";

    public static HashMap<String, Double> sortByValue(HashMap<String, Double> hm) 
    { 
        // Create a list from elements of HashMap 
        List<Map.Entry<String, Double> > list = 
               new LinkedList<Map.Entry<String, Double> >(hm.entrySet()); 
  
        // Sort the list 
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() { 
            public int compare(Map.Entry<String, Double> o1,  
                               Map.Entry<String, Double> o2) 
            { 
                return (o1.getValue()).compareTo(o2.getValue()); 
            } 
        }); 
          
        // put data from sorted list to hashmap  
        HashMap<String, Double> temp = new LinkedHashMap<String, Double>(); 
        for (Map.Entry<String, Double> aa : list) { 
            temp.put(aa.getKey(), aa.getValue()); 
        } 
        return temp; 
    } 
    
    // Find
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/ehr-data")
    String getEHRData() throws JSONException {
    	
    	String result = "";
    
    	String output =  getEHR2014Items();
    	
	    JSONObject jsonObject = new JSONObject(output);
	    
	    Iterator<String> keys = jsonObject.keys();
	    
	    HashMap<String, Double> ehrMap = new HashMap();

	    while(keys.hasNext()) {
	        String key = keys.next();
	        if (jsonObject.get(key) instanceof JSONObject) {
	        	
	        	JSONObject tempObj = (JSONObject)jsonObject.get(key);
	        	ehrMap.put(tempObj.getString("region"), tempObj.getDouble("pct_hospitals_basic_ehr_notes"));
	        	//result += "Region " + tempObj.getString("region") + " EHR Notes Percentage: " + tempObj.getString("pct_hospitals_basic_ehr_notes") + "\n";
	               
	        }
	    }	    
	    
        Map<String, Double> sortedMap = sortByValue(ehrMap); 
                
        Vector<String> setVector = new Vector<String>();
        
        // print the sorted hashmap 
        for (Map.Entry<String, Double> en : sortedMap.entrySet()) { 
            setVector.add("Region: " + en.getKey() + " EHR Percentage: " + en.getValue() + "\n"); 
        } 
        
        for (int i = setVector.size() - 1; i >= 0; i--)
        	result += setVector.get(i);
    	
    	return result;
    
    }
    
	public String getEHR2014Items()
	{
		String output = "", tempRead = "";
		
		try {

		URL url = new URL(ehrDataEndpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
		}
				
		LOGGER.info("Response code is: " + conn.getResponseCode());
		
		LOGGER.info("Response message is: " + conn.getResponseMessage());

		BufferedReader br = new BufferedReader(new InputStreamReader(
			(conn.getInputStream())));

		while ((tempRead = br.readLine()) != null) {
			output += tempRead;
		}
		
		conn.disconnect();

	  } catch (MalformedURLException e) {

		e.printStackTrace();

	  } catch (IOException e) {

		e.printStackTrace();

	  }
	
		return output;
	  
	}

}
