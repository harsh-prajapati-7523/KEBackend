package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.DropdownSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DropdownSourceRepository extends JpaRepository<DropdownSource, Long> {

    boolean existsBySourceKey(String sourceKey);

    List<DropdownSource> findAllByOrderByDisplayNameAscSourceKeyAscIdAsc();
}
