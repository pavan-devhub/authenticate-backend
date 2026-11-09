package com.authenticate.backend.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Stored as text, matching the VARCHAR column, rather than a MySQL ENUM.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private LoginOutcome outcome;

    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    protected LoginAttempt() {
        // Required by JPA.
    }

    public LoginAttempt(User user, LoginOutcome outcome) {
        this.user = user;
        this.outcome = outcome;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LoginOutcome getOutcome() {
        return outcome;
    }

    public boolean isSuccessful() {
        return outcome == LoginOutcome.SUCCESS;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
