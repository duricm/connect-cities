package com.health.ehrdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StartEHRHealthApplication {
	
    // start everything
    public static void main(String[] args) {
    	
        SpringApplication.run(StartEHRHealthApplication.class, args);
    }


}