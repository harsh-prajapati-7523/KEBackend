package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CancelTicketRequest;
import com.ke.ticketsystemke.dto.CompleteTicketRequest;
import com.ke.ticketsystemke.dto.CreateTicketRequest;
import com.ke.ticketsystemke.dto.TicketResponse;
import com.ke.ticketsystemke.dto.UpdateWarrantyRequest;
import com.ke.ticketsystemke.entity.ManufacturerStatus;
import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WarrantyStatus;
import com.ke.ticketsystemke.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository repository;
    private final TicketChargeService ticketChargeService;

    public TicketService(TicketRepository repository, TicketChargeService ticketChargeService) {
        this.repository = repository;
        this.ticketChargeService = ticketChargeService;
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
        validateWarrantyDetails(
                request.getWarrantyStatus(),
                request.getManufacturerOrBrandName(),
                request.getProductSerialNumber()
        );
        ticket.setWarrantyStatus(request.getWarrantyStatus());
        ticket.setManufacturerStatus(ManufacturerStatus.NOT_REQUIRED);
        ticket.setManufacturerComplaintNumber(trimToNull(request.getManufacturerComplaintNumber()));
        ticket.setManufacturerOrBrandName(trimToNull(request.getManufacturerOrBrandName()));
        ticket.setProductSerialNumber(trimToNull(request.getProductSerialNumber()));
        ticket.setWarrantyUpdatedAt(Instant.now());
        ticket.setWarrantyUpdatedByEmployeeId(createdByEmployeeId);

        Ticket saved = repository.save(ticket);
        return TicketResponse.from(saved, BigDecimal.ZERO.setScale(2));
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
            Ticket saved = repository.save(ticket);
            TicketResponse resp = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
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
            Ticket saved = repository.save(ticket);
            TicketResponse resp = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
            log.info("event=ticket_started ticketId={} previousOwner={} employeeId={} statusTransition=PICKED->IN_PROGRESS", ticketId, previousOwner, employeeId);
            return resp;
        }

        log.warn("event=invalid_status_transition ticketId={} status={} attemptedAction=start-work", ticketId, ticket.getStatus());
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Ticket can only be started from PICKED status"
        );
    }

    @Transactional
    public TicketResponse completeTicket(
            Long ticketId,
            CompleteTicketRequest request,
            String employeeId,
            String role
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            log.warn("event=invalid_status_transition ticketId={} status={} attemptedAction=complete", ticketId, ticket.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket can only be completed from IN_PROGRESS status"
            );
        }

        if (!isAdminRole(role) && !employeeId.equals(ticket.getPickedByEmployeeId())) {
            log.warn("event=completion_denied ticketId={} status={} employeeId={} role={}", ticketId, ticket.getStatus(), employeeId, role);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Not authorized to complete this ticket"
            );
        }

        if (ticket.getWarrantyStatus() == WarrantyStatus.NOT_CHECKED) {
            log.warn("event=warranty_completion_blocked ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                    ticketId, ticket.getTicketNumber(), employeeId, ticket.getWarrantyStatus(), ticket.getManufacturerStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket warranty must be confirmed before completion"
            );
        }

        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(Instant.now());
        ticket.setCompletedByEmployeeId(employeeId);
        ticket.setCompletionRemark(trimToNull(request.getCompletionRemark()));

        Ticket saved = repository.save(ticket);
        TicketResponse resp = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
        log.info("event=ticket_completed ticketId={} ticketNumber={} employeeId={} statusTransition=IN_PROGRESS->COMPLETED", ticketId, ticket.getTicketNumber(), employeeId);
        return resp;
    }

    @Transactional
    public TicketResponse updateWarranty(
            Long ticketId,
            UpdateWarrantyRequest request,
            String employeeId,
            String role
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (!isOperationalRole(role)) {
            log.warn("event=warranty_update_denied ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                    ticketId, ticket.getTicketNumber(), employeeId, ticket.getWarrantyStatus(), ticket.getManufacturerStatus());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update ticket warranty");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            log.warn("event=warranty_update_denied ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                    ticketId, ticket.getTicketNumber(), employeeId, ticket.getWarrantyStatus(), ticket.getManufacturerStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled ticket warranty cannot be updated");
        }

        WarrantyStatus previousWarrantyStatus = ticket.getWarrantyStatus();
        if (!isSuperAdminRole(role)
                && previousWarrantyStatus != WarrantyStatus.NOT_CHECKED
                && previousWarrantyStatus != request.getWarrantyStatus()) {
            log.warn("event=invalid_warranty_status_change ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                    ticketId, ticket.getTicketNumber(), employeeId, previousWarrantyStatus, ticket.getManufacturerStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmed warranty status cannot be changed");
        }

        validateWarrantyDetails(
                request.getWarrantyStatus(),
                request.getManufacturerOrBrandName(),
                request.getProductSerialNumber()
        );

        ticket.setWarrantyStatus(request.getWarrantyStatus());
        ticket.setManufacturerStatus(request.getManufacturerStatus());
        ticket.setManufacturerComplaintNumber(trimToNull(request.getManufacturerComplaintNumber()));
        ticket.setManufacturerOrBrandName(trimToNull(request.getManufacturerOrBrandName()));
        ticket.setProductSerialNumber(trimToNull(request.getProductSerialNumber()));
        ticket.setWarrantyUpdatedAt(Instant.now());
        ticket.setWarrantyUpdatedByEmployeeId(employeeId);

        Ticket saved = repository.save(ticket);
        TicketResponse response = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
        log.info("event=warranty_updated ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                ticketId, saved.getTicketNumber(), employeeId, saved.getWarrantyStatus(), saved.getManufacturerStatus());
        return response;
    }

    @Transactional
    public TicketResponse cancelTicket(
            Long ticketId,
            CancelTicketRequest request,
            String employeeId,
            String role
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getStatus() == TicketStatus.COMPLETED || ticket.getStatus() == TicketStatus.CANCELLED) {
            log.warn("event=invalid_status_transition ticketId={} status={} attemptedAction=cancel", ticketId, ticket.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket cannot be cancelled in its current status"
            );
        }

        if (!isAdminRole(role)) {
            log.warn("event=cancellation_denied ticketId={} status={} employeeId={} role={}", ticketId, ticket.getStatus(), employeeId, role);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Not authorized to cancel this ticket"
            );
        }

        TicketStatus previousStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(Instant.now());
        ticket.setCancelledByEmployeeId(employeeId);
        ticket.setCancellationReason(request.getCancellationReason().trim());

        Ticket saved = repository.save(ticket);
        TicketResponse resp = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
        log.info("event=ticket_cancelled ticketId={} ticketNumber={} employeeId={} statusTransition={}->CANCELLED", ticketId, ticket.getTicketNumber(), employeeId, previousStatus);
        return resp;
    }

    private boolean isAdminRole(String role) {
        return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
    }

    private boolean isSuperAdminRole(String role) {
        return "SUPER_ADMIN".equals(role);
    }

    private boolean isOperationalRole(String role) {
        return isAdminRole(role) || "EMPLOYEE".equals(role) || "TECHNICIAN".equals(role);
    }

    public List<TicketResponse> listTickets() {
        List<TicketResponse> list = repository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(ticket -> TicketResponse.from(ticket, ticketChargeService.calculateTotalCharge(ticket.getId())))
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

    private void validateWarrantyDetails(
            WarrantyStatus warrantyStatus,
            String manufacturerOrBrandName,
            String productSerialNumber
    ) {
        if (warrantyStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warranty status is required");
        }

        if (warrantyStatus == WarrantyStatus.IN_WARRANTY) {
            if (trimToNull(manufacturerOrBrandName) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manufacturer or brand name is required for in-warranty tickets");
            }
            if (trimToNull(productSerialNumber) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product serial number is required for in-warranty tickets");
            }
        }
    }
}
