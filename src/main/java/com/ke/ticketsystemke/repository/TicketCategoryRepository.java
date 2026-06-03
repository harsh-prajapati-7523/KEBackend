package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketCategoryRepository extends JpaRepository<TicketCategoryConfig, Long> {

    boolean existsByCategoryKey(String categoryKey);

    Optional<TicketCategoryConfig> findByCategoryKey(String categoryKey);

    List<TicketCategoryConfig> findAllByOrderBySortOrderAscCategoryKeyAsc();
}
