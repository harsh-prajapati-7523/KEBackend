package com.ke.ticketsystemke.repository;
import com.ke.ticketsystemke.entity.WarrantyClaimEvent;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository;
public interface WarrantyClaimEventRepository extends JpaRepository<WarrantyClaimEvent,Long>{ Page<WarrantyClaimEvent> findByWarrantyClaim_Id(Long claimId, Pageable pageable); }
