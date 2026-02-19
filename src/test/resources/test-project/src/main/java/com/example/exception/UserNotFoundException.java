package com.example.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends SampleException {
    public static final ErrorCode NOT_FOUND_USER = new ErrorCode("USER_001", "error.user.not-found");
    public static final ErrorCode DISABLED_USER = new ErrorCode("USER_002", "error.user.disabled");

    public UserNotFoundException(ErrorCode error) {
        super(HttpStatus.NOT_FOUND, error);
    }
}
