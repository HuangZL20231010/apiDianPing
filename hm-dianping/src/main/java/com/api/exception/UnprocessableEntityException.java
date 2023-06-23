package com.api.exception;

public class UnprocessableEntityException extends RuntimeException {
    public UnprocessableEntityException(String m) {
        super(m);
    }
}
