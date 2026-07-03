package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateTicketRequest;
import com.ke.ticketsystemke.dto.TicketAvailableActionsResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowMode;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.CategoryFieldConfigRepository;
import com.ke.ticketsystemke.repository.DropdownOptionRepository;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.TicketDynamicValueRepository;
import com.ke.ticketsystemke.repository.TicketRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionCategoryRuleRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceAvailableActionsTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TicketCategoryRepository ticketCategoryRepository;

    @Mock
    private CategoryFieldConfigRepository categoryFieldConfigRepository;

    @Mock
    private DropdownOptionRepository dropdownOptionRepository;

    @Mock
    private TicketDynamicValueRepository ticketDynamicValueRepository;

    @Mock
    private TicketChargeService ticketChargeService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private AccessService accessService;

    @Mock
    private WorkflowStatusRepository workflowStatusRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository;

    @Mock
    private EffectiveStatusResolver effectiveStatusResolver;

    @Mock
    private GenericTransitionExecutorService genericTransitionExecutorService;

    @Mock
    private TicketWorkflowHistoryService ticketWorkflowHistoryService;

    @Mock
    private RepairWorkflowFeatureFlag repairWorkflowFeatureFlag;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository,
                employeeRepository,
                ticketCategoryRepository,
                categoryFieldConfigRepository,
                dropdownOptionRepository,
                ticketDynamicValueRepository,
                ticketChargeService,
                workflowService,
                accessService,
                workflowStatusRepository,
                workflowTransitionRepository,
                workflowTransitionCategoryRuleRepository,
                effectiveStatusResolver,
                genericTransitionExecutorService,
                ticketWorkflowHistoryService,
                repairWorkflowFeatureFlag
        );

        lenient().when(accessService.isAllowed(eq("tech-1"), any(AccessKey.class))).thenReturn(true);
        lenient().when(accessService.isAllowed(eq("admin-1"), any(AccessKey.class))).thenReturn(true);
        lenient().when(workflowService.isTransitionAllowed(any(AccessKey.class), any(TicketStatus.class), any(TicketStatus.class)))
                .thenReturn(false);
        lenient().when(workflowTransitionRepository.findByFromStatusAndActiveTrueOrderBySortOrderAscIdAsc(TicketStatus.IN_PROGRESS))
                .thenReturn(List.of());
    }

    @Test
    void availableActionsOmitsFixedActionMapForInProgressOwner() {
        Ticket ticket = inProgressTicket("tech-1");
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        TicketAvailableActionsResponse response = ticketService.getAvailableActions(10L, "tech-1", "TECHNICIAN");

        assertThat(response.ticketId()).isEqualTo(10L);
        assertThat(response.dynamicActions()).isEmpty();
    }

    @Test
    void dbConfiguredCategoryShowsCustomActionBetweenProtectedSystemStatuses() {
        WorkflowStatus newStatus = systemStatus(1L, "NEW", TicketStatus.NEW, false);
        WorkflowStatus inProgressStatus = systemStatus(3L, "IN_PROGRESS", TicketStatus.IN_PROGRESS, false);
        TicketCategoryConfig category = new TicketCategoryConfig();
        ReflectionTestUtils.setField(category, "id", 5L);
        category.setCategoryKey("REPAIR_WORKFLOW_TEST");
        category.setWorkflowMode(WorkflowMode.DB_CONFIGURED);
        category.setDbWorkflowEnabled(true);
        category.setFixedActionsEnabled(false);

        Ticket ticket = new Ticket();
        ticket.setId(20L);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setStatusRecord(newStatus);
        ticket.setCategoryRecord(category);

        WorkflowTransition transition = new WorkflowTransition();
        ReflectionTestUtils.setField(transition, "id", 8L);
        transition.setActionKey("START_REPAIR_WORK");
        transition.setDisplayName("Start Work");
        transition.setFromStatus(TicketStatus.NEW);
        transition.setToStatus(TicketStatus.IN_PROGRESS);
        transition.setFromStatusRecord(newStatus);
        transition.setToStatusRecord(inProgressStatus);
        transition.setActive(true);
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);

        WorkflowAction action = new WorkflowAction();
        action.setActionKey("START_REPAIR_WORK");
        action.setDisplayName("Start Work");
        action.setActive(true);
        action.setSystemAction(false);
        action.setProtectedAction(false);

        ResolvedTicketStatus currentStatus = new ResolvedTicketStatus(
                TicketStatus.NEW,
                1L,
                "NEW",
                "New",
                true,
                false,
                TicketStatus.NEW,
                false,
                false,
                false,
                false,
                false,
                false,
                ResolvedTicketStatus.WarningCode.NONE
        );

        when(ticketRepository.findById(20L)).thenReturn(Optional.of(ticket));
        when(workflowTransitionRepository.findByFromStatusRecord_IdAndActiveTrueOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(transition));
        when(repairWorkflowFeatureFlag.isEnabled()).thenReturn(true);
        when(genericTransitionExecutorService.prepareExecution(20L, 8L, "admin-1"))
                .thenReturn(new GenericTransitionExecutorService.GenericTransitionExecutionPlan(
                        ticket,
                        transition,
                        action,
                        newStatus,
                        inProgressStatus,
                        TicketStatus.IN_PROGRESS,
                        currentStatus,
                        "admin-1",
                        false,
                        true
                ));

        TicketAvailableActionsResponse response = ticketService.getAvailableActions(20L, "admin-1", "SUPER_ADMIN");

        assertThat(response.dynamicActions())
                .extracting(actionResponse -> actionResponse.actionKey())
                .containsExactly("START_REPAIR_WORK");
    }

    @Test
    void dbConfiguredDefaultCategoryDoesNotReturnFixedPickActionInAvailableActions() {
        WorkflowStatus newStatus = systemStatus(1L, "NEW", TicketStatus.NEW, false);
        TicketCategoryConfig category = new TicketCategoryConfig();
        ReflectionTestUtils.setField(category, "id", 5L);
        category.setCategoryKey("REPAIR_WORKFLOW_TEST");
        category.setWorkflowMode(WorkflowMode.DB_CONFIGURED);
        category.setDbWorkflowEnabled(true);
        category.setFixedActionsEnabled(true);

        Ticket ticket = new Ticket();
        ticket.setId(21L);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setStatusRecord(newStatus);
        ticket.setCategoryRecord(category);

        when(ticketRepository.findById(21L)).thenReturn(Optional.of(ticket));

        TicketAvailableActionsResponse response = ticketService.getAvailableActions(21L, "admin-1", "SUPER_ADMIN");

        assertThat(response.ticketId()).isEqualTo(21L);
        assertThat(response.dynamicActions()).isEmpty();
    }

    @Test
    void createTicketUsesConfiguredCustomNewStatusForDbConfiguredCategory() {
        WorkflowStatus customNew = customStatus(11L, "CUSTOM_NEW", TicketStatus.NEW, false);
        WorkflowStatus customInProgress = customStatus(12L, "CUSTOM_IN_PROGRESS", TicketStatus.IN_PROGRESS, false);
        TicketCategoryConfig category = new TicketCategoryConfig();
        ReflectionTestUtils.setField(category, "id", 5L);
        category.setCategoryKey("REPAIR_WORKFLOW_TEST");
        category.setDisplayName("Repair Workflow Test");
        category.setActive(true);
        category.setWorkflowMode(WorkflowMode.DB_CONFIGURED);
        category.setDbWorkflowEnabled(true);
        category.setFixedActionsEnabled(false);

        WorkflowTransition transition = new WorkflowTransition();
        ReflectionTestUtils.setField(transition, "id", 8L);
        transition.setActionKey("START_SIMPLE_WORK");
        transition.setDisplayName("Start Simple Work");
        transition.setFromStatus(TicketStatus.NEW);
        transition.setToStatus(TicketStatus.IN_PROGRESS);
        transition.setFromStatusRecord(customNew);
        transition.setToStatusRecord(customInProgress);
        transition.setActive(true);
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);

        CreateTicketRequest request = new CreateTicketRequest();
        request.setCategoryId(5L);
        request.setCustomerName("QA Customer");
        request.setMobileNumber("9999999999");
        request.setVillageOrArea("QA Area");
        request.setProductType("QA Pump");
        request.setComplaintDescription("QA complaint");

        when(ticketCategoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(categoryFieldConfigRepository.findRenderableFormFieldsByCategoryId(5L)).thenReturn(List.of());
        when(ticketRepository.getNextTicketNumberValue()).thenReturn(42L);
        when(workflowTransitionRepository.findByFromStatusAndActiveTrueOrderBySortOrderAscIdAsc(TicketStatus.NEW))
                .thenReturn(List.of(transition));
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(8L)).thenReturn(true);
        when(workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(8L, 5L)).thenReturn(true);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ReflectionTestUtils.setField(ticket, "id", 99L);
            return ticket;
        });
        when(effectiveStatusResolver.resolve(argThat(ticket -> ticket.getStatusRecord() == customNew)))
                .thenReturn(new ResolvedTicketStatus(
                        TicketStatus.NEW,
                        11L,
                        "CUSTOM_NEW",
                        "CUSTOM_NEW",
                        true,
                        false,
                        TicketStatus.NEW,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        ResolvedTicketStatus.WarningCode.NONE
                ));

        ticketService.createTicket(request, "SUPER_ADMIN_001");

        org.mockito.Mockito.verify(ticketRepository).save(argThat((Ticket ticket) ->
                ticket.getStatus() == TicketStatus.NEW
                        && ticket.getStatusRecord() == customNew
                        && "KE-042".equals(ticket.getTicketNumber())
        ));
    }

    private Ticket inProgressTicket(String pickedByEmployeeId) {
        Ticket ticket = new Ticket();
        ticket.setId(10L);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setPickedByEmployeeId(pickedByEmployeeId);
        return ticket;
    }

    private WorkflowStatus systemStatus(Long id, String statusKey, TicketStatus behaviorBucket, boolean terminal) {
        WorkflowStatus status = new WorkflowStatus();
        ReflectionTestUtils.setField(status, "id", id);
        status.setStatusKey(statusKey);
        status.setDisplayName(statusKey);
        status.setActive(true);
        status.setSystemStatus(true);
        status.setProtectedStatus(true);
        status.setBehaviorBucket(behaviorBucket);
        status.setTerminal(terminal);
        return status;
    }

    private WorkflowStatus customStatus(Long id, String statusKey, TicketStatus behaviorBucket, boolean terminal) {
        WorkflowStatus status = systemStatus(id, statusKey, behaviorBucket, terminal);
        status.setSystemStatus(false);
        status.setProtectedStatus(false);
        return status;
    }
}
