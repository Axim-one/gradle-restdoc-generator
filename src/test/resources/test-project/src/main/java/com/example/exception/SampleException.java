package com.example.exception;

import org.springframework.http.HttpStatus;

public class SampleException extends RuntimeException {
    protected final HttpStatus status;
    protected String code;

    public SampleException(HttpStatus status, ErrorCode error) {
        this.status = status;
        this.code = error.code();
    }
}
