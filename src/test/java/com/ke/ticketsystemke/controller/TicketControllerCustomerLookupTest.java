package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CustomerLookupResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.service.AccessService;
import com.ke.ticketsystemke.service.EmployeeService;
import com.ke.ticketsystemke.service.GenericTransitionExecutorService;
import com.ke.ticketsystemke.service.TicketService;
import com.ke.ticketsystemke.service.TicketWorkflowHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerCustomerLookupTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private AccessService accessService;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private TicketWorkflowHistoryService ticketWorkflowHistoryService;

    @Mock
    private GenericTransitionExecutorService genericTransitionExecutorService;

    private TicketController ticketController;

    @BeforeEach
    void setUp() {
        ticketController = new TicketController(
                ticketService,
                accessService,
                employeeService,
                ticketWorkflowHistoryService,
                genericTransitionExecutorService
        );
    }

    @Test
    void lookupCustomerRequiresCreateTicketAccessAndReturnsLookupResponse() {
        CustomerLookupResponse lookupResponse = new CustomerLookupResponse("Ramesh Kumar", "Sahjanwa", "KE-123");
        when(ticketService.lookupCustomerByMobileNumber("9999999999")).thenReturn(Optional.of(lookupResponse));

        ResponseEntity<CustomerLookupResponse> response = ticketController.lookupCustomer(
                "9999999999",
                new TestingAuthenticationToken("EMP-1", "password")
        );

        verify(accessService).requireAllowed("EMP-1", AccessKey.CREATE_TICKET);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(lookupResponse);
    }

    @Test
    void lookupCustomerReturnsNoContentForNoMatch() {
        when(ticketService.lookupCustomerByMobileNumber("9999999999")).thenReturn(Optional.empty());

        ResponseEntity<CustomerLookupResponse> response = ticketController.lookupCustomer(
                "9999999999",
                new TestingAuthenticationToken("EMP-1", "password")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void lookupCustomerDoesNotCallServiceWhenCreateTicketAccessIsMissing() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"))
                .when(accessService)
                .requireAllowed("EMP-1", AccessKey.CREATE_TICKET);

        assertThatThrownBy(() -> ticketController.lookupCustomer(
                "9999999999",
                new TestingAuthenticationToken("EMP-1", "password")
        ))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getReason()).isEqualTo("Access denied");
                });

        verifyNoInteractions(ticketService);
    }
}
