package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateTicketRequest;
import com.ke.ticketsystemke.dto.TicketResponse;
import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TicketResponse createTicket(
            CreateTicketRequest request,
            String createdByEmployeeId
    ) {
        // Business-level create log
        log.info("event=ticket_create_requested employeeId={}", createdByEmployeeId);

        Ticket ticket = new Ticket();
        ticket.setTicketNumber(formatTicketNumber(repository.getNextTicketNumberValue()));
        ticket.setCustomerName(request.getCustomerName().trim());
        ticket.setMobileNumber(request.getMobileNumber());
        ticket.setVillageOrArea(trimToNull(request.getVillageOrArea()));
        ticket.setProductType(request.getProductType().trim());
        ticket.setCategory(request.getCategory());
        ticket.setComplaintDescription(request.getComplaintDescription().trim());
        ticket.setStatus(TicketStatus.NEW);
        ticket.setCreatedByEmployeeId(createdByEmployeeId);

        return TicketResponse.from(repository.save(ticket));
    }

    @Transactional
    public TicketResponse pickTicket(
            Long ticketId,
            String employeeId
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        TicketStatus status = ticket.getStatus();
        if (status == TicketStatus.NEW || status == TicketStatus.PICKED) {
            String previousOwner = ticket.getPickedByEmployeeId();
            ticket.setStatus(TicketStatus.PICKED);
            ticket.setPickedByEmployeeId(employeeId);
            TicketResponse resp = TicketResponse.from(repository.save(ticket));
            log.info("event=ticket_picked ticketId={} previousOwner={} newOwner={}", ticketId, previousOwner, employeeId);
            return resp;
        }

        log.warn("event=invalid_status_transition ticketId={} status={} attemptedAction=pick", ticketId, status);
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Ticket cannot be picked in its current status"
        );
    }

    @Transactional
    public TicketResponse startWork(
            Long ticketId,
            String employeeId
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getStatus() == TicketStatus.PICKED) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            String previousOwner = ticket.getPickedByEmployeeId();
            ticket.setPickedByEmployeeId(employeeId);
            TicketResponse resp = TicketResponse.from(repository.save(ticket));
            log.info("event=ticket_started ticketId={} previousOwner={} employeeId={} statusTransition=PICKED->IN_PROGRESS", ticketId, previousOwner, employeeId);
            return resp;
        }

        log.warn("event=invalid_status_transition ticketId={} status={} attemptedAction=start-work", ticketId, ticket.getStatus());
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Ticket can only be started from PICKED status"
        );
    }

    public List<TicketResponse> listTickets() {
        List<TicketResponse> list = repository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(TicketResponse::from)
            .toList();
        log.info("event=ticket_list_returned count={}", list.size());
        return list;
    }

    private String formatTicketNumber(Long sequenceValue) {
        return String.format("KE-%03d", sequenceValue);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
