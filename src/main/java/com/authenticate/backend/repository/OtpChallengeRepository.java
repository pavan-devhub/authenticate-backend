package com.authenticate.backend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.authenticate.backend.entity.OtpChallenge;
import com.authenticate.backend.entity.OtpPurpose;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    Optional<OtpChallenge> findByToken(String token);

    @Modifying
    @Query("delete from OtpChallenge c where c.purpose = :purpose and c.email = :email")
    void deleteByPurposeAndEmail(@Param("purpose") OtpPurpose purpose, @Param("email") String email);

    @Modifying
    @Transactional
    @Query("delete from OtpChallenge c where c.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
