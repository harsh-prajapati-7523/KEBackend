package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateTicketCategoryRequest;
import com.ke.ticketsystemke.dto.TicketCategoryResponse;
import com.ke.ticketsystemke.dto.TicketCategoryWorkflowConfigResponse;
import com.ke.ticketsystemke.dto.UpdateTicketCategoryWorkflowConfigRequest;
import com.ke.ticketsystemke.config.CacheNames;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.WorkflowMode;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class TicketCategoryService {

    private static final Logger log = LoggerFactory.getLogger(TicketCategoryService.class);
    private static final String OTHER_CATEGORY_KEY = "OTHER";

    private final TicketCategoryRepository ticketCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkflowValidationService workflowValidationService;

    public TicketCategoryService(
            TicketCategoryRepository ticketCategoryRepository,
            EmployeeRepository employeeRepository,
            WorkflowValidationService workflowValidationService
    ) {
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.employeeRepository = employeeRepository;
        this.workflowValidationService = workflowValidationService;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.TICKET_CATEGORIES, key = "'all'")
    public List<TicketCategoryResponse> listCategories() {
        return ticketCategoryRepository.findAllByOrderBySortOrderAscCategoryKeyAsc()
                .stream()
                .map(TicketCategoryResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.TICKET_CATEGORIES, allEntries = true)
    public TicketCategoryResponse createCategory(CreateTicketCategoryRequest request, String actorEmployeeId) {
        String categoryKey = request.getCategoryKey().trim();
        if (ticketCategoryRepository.existsByCategoryKey(categoryKey)) {
            log.warn("event=ticket_category_create_denied actorEmployeeId={} categoryKey={} decision=duplicate_category_key",
                    actorEmployeeId, categoryKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category key already exists");
        }

        TicketCategoryConfig category = new TicketCategoryConfig();
        category.setCategoryKey(categoryKey);
        category.setDisplayName(request.getDisplayName().trim());
        category.setActive(request.getActive() == null || request.getActive());
        category.setSystemCategory(false);
        category.setSortOrder(request.getSortOrder());

        try {
            TicketCategoryConfig created = ticketCategoryRepository.saveAndFlush(category);
            log.info("event=ticket_category_created actorEmployeeId={} categoryId={} categoryKey={} targetActive={}",
                    actorEmployeeId, created.getId(), created.getCategoryKey(), created.isActive());
            return TicketCategoryResponse.from(created);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=ticket_category_create_denied actorEmployeeId={} categoryKey={} decision=duplicate_category_key",
                    actorEmployeeId, categoryKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category key already exists");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.TICKET_CATEGORIES, allEntries = true)
    public TicketCategoryResponse updateStatus(Long id, boolean active, String actorEmployeeId) {
        TicketCategoryConfig category = ticketCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (!active && OTHER_CATEGORY_KEY.equals(category.getCategoryKey())) {
            log.warn("event=ticket_category_update_denied actorEmployeeId={} categoryId={} categoryKey={} targetActive={} decision=other_protected",
                    actorEmployeeId, category.getId(), category.getCategoryKey(), active);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTHER category cannot be disabled");
        }

        category.setActive(active);
        TicketCategoryConfig saved = ticketCategoryRepository.save(category);
        log.info("event={} actorEmployeeId={} categoryId={} categoryKey={} targetActive={}",
                active ? "ticket_category_enabled" : "ticket_category_disabled",
                actorEmployeeId, saved.getId(), saved.getCategoryKey(), saved.isActive());
        return TicketCategoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TicketCategoryWorkflowConfigResponse getWorkflowConfig(Long id) {
        return TicketCategoryWorkflowConfigResponse.from(findCategory(id));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.TICKET_CATEGORIES, allEntries = true)
    public TicketCategoryWorkflowConfigResponse updateWorkflowConfig(
            Long id,
            UpdateTicketCategoryWorkflowConfigRequest request,
            String actorEmployeeId
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow config request is required");
        }

        TicketCategoryConfig category = findCategory(id);
        WorkflowMode workflowMode = request.getWorkflowMode();
        boolean dbWorkflowEnabled = Boolean.TRUE.equals(request.getDbWorkflowEnabled());
        boolean fixedActionsEnabled = Boolean.TRUE.equals(request.getFixedActionsEnabled());

        validateWorkflowConfig(workflowMode, dbWorkflowEnabled, fixedActionsEnabled);

        if (workflowMode == WorkflowMode.DB_CONFIGURED && dbWorkflowEnabled) {
            workflowValidationService.requireValidCategoryWorkflow(category.getId());
        }

        category.setWorkflowMode(workflowMode);
        category.setDbWorkflowEnabled(dbWorkflowEnabled);
        category.setFixedActionsEnabled(fixedActionsEnabled);
        category.setWorkflowModeUpdatedAt(Instant.now());
        category.setWorkflowModeUpdatedByEmployee(resolveEmployee(actorEmployeeId));

        TicketCategoryConfig saved = ticketCategoryRepository.save(category);
        log.info(
                "event=ticket_category_workflow_config_updated actorEmployeeId={} categoryId={} categoryKey={} workflowMode={} dbWorkflowEnabled={} fixedActionsEnabled={}",
                actorEmployeeId,
                saved.getId(),
                saved.getCategoryKey(),
                saved.getWorkflowMode(),
                saved.isDbWorkflowEnabled(),
                saved.isFixedActionsEnabled()
        );
        return TicketCategoryWorkflowConfigResponse.from(saved);
    }

    private TicketCategoryConfig findCategory(Long id) {
        return ticketCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private void validateWorkflowConfig(
            WorkflowMode workflowMode,
            boolean dbWorkflowEnabled,
            boolean fixedActionsEnabled
    ) {
        if (workflowMode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow mode is required");
        }
        if (workflowMode == WorkflowMode.DB_CONFIGURED && !dbWorkflowEnabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DB workflow must be enabled for DB_CONFIGURED mode");
        }
        if (workflowMode == WorkflowMode.LEGACY_FIXED && dbWorkflowEnabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DB workflow must be disabled for LEGACY_FIXED mode");
        }
        if (workflowMode == WorkflowMode.LEGACY_FIXED && !fixedActionsEnabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fixed actions must remain enabled for LEGACY_FIXED mode");
        }
    }

    private Employee resolveEmployee(String employeeId) {
        String lookupEmployeeId = employeeId == null ? "" : employeeId.trim();
        return employeeRepository.findByEmployeeIdIgnoreCase(lookupEmployeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid employee"));
    }
}
