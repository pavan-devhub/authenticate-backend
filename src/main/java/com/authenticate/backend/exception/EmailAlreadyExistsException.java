package com.authenticate.backend.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailAlreadyExistsException() {
        super("An account with this email already exists.");
    }
}
