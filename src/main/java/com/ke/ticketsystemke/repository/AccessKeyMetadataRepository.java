package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccessKeyMetadataRepository extends JpaRepository<AccessKeyMetadata, Long> {

    List<AccessKeyMetadata> findAllByOrderBySortOrderAscAccessKeyAsc();

    List<AccessKeyMetadata> findAllBySystemKeyFalseAndProtectedKeyFalseOrderByCategoryAscSortOrderAscDisplayNameAscAccessKeyAsc();

    boolean existsByAccessKey(String accessKey);

    Optional<AccessKeyMetadata> findByAccessKey(String accessKey);
}
