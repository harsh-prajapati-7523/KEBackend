package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.DropdownOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DropdownOptionRepository extends JpaRepository<DropdownOption, Long> {

    boolean existsBySourceIdAndOptionKey(Long sourceId, String optionKey);

    boolean existsBySourceIdAndActiveTrue(Long sourceId);

    List<DropdownOption> findAllBySourceIdOrderBySortOrderAscDisplayValueAscIdAsc(Long sourceId);

    List<DropdownOption> findAllBySourceIdAndActiveTrueOrderBySortOrderAscDisplayValueAscIdAsc(Long sourceId);

    Optional<DropdownOption> findByIdAndSourceId(Long id, Long sourceId);

    Optional<DropdownOption> findBySourceIdAndOptionKeyAndActiveTrue(Long sourceId, String optionKey);
}
