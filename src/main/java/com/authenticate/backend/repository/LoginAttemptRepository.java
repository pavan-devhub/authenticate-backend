package com.authenticate.backend.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import com.authenticate.backend.entity.LoginAttempt;
import com.authenticate.backend.entity.User;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    List<LoginAttempt> findByUserOrderByAttemptedAtDesc(User user, Limit limit);
}
