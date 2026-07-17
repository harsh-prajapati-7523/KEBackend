package com.ke.ticketsystemke.repository;
import com.ke.ticketsystemke.entity.WarrantyReplacement; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface WarrantyReplacementRepository extends JpaRepository<WarrantyReplacement,Long>{
 Optional<WarrantyReplacement> findByWarrantyClaim_IdAndActiveTrue(Long claimId);
 List<WarrantyReplacement> findByWarrantyClaim_IdOrderByReplacementSequenceDesc(Long claimId);
 boolean existsByWarrantyClaim_IdAndActiveTrue(Long claimId);
 @Query("select coalesce(max(r.replacementSequence),0) from WarrantyReplacement r where r.warrantyClaim.id=:claimId") int findMaxSequence(@Param("claimId") Long claimId);
 List<WarrantyReplacement> findByNormalizedNewSerialNumber(String normalizedSerial);
 List<WarrantyReplacement> findByWarrantyClaim_IdInAndActiveTrue(Collection<Long> claimIds);
 List<WarrantyReplacement> findByWarrantyClaim_IdIn(Collection<Long> claimIds);
}
