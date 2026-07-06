package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.EmployeeRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRefreshTokenRepository extends JpaRepository<EmployeeRefreshToken, Long> {

    Optional<EmployeeRefreshToken> findByTokenHash(String tokenHash);
}
