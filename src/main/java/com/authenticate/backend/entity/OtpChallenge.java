package com.authenticate.backend.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A one-time code that was emailed to confirm a registration. It holds the
 * new account's details until the code is entered. Only a hash of the code
 * is stored.
 */
@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A random value the client sends back with the code. Unlike the id, it can't be guessed.
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(nullable = false, length = 254)
    private String email;

    // The account to create once the code is confirmed.
    @Column(length = 100)
    private String name;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resend_available_at", nullable = false)
    private Instant resendAvailableAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OtpChallenge() {
        // Required by JPA.
    }

    private OtpChallenge(String token, OtpPurpose purpose, String email) {
        this.token = token;
        this.purpose = purpose;
        this.email = email;
    }

    public static OtpChallenge forRegistration(String token, String name, String email, String passwordHash) {
        OtpChallenge challenge = new OtpChallenge(token, OtpPurpose.REGISTRATION, email);
        challenge.name = name;
        challenge.passwordHash = passwordHash;
        return challenge;
    }

    /**
     * Sets a new code. Wrong attempts are kept, so asking for a new code
     * doesn't give extra guesses.
     */
    public void replaceCode(String codeHash, Instant expiresAt, Instant resendAvailableAt) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.resendAvailableAt = resendAvailableAt;
    }

    public void recordFailedAttempt() {
        failedAttempts++;
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getResendAvailableAt() {
        return resendAvailableAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
