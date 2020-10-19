package com.connect.cities;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;


@CrossOrigin(origins = {"http://localhost:8080"}, allowCredentials = "true", 
methods = {RequestMethod.GET})
@RestController
public class ConnectCitiesController {
	
	private final Logger LOGGER = Logger.getLogger(this.getClass());
	
    // Check if cities are connected
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/connected")
    String checkIfCitiesAreConnected(@RequestParam("origin") String origin, @RequestParam("destination") String destination) throws Exception{
    	    
    	String output = checkIfConnected(origin, destination);
    	
    	if (output.equals("no"))
    		output = checkIfConnected(destination, origin);
    	
    	return output;
    
    }
    
	public String checkIfConnected(String origin, String destination) throws Exception
	{
		System.out.println("Origin " + origin + " Destination " + destination);
		
		// Open the file with every request so it can be changed dynamically without restarting the application
        File file = ResourceUtils.getFile("classpath:cities.txt");

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String [] tempElements;
            while ((line = br.readLine()) != null) {
               tempElements = line.split(", ");
               
               if (origin.equals(tempElements[0]) && destination.equals(tempElements[1]))
            	   return "yes";
               else
            	   if (origin.equals(tempElements[0]))
            		   return checkIfConnected(tempElements[1], destination);
            }
        }
	
        // return 'yes' or 'no'
		return "no";
	  
	}

}
