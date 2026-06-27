package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.UpdateWorkflowTransitionRequest;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionCategoryRuleRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRoleRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTransitionActivationTest {

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private WorkflowStatusRepository workflowStatusRepository;

    @Mock
    private WorkflowActionRepository workflowActionRepository;

    @Mock
    private WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository;

    @Mock
    private WorkflowTransitionRoleRuleRepository workflowTransitionRoleRuleRepository;

    @Mock
    private TicketCategoryRepository ticketCategoryRepository;

    @Mock
    private RoleRepository roleRepository;

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new WorkflowService(
                workflowTransitionRepository,
                employeeRepository,
                workflowStatusRepository,
                workflowActionRepository,
                workflowTransitionCategoryRuleRepository,
                workflowTransitionRoleRuleRepository,
                ticketCategoryRepository,
                roleRepository
        );
    }

    @Test
    void activatingTransitionWithInactiveActionIsRejected() {
        WorkflowTransition transition = transition();
        WorkflowAction inactiveAction = new WorkflowAction();
        inactiveAction.setActionKey("CUSTOM_FLOW");
        inactiveAction.setDisplayName("Custom Flow");
        inactiveAction.setButtonLabel("Custom Flow");
        inactiveAction.setActive(false);
        UpdateWorkflowTransitionRequest request = new UpdateWorkflowTransitionRequest();
        request.setActive(true);

        when(workflowTransitionRepository.findById(100L)).thenReturn(Optional.of(transition));
        when(workflowActionRepository.findByActionKey("CUSTOM_FLOW")).thenReturn(Optional.of(inactiveAction));

        assertThatThrownBy(() -> workflowService.updateTransition(100L, request, "admin-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Workflow action is inactive or missing");
    }

    private WorkflowTransition transition() {
        WorkflowStatus fromStatus = systemStatus(1L, "NEW", TicketStatus.NEW, false);
        WorkflowStatus toStatus = systemStatus(2L, "IN_PROGRESS", TicketStatus.IN_PROGRESS, false);

        WorkflowTransition transition = new WorkflowTransition();
        ReflectionTestUtils.setField(transition, "id", 100L);
        transition.setActionKey("CUSTOM_FLOW");
        transition.setDisplayName("Custom Flow");
        transition.setFromStatus(TicketStatus.NEW);
        transition.setToStatus(TicketStatus.IN_PROGRESS);
        transition.setFromStatusRecord(fromStatus);
        transition.setToStatusRecord(toStatus);
        transition.setActive(false);
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);
        return transition;
    }

    private WorkflowStatus systemStatus(Long id, String statusKey, TicketStatus behaviorBucket, boolean terminal) {
        WorkflowStatus status = new WorkflowStatus();
        ReflectionTestUtils.setField(status, "id", id);
        status.setStatusKey(statusKey);
        status.setDisplayName(statusKey);
        status.setBehaviorBucket(behaviorBucket);
        status.setActive(true);
        status.setSystemStatus(true);
        status.setProtectedStatus(true);
        status.setTerminal(terminal);
        return status;
    }
}
