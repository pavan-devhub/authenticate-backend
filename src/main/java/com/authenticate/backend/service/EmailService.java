package com.authenticate.backend.service;

import java.time.Duration;

public interface EmailService {

    /** Emails the code that confirms a new account. */
    void sendVerificationCode(String to, String recipientName, String code, Duration validity);
}
