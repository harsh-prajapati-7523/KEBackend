package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.TicketFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketFieldDefinitionRepository extends JpaRepository<TicketFieldDefinition, Long> {

    boolean existsByFieldKey(String fieldKey);

    List<TicketFieldDefinition> findAllByOrderBySortOrderAscFieldKeyAsc();
}
