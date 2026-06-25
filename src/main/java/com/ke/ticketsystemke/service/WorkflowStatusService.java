package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateWorkflowStatusRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowStatusRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowStatusStateRequest;
import com.ke.ticketsystemke.dto.WorkflowStatusResponse;
import com.ke.ticketsystemke.config.CacheNames;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class WorkflowStatusService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStatusService.class);
    private static final Pattern STATUS_KEY_PATTERN = Pattern.compile("^[A-Z0-9_]{2,50}$");
    private static final Set<String> RESERVED_STATUS_KEYS = Arrays.stream(TicketStatus.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private final WorkflowStatusRepository workflowStatusRepository;
    private final EmployeeRepository employeeRepository;

    public WorkflowStatusService(
            WorkflowStatusRepository workflowStatusRepository,
            EmployeeRepository employeeRepository
    ) {
        this.workflowStatusRepository = workflowStatusRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.WORKFLOW_STATUSES, key = "'all'")
    public List<WorkflowStatusResponse> listStatuses(String employeeId) {
        List<WorkflowStatusResponse> statuses = workflowStatusRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(WorkflowStatusResponse::from)
                .toList();
        log.info("event=workflow_statuses_returned employeeId={} statusCount={}", employeeId, statuses.size());
        return statuses;
    }

    @Transactional
    @CacheEvict(cacheNames = {
            CacheNames.WORKFLOW_STATUSES,
            CacheNames.WORKFLOW_TRANSITIONS,
            CacheNames.WORKFLOW_TRANSITION_OPTIONS
    }, allEntries = true)
    public WorkflowStatusResponse createStatus(CreateWorkflowStatusRequest request, String employeeId) {
        String statusKey = validateNewStatusKey(request.getStatusKey());
        String displayName = validateDisplayName(request.getDisplayName());

        if (workflowStatusRepository.existsByStatusKey(statusKey)) {
            log.warn("event=workflow_status_create_rejected employeeId={} statusKey={} result=duplicate",
                    employeeId, statusKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow status key already exists");
        }

        WorkflowStatus status = new WorkflowStatus();
        status.setStatusKey(statusKey);
        status.setDisplayName(displayName);
        status.setActive(request.getActive() == null || request.getActive());
        status.setSystemStatus(false);
        status.setProtectedStatus(false);
        status.setTerminal(false);
        status.setSortOrder(request.getSortOrder());
        status.setUpdatedByEmployee(resolveEmployee(employeeId));

        try {
            WorkflowStatus saved = workflowStatusRepository.saveAndFlush(status);
            log.info("event=workflow_status_created employeeId={} statusId={} statusKey={} active={} result=created",
                    employeeId, saved.getId(), saved.getStatusKey(), saved.isActive());
            return WorkflowStatusResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=workflow_status_create_rejected employeeId={} statusKey={} result=duplicate",
                    employeeId, statusKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow status key already exists");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {
            CacheNames.WORKFLOW_STATUSES,
            CacheNames.WORKFLOW_TRANSITIONS,
            CacheNames.WORKFLOW_TRANSITION_OPTIONS
    }, allEntries = true)
    public WorkflowStatusResponse updateStatus(Long id, UpdateWorkflowStatusRequest request, String employeeId) {
        WorkflowStatus status = findStatus(id);
        requireCustomEditable(status, employeeId, "edit");

        if (request.getDisplayName() != null) {
            status.setDisplayName(validateDisplayName(request.getDisplayName()));
        }
        if (request.getSortOrder() != null) {
            status.setSortOrder(request.getSortOrder());
        }
        status.setUpdatedByEmployee(resolveEmployee(employeeId));

        WorkflowStatus saved = workflowStatusRepository.save(status);
        log.info("event=workflow_status_updated employeeId={} statusId={} statusKey={} result=updated",
                employeeId, saved.getId(), saved.getStatusKey());
        return WorkflowStatusResponse.from(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            CacheNames.WORKFLOW_STATUSES,
            CacheNames.WORKFLOW_TRANSITIONS,
            CacheNames.WORKFLOW_TRANSITION_OPTIONS
    }, allEntries = true)
    public WorkflowStatusResponse updateStatusState(Long id, UpdateWorkflowStatusStateRequest request, String employeeId) {
        WorkflowStatus status = findStatus(id);
        requireCustomEditable(status, employeeId, "status_update");

        status.setActive(request.getActive());
        status.setUpdatedByEmployee(resolveEmployee(employeeId));

        WorkflowStatus saved = workflowStatusRepository.save(status);
        log.info("event=workflow_status_state_updated employeeId={} statusId={} statusKey={} active={} result=updated",
                employeeId, saved.getId(), saved.getStatusKey(), saved.isActive());
        return WorkflowStatusResponse.from(saved);
    }

    private WorkflowStatus findStatus(Long id) {
        return workflowStatusRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow status not found"));
    }

    private String validateNewStatusKey(String rawStatusKey) {
        String statusKey = rawStatusKey == null ? "" : rawStatusKey.trim();
        if (!STATUS_KEY_PATTERN.matcher(statusKey).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status key must contain 2 to 50 uppercase letters, digits, or underscores");
        }
        if (RESERVED_STATUS_KEYS.contains(statusKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System workflow status keys cannot be used for custom statuses");
        }
        return statusKey;
    }

    private String validateDisplayName(String rawDisplayName) {
        String displayName = rawDisplayName == null ? "" : rawDisplayName.trim();
        if (displayName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name cannot be blank");
        }
        if (displayName.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name must be at most 80 characters");
        }
        return displayName;
    }

    private void requireCustomEditable(WorkflowStatus status, String employeeId, String action) {
        if (status.isSystemStatus() || status.isProtectedStatus()) {
            log.warn("event=workflow_status_update_rejected employeeId={} statusId={} statusKey={} action={} result=protected",
                    employeeId, status.getId(), status.getStatusKey(), action);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System and protected workflow statuses are read-only");
        }
        if (status.isTerminal()) {
            log.warn("event=workflow_status_update_rejected employeeId={} statusId={} statusKey={} action={} result=terminal",
                    employeeId, status.getId(), status.getStatusKey(), action);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terminal workflow statuses are read-only");
        }
    }

    private Employee resolveEmployee(String employeeId) {
        String lookupEmployeeId = employeeId == null ? "" : employeeId.trim();
        return employeeRepository.findByEmployeeIdIgnoreCase(lookupEmployeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid employee"));
    }
}
