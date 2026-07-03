package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.WorkflowValidationResponse;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.entity.WorkflowTransitionRoleRule;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import com.ke.ticketsystemke.repository.RoleAccessRuleRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionCategoryRuleRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRoleRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowValidationServiceTest {

    @Mock
    private TicketCategoryRepository ticketCategoryRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private WorkflowActionRepository workflowActionRepository;

    @Mock
    private WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository;

    @Mock
    private WorkflowTransitionRoleRuleRepository workflowTransitionRoleRuleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleAccessRuleRepository roleAccessRuleRepository;

    @Mock
    private AccessKeyMetadataRepository accessKeyMetadataRepository;

    private WorkflowValidationService workflowValidationService;

    @BeforeEach
    void setUp() {
        workflowValidationService = new WorkflowValidationService(
                ticketCategoryRepository,
                workflowTransitionRepository,
                workflowActionRepository,
                workflowTransitionCategoryRuleRepository,
                workflowTransitionRoleRuleRepository,
                roleRepository,
                roleAccessRuleRepository,
                accessKeyMetadataRepository
        );
    }

    @Test
    void validationReportsMissingRoleAccessGrant() {
        TicketCategoryConfig category = category();
        WorkflowAction action = action();
        WorkflowTransition transition = transition(100L, status(1L, "NEW", TicketStatus.NEW, false), status(2L, "IN_PROGRESS", TicketStatus.IN_PROGRESS, false));
        Role technician = role(20L, "TECHNICIAN");
        WorkflowTransitionRoleRule roleRule = new WorkflowTransitionRoleRule();
        roleRule.setWorkflowTransition(transition);
        roleRule.setRole(technician);
        roleRule.setActive(true);

        when(ticketCategoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(workflowTransitionRepository.findAll()).thenReturn(List.of(transition));
        when(workflowActionRepository.findByActionKey("CUSTOM_FLOW")).thenReturn(Optional.of(action));
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(100L)).thenReturn(false);
        when(workflowTransitionRoleRuleRepository.findAllByWorkflowTransition_IdOrderByIdAsc(100L)).thenReturn(List.of(roleRule));
        when(roleRepository.findAll()).thenReturn(List.of(technician));
        when(roleAccessRuleRepository.findByRoleIdAndAccessKey(20L, "CUSTOM_FLOW")).thenReturn(Optional.empty());

        WorkflowValidationResponse response = workflowValidationService.validateCategoryWorkflow(3L, true);

        assertThat(response.readyToActivate()).isFalse();
        assertThat(response.blockingIssues())
                .anyMatch(issue -> "MISSING_ROLE_ACCESS_GRANT".equals(issue.code()))
                .anyMatch(issue -> "NOT_EXECUTABLE_BY_ACTIVE_ROLE".equals(issue.code()));
    }

    @Test
    void validationReportsMissingReachableTerminalPath() {
        TicketCategoryConfig category = category();
        WorkflowAction action = action();
        WorkflowTransition transition = transition(100L, status(1L, "NEW", TicketStatus.NEW, false), status(2L, "IN_PROGRESS", TicketStatus.IN_PROGRESS, false));
        Role superAdmin = role(1L, "SUPER_ADMIN");

        when(ticketCategoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(workflowTransitionRepository.findAll()).thenReturn(List.of(transition));
        when(workflowActionRepository.findByActionKey("CUSTOM_FLOW")).thenReturn(Optional.of(action));
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(100L)).thenReturn(false);
        when(workflowTransitionRoleRuleRepository.findAllByWorkflowTransition_IdOrderByIdAsc(100L)).thenReturn(List.of());
        when(roleRepository.findAll()).thenReturn(List.of(superAdmin));

        WorkflowValidationResponse response = workflowValidationService.validateCategoryWorkflow(3L, true);

        assertThat(response.readyToActivate()).isFalse();
        assertThat(response.blockingIssues())
                .anyMatch(issue -> "MISSING_TERMINAL_COMPLETION_PATH".equals(issue.code()));
        assertThat(response.warnings())
                .anyMatch(issue -> "MISSING_ROLE_TRANSITION_SCOPE_COVERAGE".equals(issue.code()));
    }

    @Test
    void validationAcceptsCustomNewBehaviorBucketAsReachabilityStart() {
        TicketCategoryConfig category = category();
        WorkflowAction action = action();
        WorkflowTransition startTransition = transition(
                100L,
                status(1L, "CUSTOM_NEW", TicketStatus.NEW, false),
                status(2L, "CUSTOM_IN_PROGRESS", TicketStatus.IN_PROGRESS, false)
        );
        WorkflowTransition completeTransition = transition(
                101L,
                startTransition.getToStatusRecord(),
                status(3L, "CUSTOM_DONE", TicketStatus.COMPLETED, true)
        );
        Role superAdmin = role(1L, "SUPER_ADMIN");

        when(ticketCategoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(workflowTransitionRepository.findAll()).thenReturn(List.of(startTransition, completeTransition));
        when(workflowActionRepository.findByActionKey("CUSTOM_FLOW")).thenReturn(Optional.of(action));
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(100L)).thenReturn(false);
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(101L)).thenReturn(false);
        when(workflowTransitionRoleRuleRepository.findAllByWorkflowTransition_IdOrderByIdAsc(100L)).thenReturn(List.of());
        when(workflowTransitionRoleRuleRepository.findAllByWorkflowTransition_IdOrderByIdAsc(101L)).thenReturn(List.of());
        when(roleRepository.findAll()).thenReturn(List.of(superAdmin));

        WorkflowValidationResponse response = workflowValidationService.validateCategoryWorkflow(3L, true);

        assertThat(response.readyToActivate()).isTrue();
        assertThat(response.blockingIssues())
                .noneMatch(issue -> "UNREACHABLE_WORKFLOW_FROM_NEW".equals(issue.code()));
        assertThat(response.warnings())
                .hasSize(2)
                .allMatch(issue -> "MISSING_ROLE_TRANSITION_SCOPE_COVERAGE".equals(issue.code()));
    }

    @Test
    void validationAcceptsCustomNewBehaviorBucketStartWhenCategoryRuleIsEnabled() {
        TicketCategoryConfig category = category();
        WorkflowAction startAction = action("START_SIMPLE_WORK");
        WorkflowAction completeAction = action("COMPLETE_SIMPLE_WORK");
        WorkflowStatus customNew = status(1L, "CUSTOM_NEW", TicketStatus.NEW, false);
        WorkflowStatus customInProgress = status(2L, "CUSTOM_IN_PROGRESS", TicketStatus.IN_PROGRESS, false);
        WorkflowStatus customDone = status(3L, "CUSTOM_DONE", TicketStatus.COMPLETED, true);
        WorkflowTransition startTransition = transition(100L, "START_SIMPLE_WORK", customNew, customInProgress);
        WorkflowTransition completeTransition = transition(101L, "COMPLETE_SIMPLE_WORK", customInProgress, customDone);
        Role superAdmin = role(1L, "SUPER_ADMIN");

        when(ticketCategoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(workflowTransitionRepository.findAll()).thenReturn(List.of(startTransition, completeTransition));
        when(workflowActionRepository.findByActionKey("START_SIMPLE_WORK")).thenReturn(Optional.of(startAction));
        when(workflowActionRepository.findByActionKey("COMPLETE_SIMPLE_WORK")).thenReturn(Optional.of(completeAction));
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(100L)).thenReturn(true);
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(101L)).thenReturn(true);
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(100L, 3L))
                .thenReturn(true);
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(101L, 3L))
                .thenReturn(true);
        when(workflowTransitionRoleRuleRepository.findAllByWorkflowTransition_IdOrderByIdAsc(100L)).thenReturn(List.of());
        when(workflowTransitionRoleRuleRepository.findAllByWorkflowTransition_IdOrderByIdAsc(101L)).thenReturn(List.of());
        when(roleRepository.findAll()).thenReturn(List.of(superAdmin));

        WorkflowValidationResponse response = workflowValidationService.validateCategoryWorkflow(3L, true);

        assertThat(response.readyToActivate()).isTrue();
        assertThat(response.blockingIssues()).isEmpty();
        assertThat(response.warnings())
                .hasSize(2)
                .allMatch(issue -> "MISSING_ROLE_TRANSITION_SCOPE_COVERAGE".equals(issue.code()));
        assertThat(response.warnings())
                .extracting("transitionId")
                .containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void validationReportsStaleTransitionTargetBehavior() {
        TicketCategoryConfig category = category();
        WorkflowAction action = action();
        WorkflowTransition transition = transition(
                100L,
                status(1L, "CHANGRED", TicketStatus.IN_PROGRESS, false),
                status(2L, "DELIVERD", TicketStatus.COMPLETED, true)
        );
        transition.setToStatus(TicketStatus.IN_PROGRESS);
        Role superAdmin = role(1L, "SUPER_ADMIN");

        when(ticketCategoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(workflowTransitionRepository.findAll()).thenReturn(List.of(transition));
        when(workflowActionRepository.findByActionKey("CUSTOM_FLOW")).thenReturn(Optional.of(action));
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(100L)).thenReturn(false);
        when(workflowTransitionRoleRuleRepository.findAllByWorkflowTransition_IdOrderByIdAsc(100L)).thenReturn(List.of());
        when(roleRepository.findAll()).thenReturn(List.of(superAdmin));

        WorkflowValidationResponse response = workflowValidationService.validateCategoryWorkflow(3L, true);

        assertThat(response.readyToActivate()).isFalse();
        assertThat(response.blockingIssues())
                .anyMatch(issue -> "STALE_TARGET_STATUS_BEHAVIOR".equals(issue.code()));
    }

    @Test
    void validationAcceptsProtectedFixedTransitionsWhenExplicitlyEnabledForCategory() {
        TicketCategoryConfig category = category();
        WorkflowAction action = action();
        action.setProtectedAction(true);
        WorkflowTransition startTransition = transition(
                100L,
                status(1L, "NEW", TicketStatus.NEW, false),
                status(2L, "PICKED", TicketStatus.PICKED, false)
        );
        startTransition.setProtectedTransition(true);
        WorkflowTransition completeTransition = transition(
                101L,
                startTransition.getToStatusRecord(),
                status(3L, "COMPLETED", TicketStatus.COMPLETED, true)
        );
        completeTransition.setProtectedTransition(true);
        Role superAdmin = role(1L, "SUPER_ADMIN");

        when(ticketCategoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(workflowTransitionRepository.findAll()).thenReturn(List.of(startTransition, completeTransition));
        when(workflowActionRepository.findByActionKey("CUSTOM_FLOW")).thenReturn(Optional.of(action));
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(100L, 3L))
                .thenReturn(true);
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(101L, 3L))
                .thenReturn(true);
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(100L)).thenReturn(true);
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(101L)).thenReturn(true);
        when(workflowTransitionRoleRuleRepository.findAllByWorkflowTransition_IdOrderByIdAsc(100L)).thenReturn(List.of());
        when(workflowTransitionRoleRuleRepository.findAllByWorkflowTransition_IdOrderByIdAsc(101L)).thenReturn(List.of());
        when(roleRepository.findAll()).thenReturn(List.of(superAdmin));

        WorkflowValidationResponse response = workflowValidationService.validateCategoryWorkflow(3L, true);

        assertThat(response.readyToActivate()).isTrue();
        assertThat(response.blockingIssues()).isEmpty();
    }

    private TicketCategoryConfig category() {
        TicketCategoryConfig category = new TicketCategoryConfig();
        ReflectionTestUtils.setField(category, "id", 3L);
        category.setCategoryKey("TEST_CATEGORY");
        category.setDisplayName("Test Category");
        category.setActive(true);
        return category;
    }

    private WorkflowAction action() {
        return action("CUSTOM_FLOW");
    }

    private WorkflowAction action(String actionKey) {
        WorkflowAction action = new WorkflowAction();
        action.setActionKey(actionKey);
        action.setDisplayName(actionKey);
        action.setButtonLabel(actionKey);
        action.setActive(true);
        return action;
    }

    private WorkflowTransition transition(Long id, WorkflowStatus fromStatus, WorkflowStatus toStatus) {
        return transition(id, "CUSTOM_FLOW", fromStatus, toStatus);
    }

    private WorkflowTransition transition(Long id, String actionKey, WorkflowStatus fromStatus, WorkflowStatus toStatus) {
        WorkflowTransition transition = new WorkflowTransition();
        ReflectionTestUtils.setField(transition, "id", id);
        transition.setActionKey(actionKey);
        transition.setDisplayName(actionKey);
        transition.setFromStatus(fromStatus.getBehaviorBucket());
        transition.setToStatus(toStatus.getBehaviorBucket());
        transition.setFromStatusRecord(fromStatus);
        transition.setToStatusRecord(toStatus);
        transition.setActive(true);
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);
        return transition;
    }

    private WorkflowStatus status(Long id, String statusKey, TicketStatus behaviorBucket, boolean terminal) {
        WorkflowStatus status = new WorkflowStatus();
        ReflectionTestUtils.setField(status, "id", id);
        status.setStatusKey(statusKey);
        status.setDisplayName(statusKey);
        status.setBehaviorBucket(behaviorBucket);
        status.setActive(true);
        status.setTerminal(terminal);
        return status;
    }

    private Role role(Long id, String roleKey) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "id", id);
        role.setRoleKey(roleKey);
        role.setDisplayName(roleKey);
        role.setActive(true);
        return role;
    }
}
