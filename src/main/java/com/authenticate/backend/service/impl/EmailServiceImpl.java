package com.authenticate.backend.service.impl;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.authenticate.backend.service.EmailService;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String APP_NAME = "Authenticate";

    private final JavaMailSender mailSender;
    private final String from;

    public EmailServiceImpl(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
        if (from.isBlank()) {
            log.warn("No Gmail account is set (spring.mail.username / MAIL_USERNAME), "
                    + "so verification codes can't be emailed. See .env.example.");
        }
    }

    @Override
    public void sendVerificationCode(String to, String recipientName, String code, Duration validity) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(APP_NAME + " <" + from + ">");
        message.setTo(to);
        message.setSubject(code + " is your " + APP_NAME + " verification code");
        message.setText("""
                Hi %s,

                Enter this code to finish creating your %s account:

                    %s

                It expires in %d minutes. If you didn't try to sign up, you can ignore this email.
                """.formatted(recipientName, APP_NAME, code, validity.toMinutes()));

        mailSender.send(message);
    }
}
