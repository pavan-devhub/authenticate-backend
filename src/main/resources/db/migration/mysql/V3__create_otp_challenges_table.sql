-- One-time codes emailed to confirm a registration or a login.
-- A registration waits here, with its hashed password, until the code is
-- entered, so no row in users exists before the email address is confirmed.
CREATE TABLE otp_challenges (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    token               VARCHAR(64)  NOT NULL,
    purpose             VARCHAR(20)  NOT NULL,
    email               VARCHAR(254) NOT NULL,
    name                VARCHAR(100) NULL,
    password_hash       VARCHAR(255) NULL,
    user_id             BIGINT       NULL,
    code_hash           VARCHAR(255) NOT NULL,
    failed_attempts     INT          NOT NULL,
    expires_at          DATETIME(6)  NOT NULL,
    resend_available_at DATETIME(6)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_otp_challenges_token UNIQUE (token),
    INDEX idx_otp_challenges_purpose_email (purpose, email),
    INDEX idx_otp_challenges_expires_at (expires_at),
    CONSTRAINT fk_otp_challenges_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
