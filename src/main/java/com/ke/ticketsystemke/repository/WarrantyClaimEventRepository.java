package com.ke.ticketsystemke.repository;
import com.ke.ticketsystemke.entity.WarrantyClaimEvent;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
public interface WarrantyClaimEventRepository extends JpaRepository<WarrantyClaimEvent,Long>{ Page<WarrantyClaimEvent> findByWarrantyClaim_Id(Long claimId, Pageable pageable);@Query("select e.warrantyClaim.id,max(e.eventTimestamp) from WarrantyClaimEvent e where e.warrantyClaim.id in :claimIds group by e.warrantyClaim.id")List<Object[]> findLastActivityByClaimIds(@Param("claimIds")Collection<Long> claimIds); }
