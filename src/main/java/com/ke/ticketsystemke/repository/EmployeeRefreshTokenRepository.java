package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.EmployeeRefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRefreshTokenRepository extends JpaRepository<EmployeeRefreshToken, Long> {

    Optional<EmployeeRefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from EmployeeRefreshToken token where token.tokenHash = :tokenHash")
    Optional<EmployeeRefreshToken> findWithLockByTokenHash(@Param("tokenHash") String tokenHash);
}
