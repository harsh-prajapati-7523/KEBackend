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

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TicketResponse createTicket(
            CreateTicketRequest request,
            String createdByEmployeeId
    ) {
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
            ticket.setStatus(TicketStatus.PICKED);
            ticket.setPickedByEmployeeId(employeeId);
            return TicketResponse.from(repository.save(ticket));
        }

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
            ticket.setPickedByEmployeeId(employeeId);
            return TicketResponse.from(repository.save(ticket));
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ticket can only be started from PICKED status"
        );
    }

    public List<TicketResponse> listTickets() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(TicketResponse::from)
                .toList();
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
