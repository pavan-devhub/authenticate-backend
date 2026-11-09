package com.authenticate.backend.entity;

public enum LoginOutcome {
    SUCCESS,
    WRONG_PASSWORD,
    // Only in history from when logging in needed an emailed code. Kept so those rows still load.
    WRONG_CODE
}
