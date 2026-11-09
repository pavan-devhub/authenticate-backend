package com.authenticate.backend.dto.response;

import java.util.List;

/**
 * Returned on a successful login. The login history is sent here, rather than
 * from a separate endpoint, because the API has no sessions or tokens yet, so
 * this is the only moment it knows the caller owns the account.
 */
public record LoginResponse(UserResponse user, List<LoginAttemptResponse> loginAttempts) {
}
