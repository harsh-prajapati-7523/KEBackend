package com.ke.ticketsystemke.config;

import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.RoleAccessRule;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import com.ke.ticketsystemke.repository.RoleAccessRuleRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import com.ke.ticketsystemke.service.RepairWorkflowFeatureFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RepairWorkflowConfigurationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RepairWorkflowConfigurationInitializer.class);
    private static final String SUPER_ADMIN_ROLE_KEY = "SUPER_ADMIN";
    private static final String ACCESS_CATEGORY = "Repair Workflow";

    private static final List<StatusSeed> STATUS_SEEDS = List.of(
            new StatusSeed("MISSING_PART", "Missing Part", TicketStatus.IN_PROGRESS, false, 110),
            new StatusSeed("PART_AVAILABLE", "Part Available", TicketStatus.IN_PROGRESS, false, 120),
            new StatusSeed("CUSTOMER_APPROVAL_PENDING", "Customer Approval Pending", TicketStatus.IN_PROGRESS, false, 130),
            new StatusSeed("IN_WARRANTY", "In Warranty", TicketStatus.IN_PROGRESS, false, 140),
            new StatusSeed("WARRANTY_COMPLAINT_LOGGED", "Warranty Complaint Logged", TicketStatus.IN_PROGRESS, false, 150),
            new StatusSeed("REPAIR_COMPLETED", "Repair Completed / Ready for Delivery", TicketStatus.IN_PROGRESS, false, 160),
            new StatusSeed("CUSTOMER_DECLINED", "Customer Declined Repair", TicketStatus.IN_PROGRESS, false, 170),
            new StatusSeed("CANCELLED_PENDING_DELIVERY", "Cancelled / Return Pending", TicketStatus.IN_PROGRESS, false, 180),
            new StatusSeed("DELIVERED_TO_CUSTOMER", "Delivered To Customer", TicketStatus.COMPLETED, true, 190)
    );

    private static final List<ActionSeed> ACTION_SEEDS = List.of(
            new ActionSeed("START_REPAIR_WORK", "Start Work", "Start Work", "Move a new repair ticket into work in progress.", 110),
            new ActionSeed("MARK_MISSING_PART", "Mark Missing Part", "Mark Missing Part", "Mark that a required part is missing.", 120),
            new ActionSeed("MARK_PART_AVAILABLE", "Mark Part Available", "Mark Part Available", "Mark that the required part is now available.", 130),
            new ActionSeed("RESUME_WORK", "Resume Work", "Resume Work", "Move the ticket back into active repair work.", 140),
            new ActionSeed("NEED_CUSTOMER_APPROVAL", "Need Customer Approval", "Need Customer Approval", "Move the ticket to customer approval pending.", 150),
            new ActionSeed("CUSTOMER_APPROVED", "Customer Approved", "Customer Approved", "Resume repair after customer approval.", 160),
            new ActionSeed("MARK_IN_WARRANTY", "Mark In Warranty", "Mark In Warranty", "Mark the repair as in warranty workflow.", 170),
            new ActionSeed("LOG_WARRANTY_COMPLAINT", "Log Warranty Complaint", "Log Warranty Complaint", "Record that a warranty complaint has been logged.", 180),
            new ActionSeed("MARK_REPAIR_COMPLETED", "Mark Repair Completed", "Mark Repair Completed", "Mark repair as completed and ready for delivery.", 190),
            new ActionSeed("CUSTOMER_DECLINED_REPAIR", "Customer Declined Repair", "Customer Declined Repair", "Mark that the customer declined the repair.", 200),
            new ActionSeed("CANCEL_PENDING_DELIVERY", "Cancel / Return Pending", "Cancel / Return Pending", "Mark the ticket as cancelled with return pending.", 210),
            new ActionSeed("DELIVER_TO_CUSTOMER", "Delivered To Customer", "Delivered To Customer", "Close the ticket after delivery to customer.", 220)
    );

    private static final List<TransitionSeed> TRANSITION_SEEDS = List.of(
            new TransitionSeed("START_REPAIR_WORK", "Start Work", "NEW", "IN_PROGRESS", 110),
            new TransitionSeed("MARK_MISSING_PART", "Mark Missing Part", "IN_PROGRESS", "MISSING_PART", 120),
            new TransitionSeed("NEED_CUSTOMER_APPROVAL", "Need Customer Approval", "IN_PROGRESS", "CUSTOMER_APPROVAL_PENDING", 130),
            new TransitionSeed("MARK_IN_WARRANTY", "Mark In Warranty", "IN_PROGRESS", "IN_WARRANTY", 140),
            new TransitionSeed("MARK_REPAIR_COMPLETED", "Mark Repair Completed", "IN_PROGRESS", "REPAIR_COMPLETED", 150),
            new TransitionSeed("CUSTOMER_DECLINED_REPAIR", "Customer Declined Repair", "IN_PROGRESS", "CUSTOMER_DECLINED", 160),
            new TransitionSeed("CANCEL_PENDING_DELIVERY", "Cancel / Return Pending", "IN_PROGRESS", "CANCELLED_PENDING_DELIVERY", 170),
            new TransitionSeed("MARK_PART_AVAILABLE", "Mark Part Available", "MISSING_PART", "PART_AVAILABLE", 180),
            new TransitionSeed("CUSTOMER_DECLINED_REPAIR", "Customer Declined Repair", "MISSING_PART", "CUSTOMER_DECLINED", 190),
            new TransitionSeed("CANCEL_PENDING_DELIVERY", "Cancel / Return Pending", "MISSING_PART", "CANCELLED_PENDING_DELIVERY", 200),
            new TransitionSeed("RESUME_WORK", "Resume Work", "PART_AVAILABLE", "IN_PROGRESS", 210),
            new TransitionSeed("MARK_REPAIR_COMPLETED", "Mark Repair Completed", "PART_AVAILABLE", "REPAIR_COMPLETED", 220),
            new TransitionSeed("MARK_MISSING_PART", "Mark Missing Part", "PART_AVAILABLE", "MISSING_PART", 230),
            new TransitionSeed("CUSTOMER_DECLINED_REPAIR", "Customer Declined Repair", "PART_AVAILABLE", "CUSTOMER_DECLINED", 240),
            new TransitionSeed("CUSTOMER_APPROVED", "Customer Approved", "CUSTOMER_APPROVAL_PENDING", "IN_PROGRESS", 250),
            new TransitionSeed("CUSTOMER_DECLINED_REPAIR", "Customer Declined Repair", "CUSTOMER_APPROVAL_PENDING", "CUSTOMER_DECLINED", 260),
            new TransitionSeed("LOG_WARRANTY_COMPLAINT", "Log Warranty Complaint", "IN_WARRANTY", "WARRANTY_COMPLAINT_LOGGED", 270),
            new TransitionSeed("CUSTOMER_DECLINED_REPAIR", "Customer Declined Repair", "IN_WARRANTY", "CUSTOMER_DECLINED", 280),
            new TransitionSeed("CANCEL_PENDING_DELIVERY", "Cancel / Return Pending", "IN_WARRANTY", "CANCELLED_PENDING_DELIVERY", 290),
            new TransitionSeed("MARK_REPAIR_COMPLETED", "Mark Repair Completed", "WARRANTY_COMPLAINT_LOGGED", "REPAIR_COMPLETED", 300),
            new TransitionSeed("CUSTOMER_DECLINED_REPAIR", "Customer Declined Repair", "WARRANTY_COMPLAINT_LOGGED", "CUSTOMER_DECLINED", 310),
            new TransitionSeed("CANCEL_PENDING_DELIVERY", "Cancel / Return Pending", "WARRANTY_COMPLAINT_LOGGED", "CANCELLED_PENDING_DELIVERY", 320),
            new TransitionSeed("DELIVER_TO_CUSTOMER", "Delivered To Customer", "REPAIR_COMPLETED", "DELIVERED_TO_CUSTOMER", 330),
            new TransitionSeed("DELIVER_TO_CUSTOMER", "Delivered To Customer", "CUSTOMER_DECLINED", "DELIVERED_TO_CUSTOMER", 340),
            new TransitionSeed("DELIVER_TO_CUSTOMER", "Delivered To Customer", "CANCELLED_PENDING_DELIVERY", "DELIVERED_TO_CUSTOMER", 350)
    );

    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final AccessKeyMetadataRepository accessKeyMetadataRepository;
    private final RoleRepository roleRepository;
    private final RoleAccessRuleRepository roleAccessRuleRepository;
    private final RepairWorkflowFeatureFlag repairWorkflowFeatureFlag;

    public RepairWorkflowConfigurationInitializer(
            WorkflowStatusRepository workflowStatusRepository,
            WorkflowActionRepository workflowActionRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            AccessKeyMetadataRepository accessKeyMetadataRepository,
            RoleRepository roleRepository,
            RoleAccessRuleRepository roleAccessRuleRepository,
            RepairWorkflowFeatureFlag repairWorkflowFeatureFlag
    ) {
        this.workflowStatusRepository = workflowStatusRepository;
        this.workflowActionRepository = workflowActionRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.accessKeyMetadataRepository = accessKeyMetadataRepository;
        this.roleRepository = roleRepository;
        this.roleAccessRuleRepository = roleAccessRuleRepository;
        this.repairWorkflowFeatureFlag = repairWorkflowFeatureFlag;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, WorkflowStatus> statusesByKey = ensureStatuses();
        Map<String, WorkflowAction> actionsByKey = ensureActions();
        ensureAccessMetadata(actionsByKey);
        if (repairWorkflowFeatureFlag.isEnabled()) {
            ensureSuperAdminGrants(actionsByKey);
        }
        if (!repairWorkflowFeatureFlag.isEnabled()) {
            deactivateRepairWorkflowTransitions();
        }
        log.info("event=repair_workflow_configuration_ensured statusCount={} actionCount={} transitionSeedCount={} transitionsSeeded=false",
                STATUS_SEEDS.size(), ACTION_SEEDS.size(), TRANSITION_SEEDS.size());
    }

    private Map<String, WorkflowStatus> ensureStatuses() {
        Map<String, WorkflowStatus> statusesByKey = workflowStatusRepository.findAll()
                .stream()
                .collect(Collectors.toMap(WorkflowStatus::getStatusKey, Function.identity()));

        ensureSystemStatus(statusesByKey, "NEW");
        ensureSystemStatus(statusesByKey, "IN_PROGRESS");

        for (StatusSeed seed : STATUS_SEEDS) {
            WorkflowStatus status = statusesByKey.get(seed.statusKey());
            if (status == null) {
                status = new WorkflowStatus();
                status.setStatusKey(seed.statusKey());
                status.setDisplayName(seed.displayName());
                status.setActive(true);
                status.setSystemStatus(false);
                status.setProtectedStatus(false);
                status.setTerminal(seed.terminal());
                status.setBehaviorBucket(seed.behaviorBucket());
                status.setSortOrder(seed.sortOrder());
                workflowStatusRepository.save(status);
                statusesByKey.put(status.getStatusKey(), status);
                continue;
            }

            if (!status.isSystemStatus() && !status.isProtectedStatus()) {
                status.setDisplayName(seed.displayName());
                status.setTerminal(seed.terminal());
                status.setBehaviorBucket(seed.behaviorBucket());
                status.setSortOrder(seed.sortOrder());
                workflowStatusRepository.save(status);
            }
        }

        return workflowStatusRepository.findAll()
                .stream()
                .collect(Collectors.toMap(WorkflowStatus::getStatusKey, Function.identity()));
    }

    private void ensureSystemStatus(Map<String, WorkflowStatus> statusesByKey, String statusKey) {
        WorkflowStatus status = statusesByKey.get(statusKey);
        if (status == null || !status.isActive() || status.getBehaviorBucket() == null) {
            throw new IllegalStateException("Required system workflow status is missing or invalid: " + statusKey);
        }
    }

    private Map<String, WorkflowAction> ensureActions() {
        Map<String, WorkflowAction> actionsByKey = workflowActionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(WorkflowAction::getActionKey, Function.identity()));

        for (ActionSeed seed : ACTION_SEEDS) {
            WorkflowAction action = actionsByKey.get(seed.actionKey());
            if (action == null) {
                action = new WorkflowAction();
                action.setActionKey(seed.actionKey());
                action.setDisplayName(seed.displayName());
                action.setButtonLabel(seed.buttonLabel());
                action.setDescription(seed.description());
                action.setActive(true);
                action.setSystemAction(false);
                action.setProtectedAction(false);
                action.setSortOrder(seed.sortOrder());
                action.setRequiresComment(false);
                action.setConfirmationRequired(false);
                workflowActionRepository.save(action);
                actionsByKey.put(action.getActionKey(), action);
            }
        }

        return workflowActionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(WorkflowAction::getActionKey, Function.identity()));
    }

    private void ensureAccessMetadata(Map<String, WorkflowAction> actionsByKey) {
        for (ActionSeed seed : ACTION_SEEDS) {
            WorkflowAction action = actionsByKey.get(seed.actionKey());
            if (action == null || accessKeyMetadataRepository.existsByAccessKey(seed.actionKey())) {
                continue;
            }

            AccessKeyMetadata metadata = new AccessKeyMetadata();
            metadata.setAccessKey(seed.actionKey());
            metadata.setDisplayName(seed.displayName());
            metadata.setDescription(seed.description());
            metadata.setCategory(ACCESS_CATEGORY);
            metadata.setActive(true);
            metadata.setSystemKey(false);
            metadata.setProtectedKey(false);
            metadata.setSortOrder(seed.sortOrder());
            accessKeyMetadataRepository.save(metadata);
        }
    }

    private void ensureSuperAdminGrants(Map<String, WorkflowAction> actionsByKey) {
        Role superAdmin = roleRepository.findByRoleKey(SUPER_ADMIN_ROLE_KEY).orElse(null);
        if (superAdmin == null) {
            return;
        }

        for (String actionKey : actionsByKey.keySet()) {
            if (ACTION_SEEDS.stream().noneMatch(seed -> seed.actionKey().equals(actionKey))) {
                continue;
            }

            RoleAccessRule rule = roleAccessRuleRepository.findByRoleIdAndAccessKey(superAdmin.getId(), actionKey)
                    .orElseGet(() -> {
                        RoleAccessRule created = new RoleAccessRule();
                        created.setRole(superAdmin);
                        created.setAccessKey(actionKey);
                        return created;
                    });
            if (!rule.isAllowed()) {
                rule.setAllowed(true);
                roleAccessRuleRepository.save(rule);
            }
        }
    }

    private void deactivateRepairWorkflowTransitions() {
        List<WorkflowTransition> transitions = workflowTransitionRepository
                .findByActionKeyIn(repairWorkflowFeatureFlag.repairWorkflowActionKeys());
        for (WorkflowTransition transition : transitions) {
            if (!transition.isActive()) {
                continue;
            }
            transition.setActive(false);
            workflowTransitionRepository.save(transition);
        }
    }

    private record StatusSeed(
            String statusKey,
            String displayName,
            TicketStatus behaviorBucket,
            boolean terminal,
            Integer sortOrder
    ) {
    }

    private record ActionSeed(
            String actionKey,
            String displayName,
            String buttonLabel,
            String description,
            Integer sortOrder
    ) {
    }

    private record TransitionSeed(
            String actionKey,
            String displayName,
            String fromStatusKey,
            String toStatusKey,
            Integer sortOrder
    ) {
    }
}
