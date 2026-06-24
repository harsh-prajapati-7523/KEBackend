package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionCategoryRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkflowTransitionCategoryScopeValidator {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTransitionCategoryScopeValidator.class);

    private final WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository;
    private final TicketCategoryRepository ticketCategoryRepository;

    public WorkflowTransitionCategoryScopeValidator(
            WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository,
            TicketCategoryRepository ticketCategoryRepository
    ) {
        this.workflowTransitionCategoryRuleRepository = workflowTransitionCategoryRuleRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(WorkflowTransition transition, Ticket ticket) {
        if (transition == null || transition.getId() == null) {
            return false;
        }

        Long transitionId = transition.getId();
        if (!workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(transitionId)) {
            return true;
        }

        try {
            TicketCategoryConfig category = resolveActiveCategory(ticket);
            return workflowTransitionCategoryRuleRepository
                    .existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(transitionId, category.getId());
        } catch (RuntimeException ex) {
            log.warn(
                    "event=workflow_transition_category_scope_check_failed ticketId={} transitionId={} decision=deny",
                    ticket == null ? null : ticket.getId(),
                    transitionId
            );
            return false;
        }
    }

    @Transactional(readOnly = true)
    public void requireAllowed(WorkflowTransition transition, Ticket ticket) {
        if (!isAllowed(transition, ticket)) {
            log.warn(
                    "event=workflow_transition_category_scope_denied ticketId={} transitionId={} decision=deny",
                    ticket == null ? null : ticket.getId(),
                    transition == null ? null : transition.getId()
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow transition is not available");
        }
    }

    private TicketCategoryConfig resolveActiveCategory(Ticket ticket) {
        if (ticket == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket category is required");
        }

        TicketCategoryConfig category = ticket.getCategoryRecord();
        if (category == null && ticket.getCategory() != null) {
            category = ticketCategoryRepository.findByCategoryKey(ticket.getCategory().name()).orElse(null);
        }
        if (category == null || !category.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket category is not available");
        }
        return category;
    }
}
