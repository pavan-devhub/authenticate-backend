package com.authenticate.backend.exception;

/** A wrong code, with tries still left. */
public class InvalidOtpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int attemptsLeft;

    public InvalidOtpException(int attemptsLeft) {
        super("That code is incorrect. You have " + attemptsLeft
                + (attemptsLeft == 1 ? " attempt" : " attempts") + " left.");
        this.attemptsLeft = attemptsLeft;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }
}
