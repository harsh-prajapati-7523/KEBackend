package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CustomerLookupResponse;
import com.ke.ticketsystemke.entity.Ticket;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceCustomerLookupTest {

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

    @Mock
    private TicketSuggestionService ticketSuggestionService;

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
                workflowStatusRepository,
                workflowTransitionRepository,
                workflowTransitionCategoryRuleRepository,
                effectiveStatusResolver,
                genericTransitionExecutorService,
                ticketWorkflowHistoryService,
                repairWorkflowFeatureFlag,
                ticketSuggestionService
        );
    }

    @Test
    void lookupCustomerByMobileNumberReturnsOnlyCustomerLookupFields() {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("KE-123");
        ticket.setCustomerName("Ramesh Kumar");
        ticket.setMobileNumber("9999999999");
        ticket.setVillageOrArea("Sahjanwa");
        ticket.setProductType("Battery");
        ticket.setComplaintDescription("Do not expose this");

        when(ticketRepository.findFirstByMobileNumberOrderByCreatedAtDesc("9999999999"))
                .thenReturn(Optional.of(ticket));

        Optional<CustomerLookupResponse> response = ticketService.lookupCustomerByMobileNumber("9999999999");

        assertThat(response).isPresent();
        assertThat(response.get().customerName()).isEqualTo("Ramesh Kumar");
        assertThat(response.get().villageOrArea()).isEqualTo("Sahjanwa");
        assertThat(response.get().sourceTicketNumber()).isEqualTo("KE-123");
    }

    @Test
    void lookupCustomerByMobileNumberReturnsEmptyForNoMatch() {
        when(ticketRepository.findFirstByMobileNumberOrderByCreatedAtDesc("9999999999"))
                .thenReturn(Optional.empty());

        assertThat(ticketService.lookupCustomerByMobileNumber("9999999999")).isEmpty();
    }

    @Test
    void lookupCustomerByMobileNumberRejectsInvalidMobileNumber() {
        assertThatThrownBy(() -> ticketService.lookupCustomerByMobileNumber("12345"))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo("mobileNumber must contain exactly 10 digits");
                });
    }
}
