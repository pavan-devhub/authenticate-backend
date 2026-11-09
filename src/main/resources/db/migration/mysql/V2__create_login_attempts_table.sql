CREATE TABLE login_attempts (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    successful   BOOLEAN     NOT NULL,
    attempted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_login_attempts_user_time (user_id, attempted_at),
    CONSTRAINT fk_login_attempts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
