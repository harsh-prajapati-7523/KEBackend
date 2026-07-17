package com.ke.ticketsystemke.repository;
import com.ke.ticketsystemke.entity.WarrantyClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim,Long>{
    Optional<WarrantyClaim> findByTicket_IdAndActiveTrue(Long ticketId);
    boolean existsByTicket_IdAndActiveTrue(Long ticketId);
    long countByTicket_Id(Long ticketId);
    @Query("select coalesce(max(c.claimSequence),0) from WarrantyClaim c where c.ticket.id=:ticketId")
    int findMaxClaimSequence(Long ticketId);
    boolean existsByTicket_CategoryRecord_IdAndActiveTrue(Long categoryId);
    Optional<WarrantyClaim> findFirstByTicket_IdOrderByClaimSequenceDesc(Long ticketId);
}
