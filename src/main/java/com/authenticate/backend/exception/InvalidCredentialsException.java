package com.authenticate.backend.exception;

/**
 * Thrown for both an unknown email and a wrong password, so a failed login
 * never reveals which of the two was incorrect.
 */
public class InvalidCredentialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("Please enter the correct credentials.");
    }
}
