package com.authenticate.backend.exception;

/** The code can't be used any more, so the user has to start again. */
public class OtpExpiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private OtpExpiredException(String message) {
        super(message);
    }

    public static OtpExpiredException expired() {
        return new OtpExpiredException("This code has expired. Please start again.");
    }

    public static OtpExpiredException notFound() {
        return new OtpExpiredException("This code is no longer valid. Please start again.");
    }

    public static OtpExpiredException tooManyAttempts() {
        return new OtpExpiredException("Too many incorrect codes. Please start again.");
    }
}
