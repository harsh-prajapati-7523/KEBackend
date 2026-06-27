package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateWorkflowActionRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowActionRequest;
import com.ke.ticketsystemke.dto.WorkflowActionResponse;
import com.ke.ticketsystemke.config.CacheNames;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class WorkflowActionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowActionService.class);
    private static final Pattern ACTION_KEY_PATTERN = Pattern.compile("^[A-Z0-9_]{2,60}$");
    private static final int DISPLAY_NAME_MAX_LENGTH = 80;
    private static final int BUTTON_LABEL_MAX_LENGTH = 80;
    private static final int DESCRIPTION_MAX_LENGTH = 255;
    private static final String ACCESS_CATEGORY = "Workflow Actions";

    private final WorkflowActionRepository workflowActionRepository;
    private final AccessKeyMetadataRepository accessKeyMetadataRepository;

    public WorkflowActionService(
            WorkflowActionRepository workflowActionRepository,
            AccessKeyMetadataRepository accessKeyMetadataRepository
    ) {
        this.workflowActionRepository = workflowActionRepository;
        this.accessKeyMetadataRepository = accessKeyMetadataRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.WORKFLOW_ACTIONS, key = "'all'")
    public List<WorkflowActionResponse> listActions(String employeeId) {
        List<WorkflowActionResponse> actions = workflowActionRepository.findAllByOrderBySortOrderAscActionKeyAsc()
                .stream()
                .map(WorkflowActionResponse::from)
                .toList();
        log.info("event=workflow_actions_returned employeeId={} actionCount={}", employeeId, actions.size());
        return actions;
    }

    @Transactional
    @CacheEvict(cacheNames = {
            CacheNames.WORKFLOW_ACTIONS,
            CacheNames.ACCESS_KEY_METADATA,
            CacheNames.WORKFLOW_TRANSITIONS,
            CacheNames.WORKFLOW_TRANSITION_OPTIONS
    }, allEntries = true)
    public WorkflowActionResponse createAction(CreateWorkflowActionRequest request, String employeeId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow action request is required");
        }

        String actionKey = validateNewActionKey(request.getActionKey());
        if (workflowActionRepository.existsByActionKey(actionKey)) {
            log.warn("event=workflow_action_create_rejected employeeId={} actionKey={} result=duplicate",
                    employeeId, actionKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow action key already exists");
        }

        WorkflowAction action = new WorkflowAction();
        action.setActionKey(actionKey);
        action.setDisplayName(validateRequiredText(request.getDisplayName(), "Display name", DISPLAY_NAME_MAX_LENGTH));
        action.setButtonLabel(validateOptionalText(request.getButtonLabel(), "Button label", BUTTON_LABEL_MAX_LENGTH, action.getDisplayName()));
        action.setDescription(validateOptionalText(request.getDescription(), "Description", DESCRIPTION_MAX_LENGTH, null));
        action.setActive(request.getActive() == null || request.getActive());
        action.setSystemAction(false);
        action.setProtectedAction(false);
        action.setSortOrder(request.getSortOrder());
        action.setRequiresComment(Boolean.TRUE.equals(request.getRequiresComment()));
        action.setConfirmationRequired(Boolean.TRUE.equals(request.getConfirmationRequired()));

        try {
            WorkflowAction saved = workflowActionRepository.saveAndFlush(action);
            ensureAccessMetadata(saved);
            log.info("event=workflow_action_created employeeId={} actionId={} actionKey={} active={} result=created",
                    employeeId, saved.getId(), saved.getActionKey(), saved.isActive());
            return WorkflowActionResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=workflow_action_create_rejected employeeId={} actionKey={} result=duplicate",
                    employeeId, actionKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow action key already exists");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {
            CacheNames.WORKFLOW_ACTIONS,
            CacheNames.ACCESS_KEY_METADATA,
            CacheNames.WORKFLOW_TRANSITIONS,
            CacheNames.WORKFLOW_TRANSITION_OPTIONS
    }, allEntries = true)
    public WorkflowActionResponse updateAction(
            Long id,
            UpdateWorkflowActionRequest request,
            String employeeId
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow action request is required");
        }

        WorkflowAction action = findAction(id);
        requireCustomEditable(action, employeeId, "update");

        if (request.getDisplayName() != null) {
            action.setDisplayName(validateRequiredText(request.getDisplayName(), "Display name", DISPLAY_NAME_MAX_LENGTH));
        }
        if (request.getButtonLabel() != null) {
            action.setButtonLabel(validateOptionalText(request.getButtonLabel(), "Button label", BUTTON_LABEL_MAX_LENGTH, action.getDisplayName()));
        }
        if (request.getDescription() != null) {
            action.setDescription(validateOptionalText(request.getDescription(), "Description", DESCRIPTION_MAX_LENGTH, null));
        }
        if (request.getSortOrder() != null) {
            action.setSortOrder(request.getSortOrder());
        }
        if (request.getRequiresComment() != null) {
            action.setRequiresComment(request.getRequiresComment());
        }
        if (request.getConfirmationRequired() != null) {
            action.setConfirmationRequired(request.getConfirmationRequired());
        }

        WorkflowAction saved = workflowActionRepository.save(action);
        syncAccessMetadata(saved);
        log.info("event=workflow_action_updated employeeId={} actionId={} actionKey={} result=updated",
                employeeId, saved.getId(), saved.getActionKey());
        return WorkflowActionResponse.from(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            CacheNames.WORKFLOW_ACTIONS,
            CacheNames.ACCESS_KEY_METADATA,
            CacheNames.WORKFLOW_TRANSITIONS,
            CacheNames.WORKFLOW_TRANSITION_OPTIONS
    }, allEntries = true)
    public WorkflowActionResponse updateActionState(
            Long id,
            Boolean active,
            String employeeId
    ) {
        if (active == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active state is required");
        }

        WorkflowAction action = findAction(id);
        requireCustomEditable(action, employeeId, active ? "enable" : "disable");
        action.setActive(active);

        WorkflowAction saved = workflowActionRepository.save(action);
        syncAccessMetadata(saved);
        log.info("event={} employeeId={} actionId={} actionKey={} active={} result=updated",
                active ? "workflow_action_enabled" : "workflow_action_disabled",
                employeeId,
                saved.getId(),
                saved.getActionKey(),
                saved.isActive());
        return WorkflowActionResponse.from(saved);
    }

    private void ensureAccessMetadata(WorkflowAction action) {
        if (accessKeyMetadataRepository.existsByAccessKey(action.getActionKey())) {
            return;
        }

        AccessKeyMetadata metadata = new AccessKeyMetadata();
        metadata.setAccessKey(action.getActionKey());
        metadata.setDisplayName(action.getDisplayName());
        metadata.setDescription(action.getDescription());
        metadata.setCategory(ACCESS_CATEGORY);
        metadata.setActive(action.isActive());
        metadata.setSystemKey(false);
        metadata.setProtectedKey(false);
        metadata.setSortOrder(action.getSortOrder());
        accessKeyMetadataRepository.save(metadata);
    }

    private void syncAccessMetadata(WorkflowAction action) {
        AccessKeyMetadata metadata = accessKeyMetadataRepository.findByAccessKey(action.getActionKey())
                .orElseGet(() -> {
                    AccessKeyMetadata created = new AccessKeyMetadata();
                    created.setAccessKey(action.getActionKey());
                    created.setSystemKey(false);
                    created.setProtectedKey(false);
                    created.setCategory(ACCESS_CATEGORY);
                    return created;
                });

        if (metadata.isSystemKey() || metadata.isProtectedKey()) {
            return;
        }

        metadata.setDisplayName(action.getDisplayName());
        metadata.setDescription(action.getDescription());
        metadata.setCategory(ACCESS_CATEGORY);
        metadata.setActive(action.isActive());
        metadata.setSortOrder(action.getSortOrder());
        accessKeyMetadataRepository.save(metadata);
    }

    private WorkflowAction findAction(Long id) {
        return workflowActionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow action not found"));
    }

    private void requireCustomEditable(WorkflowAction action, String employeeId, String operation) {
        if (action.isSystemAction() || action.isProtectedAction() || isProtectedSystemAction(action.getActionKey())) {
            log.warn("event=workflow_action_{}_rejected employeeId={} actionId={} actionKey={} result=protected",
                    operation, employeeId, action.getId(), action.getActionKey());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System and protected workflow actions are read-only");
        }
    }

    private String validateNewActionKey(String rawActionKey) {
        String actionKey = rawActionKey == null ? "" : rawActionKey.trim().toUpperCase(java.util.Locale.ROOT);
        if (!ACTION_KEY_PATTERN.matcher(actionKey).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action key must contain 2 to 60 uppercase letters, digits, or underscores");
        }
        if (isProtectedSystemAction(actionKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Protected workflow action keys cannot be used for custom actions");
        }
        return actionKey;
    }

    private boolean isProtectedSystemAction(String actionKey) {
        try {
            AccessKey.valueOf(actionKey);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String validateRequiredText(String value, String fieldName, int maxLength) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        if (text.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must contain at most " + maxLength + " characters");
        }
        return text;
    }

    private String validateOptionalText(String value, String fieldName, int maxLength, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.trim();
        if (text.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must contain at most " + maxLength + " characters");
        }
        return text.isEmpty() ? fallback : text;
    }
}
