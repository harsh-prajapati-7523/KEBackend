package com.ke.ticketsystemke.service;
import com.ke.ticketsystemke.dto.*; import com.ke.ticketsystemke.entity.*; import com.ke.ticketsystemke.repository.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.*;
@Service
public class WorkflowTransitionExecutionService {
 private final TicketRepository tickets; private final TicketWorkflowHistoryService history; private final TicketChargeService charges; private final EffectiveStatusResolver statuses; private final EmployeeRepository employees;
 public WorkflowTransitionExecutionService(TicketRepository tickets,TicketWorkflowHistoryService history,TicketChargeService charges,EffectiveStatusResolver statuses,EmployeeRepository employees){this.tickets=tickets;this.history=history;this.charges=charges;this.statuses=statuses;this.employees=employees;}
 @Transactional(propagation=Propagation.MANDATORY)
 public TicketResponse execute(GenericTransitionExecutorService.GenericTransitionExecutionPlan plan,String actor,GenericTransitionExecutionRequest request){Ticket ticket=plan.ticket();WorkflowTransition transition=plan.transition();TicketStatus from=ticket.getStatus();Long fromId=plan.fromStatus().getId();ticket.setStatus(plan.toStatusBehaviorBucket());ticket.setStatusRecord(plan.toStatus());Ticket saved=tickets.save(ticket);history.recordSuccessfulGenericAction(saved,plan.action(),transition,from,plan.toStatusBehaviorBucket(),fromId,plan.toStatus().getId(),actor,request==null?null:request.getComment(),request==null?null:request.getReason(),plan.systemTransition(),plan.customTransition());return TicketResponse.from(saved,charges.calculateTotalCharge(saved.getId()),statuses.resolve(saved),employeeName(saved.getPickedByEmployeeId()));}
 private String employeeName(String id){if(id==null||id.isBlank())return null;return employees.findByEmployeeIdIgnoreCase(id.trim()).map(Employee::getName).orElse(null);}
}
