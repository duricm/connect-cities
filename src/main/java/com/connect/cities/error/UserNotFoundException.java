package com.connect.cities.error;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userIdentifier) {
        super("User not found : " + userIdentifier);
    }

}
