package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.RoleAccessRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleAccessRuleRepository extends JpaRepository<RoleAccessRule, Long> {

    List<RoleAccessRule> findAllByRoleId(Long roleId);

    Optional<RoleAccessRule> findByRoleIdAndAccessKey(Long roleId, AccessKey accessKey);
}
