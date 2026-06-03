package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByRoleKey(String roleKey);

    Optional<Role> findByRoleKey(String roleKey);

    List<Role> findAllByOrderByRoleKeyAsc();
}
