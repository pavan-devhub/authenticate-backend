package com.authenticate.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void theEmailHasTheCodeAndComesFromTheConfiguredAddress() {
        new EmailServiceImpl(mailSender, "sender@gmail.com")
                .sendVerificationCode("ada@example.com", "Ada", "123456", Duration.ofMinutes(10));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        SimpleMailMessage email = sent.getValue();

        assertThat(email.getFrom()).isEqualTo("Authenticate <sender@gmail.com>");
        assertThat(email.getTo()).containsExactly("ada@example.com");
        assertThat(email.getSubject()).isEqualTo("123456 is your Authenticate verification code");
        assertThat(email.getText()).contains("Hi Ada,", "123456", "expires in 10 minutes");
    }
}
