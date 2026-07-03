package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.UpdateWorkflowStatusRequest;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowStatusServiceTest {

    @Mock
    private WorkflowStatusRepository workflowStatusRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private WorkflowStatusService workflowStatusService;

    @BeforeEach
    void setUp() {
        workflowStatusService = new WorkflowStatusService(
                workflowStatusRepository,
                workflowTransitionRepository,
                employeeRepository
        );
    }

    @Test
    void updateStatusSynchronizesLinkedTransitionBehaviorBuckets() {
        Employee employee = new Employee();
        employee.setEmployeeId("admin-1");
        WorkflowStatus source = status(9L, "CHANGRED", TicketStatus.IN_PROGRESS, false);
        WorkflowStatus target = status(10L, "DELIVERD", TicketStatus.IN_PROGRESS, false);
        WorkflowTransition transition = new WorkflowTransition();
        ReflectionTestUtils.setField(transition, "id", 12L);
        transition.setActionKey("DELIVER");
        transition.setDisplayName("Deliver");
        transition.setFromStatus(TicketStatus.IN_PROGRESS);
        transition.setToStatus(TicketStatus.IN_PROGRESS);
        transition.setFromStatusRecord(source);
        transition.setToStatusRecord(target);
        transition.setActive(true);
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);

        UpdateWorkflowStatusRequest request = new UpdateWorkflowStatusRequest();
        request.setTerminal(true);
        request.setBehaviorBucket(TicketStatus.COMPLETED);

        when(workflowStatusRepository.findById(10L)).thenReturn(Optional.of(target));
        when(employeeRepository.findByEmployeeIdIgnoreCase("admin-1")).thenReturn(Optional.of(employee));
        when(workflowStatusRepository.save(target)).thenReturn(target);
        when(workflowTransitionRepository.findByFromStatusRecord_IdOrToStatusRecord_Id(10L, 10L))
                .thenReturn(List.of(transition));

        workflowStatusService.updateStatus(10L, request, "admin-1");

        assertThat(transition.getToStatus()).isEqualTo(TicketStatus.COMPLETED);
        assertThat(transition.getFromStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(workflowTransitionRepository).saveAll(List.of(transition));
    }

    private WorkflowStatus status(Long id, String statusKey, TicketStatus behaviorBucket, boolean terminal) {
        WorkflowStatus status = new WorkflowStatus();
        ReflectionTestUtils.setField(status, "id", id);
        status.setStatusKey(statusKey);
        status.setDisplayName(statusKey);
        status.setActive(true);
        status.setSystemStatus(false);
        status.setProtectedStatus(false);
        status.setBehaviorBucket(behaviorBucket);
        status.setTerminal(terminal);
        return status;
    }
}
