package com.bitcoin.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StartBitcoinAccountApplication {
	
    // start everything
    public static void main(String[] args) {
    	
        SpringApplication.run(StartBitcoinAccountApplication.class, args);
    }


}