package com.authenticate.backend.mapper;

import com.authenticate.backend.dto.response.UserResponse;
import com.authenticate.backend.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
