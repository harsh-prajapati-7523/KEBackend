package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.TicketAvailableActionsResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.repository.CategoryFieldConfigRepository;
import com.ke.ticketsystemke.repository.DropdownOptionRepository;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.TicketDynamicValueRepository;
import com.ke.ticketsystemke.repository.TicketRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceAvailableActionsTest {

    @Mock
    private TicketRepository ticketRepository;

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
                ticketCategoryRepository,
                categoryFieldConfigRepository,
                dropdownOptionRepository,
                ticketDynamicValueRepository,
                ticketChargeService,
                workflowService,
                accessService,
                workflowStatusRepository,
                workflowTransitionRepository,
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
    void inProgressOwnerCanSeeCompleteAction() {
        Ticket ticket = inProgressTicket("tech-1");
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(workflowService.isTransitionAllowed(AccessKey.COMPLETE_TICKET, TicketStatus.IN_PROGRESS, TicketStatus.COMPLETED))
                .thenReturn(true);

        TicketAvailableActionsResponse response = ticketService.getAvailableActions(10L, "tech-1", "TECHNICIAN");

        assertThat(response.actions().get(AccessKey.COMPLETE_TICKET).available()).isTrue();
    }

    @Test
    void inProgressAdminCanSeeCancelActionWhenTransitionIsAllowed() {
        Ticket ticket = inProgressTicket("tech-1");
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(workflowService.isTransitionAllowed(AccessKey.CANCEL_TICKET, TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED))
                .thenReturn(true);

        TicketAvailableActionsResponse response = ticketService.getAvailableActions(10L, "admin-1", "ADMIN");

        assertThat(response.actions().get(AccessKey.CANCEL_TICKET).available()).isTrue();
    }

    private Ticket inProgressTicket(String pickedByEmployeeId) {
        Ticket ticket = new Ticket();
        ticket.setId(10L);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setPickedByEmployeeId(pickedByEmployeeId);
        return ticket;
    }
}
