package com.example.interviewmockcoach.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(40401, message);
    }
}
