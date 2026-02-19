package com.example.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends SampleException {
    public static final ErrorCode INVALID_TOKEN = new ErrorCode("AUTH_001", "error.auth.invalid-token");
    public static final ErrorCode EXPIRED_TOKEN = new ErrorCode("AUTH_002", "error.auth.expired-token");

    public AuthException(ErrorCode error) {
        super(HttpStatus.UNAUTHORIZED, error);
    }
}
