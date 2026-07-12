package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketCategory;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> queryTickets(
            String search,
            TicketStatus status,
            WorkflowStatus statusRecord,
            TicketCategory category,
            Instant createdFrom,
            Instant createdToExclusive,
            String pickedByEmployeeId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (statusRecord != null) {
                predicates.add(criteriaBuilder.equal(root.get("statusRecord"), statusRecord));
            } else if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (createdFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdToExclusive != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), createdToExclusive));
            }
            if (pickedByEmployeeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("pickedByEmployeeId"), pickedByEmployeeId));
            }
            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        contains(criteriaBuilder, root.get("ticketNumber"), pattern),
                        contains(criteriaBuilder, root.get("mobileNumber"), pattern),
                        contains(criteriaBuilder, root.get("customerName"), pattern),
                        contains(criteriaBuilder, root.get("productType"), pattern),
                        contains(criteriaBuilder, root.get("villageOrArea"), pattern)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate contains(
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            Expression<String> field,
            String pattern
    ) {
        return criteriaBuilder.like(criteriaBuilder.lower(field), pattern, '\\');
    }
}
