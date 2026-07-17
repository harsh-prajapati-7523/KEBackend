package com.ke.ticketsystemke.repository;
import com.ke.ticketsystemke.entity.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.time.Instant;import java.util.*;
public interface WarrantyAttachmentRepository extends JpaRepository<WarrantyAttachment,Long>{
 @Query("select a.warrantyClaim.id,a.category from WarrantyAttachment a where a.active=true and a.warrantyClaim.id in :claimIds")List<Object[]> findActiveCategoriesByClaimIds(@Param("claimIds")Collection<Long> claimIds);
 List<WarrantyAttachment> findByWarrantyClaim_IdOrderByUploadedAtDesc(Long claimId);long countByWarrantyClaim_IdAndActiveTrue(Long claimId);long countByWarrantyReplacement_IdAndActiveTrue(Long replacementId);boolean existsByWarrantyClaim_IdAndWarrantyReplacementIsNullAndCategoryAndActiveTrue(Long claimId,WarrantyAttachmentCategory category);boolean existsByWarrantyReplacement_IdAndCategoryAndActiveTrue(Long replacementId,WarrantyAttachmentCategory category);List<WarrantyAttachment> findByActiveFalseAndDeactivatedAtBefore(Instant before);
}
