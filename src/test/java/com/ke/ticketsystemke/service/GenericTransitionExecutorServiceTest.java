package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.TicketRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericTransitionExecutorServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCategoryRepository ticketCategoryRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private WorkflowActionRepository workflowActionRepository;

    @Mock
    private WorkflowStatusRepository workflowStatusRepository;

    @Mock
    private EffectiveStatusResolver effectiveStatusResolver;

    @Mock
    private AccessService accessService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TicketChargeService ticketChargeService;

    @Mock
    private TicketWorkflowHistoryService ticketWorkflowHistoryService;

    @Mock
    private WorkflowTransitionRoleRuleRepository workflowTransitionRoleRuleRepository;

    @Mock
    private WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository;

    @Mock
    private RepairWorkflowFeatureFlag repairWorkflowFeatureFlag;

    private GenericTransitionExecutorService service;

    @BeforeEach
    void setUp() {
        service = new GenericTransitionExecutorService(
                ticketRepository,
                ticketCategoryRepository,
                workflowTransitionRepository,
                workflowActionRepository,
                workflowStatusRepository,
                effectiveStatusResolver,
                accessService,
                employeeRepository,
                ticketChargeService,
                ticketWorkflowHistoryService,
                new WorkflowTransitionRoleScopeValidator(workflowTransitionRoleRuleRepository, null, null),
                new WorkflowTransitionCategoryScopeValidator(workflowTransitionCategoryRuleRepository, ticketCategoryRepository),
                repairWorkflowFeatureFlag
        );
    }

    @Test
    void prepareExecutionAllowsCustomTerminalTargetStatus() {
        WorkflowStatus repairCompleted = customStatus(11L, "REPAIR_COMPLETED", TicketStatus.IN_PROGRESS, false);
        WorkflowStatus delivered = customStatus(14L, "DELIVERED_TO_CUSTOMER", TicketStatus.COMPLETED, true);
        WorkflowAction action = action("DELIVER_TO_CUSTOMER");
        WorkflowTransition transition = transition(78L, action.getActionKey(), repairCompleted, delivered);
        Ticket ticket = ticket(repairCompleted);

        when(ticketRepository.findById(4L)).thenReturn(Optional.of(ticket));
        when(workflowTransitionRepository.findById(78L)).thenReturn(Optional.of(transition));
        when(workflowActionRepository.findByActionKey("DELIVER_TO_CUSTOMER")).thenReturn(Optional.of(action));
        when(effectiveStatusResolver.resolve(ticket)).thenReturn(new ResolvedTicketStatus(
                TicketStatus.IN_PROGRESS,
                11L,
                "REPAIR_COMPLETED",
                "Repair Completed / Ready for Delivery",
                true,
                false,
                TicketStatus.IN_PROGRESS,
                false,
                false,
                false,
                false,
                false,
                false,
                ResolvedTicketStatus.WarningCode.NONE
        ));
        when(repairWorkflowFeatureFlag.isEnabled()).thenReturn(true);
        when(workflowTransitionRoleRuleRepository.existsByWorkflowTransition_Id(78L)).thenReturn(false);
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(78L)).thenReturn(false);

        GenericTransitionExecutorService.GenericTransitionExecutionPlan plan =
                service.prepareExecution(4L, 78L, "SUPER_ADMIN_001");

        assertThat(plan.toStatus()).isSameAs(delivered);
        assertThat(plan.toStatusBehaviorBucket()).isEqualTo(TicketStatus.COMPLETED);
        assertThat(plan.customTransition()).isTrue();
    }

    private Ticket ticket(WorkflowStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(4L);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setStatusRecord(status);
        return ticket;
    }

    private WorkflowTransition transition(
            Long id,
            String actionKey,
            WorkflowStatus fromStatus,
            WorkflowStatus toStatus
    ) {
        WorkflowTransition transition = new WorkflowTransition();
        ReflectionTestUtils.setField(transition, "id", id);
        transition.setActionKey(actionKey);
        transition.setDisplayName("Deliver To Customer");
        transition.setFromStatus(fromStatus.getBehaviorBucket());
        transition.setToStatus(toStatus.getBehaviorBucket());
        transition.setFromStatusRecord(fromStatus);
        transition.setToStatusRecord(toStatus);
        transition.setActive(true);
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);
        return transition;
    }

    private WorkflowAction action(String actionKey) {
        WorkflowAction action = new WorkflowAction();
        action.setActionKey(actionKey);
        action.setDisplayName("Deliver To Customer");
        action.setButtonLabel("Deliver To Customer");
        action.setActive(true);
        action.setSystemAction(false);
        action.setProtectedAction(false);
        return action;
    }

    private WorkflowStatus customStatus(
            Long id,
            String statusKey,
            TicketStatus behaviorBucket,
            boolean terminal
    ) {
        WorkflowStatus status = new WorkflowStatus();
        ReflectionTestUtils.setField(status, "id", id);
        status.setStatusKey(statusKey);
        status.setDisplayName(statusKey);
        status.setBehaviorBucket(behaviorBucket);
        status.setActive(true);
        status.setSystemStatus(false);
        status.setProtectedStatus(false);
        status.setTerminal(terminal);
        return status;
    }
}
