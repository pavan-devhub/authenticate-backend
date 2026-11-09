package com.authenticate.backend.exception;

public class OtpResendTooSoonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long retryAfterSeconds;

    public OtpResendTooSoonException(long retryAfterSeconds) {
        super("Please wait " + retryAfterSeconds + " seconds before asking for a new code.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
