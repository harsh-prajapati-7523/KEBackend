package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.ChargeItemRequest;
import com.ke.ticketsystemke.dto.ChargeItemResponse;
import com.ke.ticketsystemke.dto.ChargeListResponse;
import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketChargeItem;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.repository.TicketChargeItemRepository;
import com.ke.ticketsystemke.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketChargeService {

    private static final Logger log = LoggerFactory.getLogger(TicketChargeService.class);

    private final TicketRepository ticketRepository;
    private final TicketChargeItemRepository chargeRepository;

    public TicketChargeService(
            TicketRepository ticketRepository,
            TicketChargeItemRepository chargeRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.chargeRepository = chargeRepository;
    }

    @Transactional(readOnly = true)
    public ChargeListResponse listCharges(Long ticketId, String employeeId) {
        Ticket ticket = fetchTicket(ticketId);

        List<ChargeItemResponse> chargeItems = chargeRepository.findAllByTicketAndDeletedAtIsNullOrderByCreatedAtAsc(ticket)
                .stream()
                .map(ChargeItemResponse::from)
                .collect(Collectors.toList());

        BigDecimal totalCharge = calculateTotalCharge(ticket);
        log.info("event=charge_list_requested ticketId={} ticketNumber={} employeeId={}", ticketId, ticket.getTicketNumber(), employeeId);
        log.info("event=charge_total_calculated ticketId={} ticketNumber={} totalCharge={}", ticketId, ticket.getTicketNumber(), totalCharge);

        return new ChargeListResponse(chargeItems, totalCharge);
    }

    @Transactional
    public ChargeListResponse addCharge(Long ticketId, ChargeItemRequest request, String employeeId, String role) {
        Ticket ticket = fetchTicket(ticketId);
        validateChargeAddAllowed(ticket, employeeId, role);
        validateChargeRequest(request);

        TicketChargeItem item = new TicketChargeItem();
        item.setTicket(ticket);
        item.setDescription(request.getDescription().trim());
        item.setAmount(request.getAmount().setScale(2, RoundingMode.UNNECESSARY));
        item.setCreatedByEmployeeId(employeeId);

        TicketChargeItem created = chargeRepository.save(item);

        BigDecimal totalCharge = calculateTotalCharge(ticket);
        log.info("event=charge_item_added ticketId={} ticketNumber={} chargeItemId={} amount={} employeeId={} status={}",
                ticketId,
                ticket.getTicketNumber(),
                created.getId(),
                created.getAmount(),
                employeeId,
                ticket.getStatus());
        log.info("event=charge_total_calculated ticketId={} ticketNumber={} totalCharge={}", ticketId, ticket.getTicketNumber(), totalCharge);

        return buildChargeListResponse(ticket);
    }

    @Transactional
    public ChargeListResponse deleteCharge(Long ticketId, Long chargeItemId, String employeeId, String role) {
        Ticket ticket = fetchTicket(ticketId);
        validateChargeDeleteAllowed(ticket, role);

        TicketChargeItem item = chargeRepository.findByIdAndTicketAndDeletedAtIsNull(chargeItemId, ticket)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charge item not found"));

        item.setDeletedAt(Instant.now());
        item.setDeletedByEmployeeId(employeeId);
        chargeRepository.save(item);

        BigDecimal totalCharge = calculateTotalCharge(ticket);
        log.info("event=charge_item_deleted ticketId={} ticketNumber={} chargeItemId={} amount={} employeeId={} status={}",
                ticketId,
                ticket.getTicketNumber(),
                item.getId(),
                item.getAmount(),
                employeeId,
                ticket.getStatus());
        log.info("event=charge_total_calculated ticketId={} ticketNumber={} totalCharge={}", ticketId, ticket.getTicketNumber(), totalCharge);

        return buildChargeListResponse(ticket);
    }

    public BigDecimal calculateTotalCharge(Long ticketId) {
        Ticket ticket = fetchTicket(ticketId);
        return calculateTotalCharge(ticket);
    }

    BigDecimal calculateTotalCharge(Ticket ticket) {
        BigDecimal total = chargeRepository.sumAmountByTicket(ticket);
        return total != null ? total.setScale(2, RoundingMode.UNNECESSARY) : BigDecimal.ZERO.setScale(2);
    }

    private ChargeListResponse buildChargeListResponse(Ticket ticket) {
        List<ChargeItemResponse> chargeItems = chargeRepository.findAllByTicketAndDeletedAtIsNullOrderByCreatedAtAsc(ticket)
                .stream()
                .map(ChargeItemResponse::from)
                .collect(Collectors.toList());
        return new ChargeListResponse(chargeItems, calculateTotalCharge(ticket));
    }

    private Ticket fetchTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private void validateChargeAddAllowed(Ticket ticket, String employeeId, String role) {
        TicketStatus status = ticket.getStatus();
        if (status == TicketStatus.NEW || status == TicketStatus.CANCELLED) {
            log.warn("event=charge_action_denied ticketId={} ticketNumber={} employeeId={} role={} status={}",
                    ticket.getId(), ticket.getTicketNumber(), employeeId, role, status);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Charges cannot be added to ticket in its current status");
        }

        if (status == TicketStatus.COMPLETED && !isAdminRole(role)) {
            log.warn("event=charge_action_denied ticketId={} ticketNumber={} employeeId={} role={} status={}",
                    ticket.getId(), ticket.getTicketNumber(), employeeId, role, status);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to add charges to completed tickets");
        }

        if ((status == TicketStatus.PICKED || status == TicketStatus.IN_PROGRESS)
                && !isAdminRole(role)
                && !isTicketOwner(ticket, employeeId)) {
            log.warn("event=charge_action_denied ticketId={} ticketNumber={} employeeId={} role={} status={}",
                    ticket.getId(), ticket.getTicketNumber(), employeeId, role, status);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to add charges to this ticket");
        }
    }

    private void validateChargeDeleteAllowed(Ticket ticket, String role) {
        TicketStatus status = ticket.getStatus();

        if (!isAdminRole(role)) {
            log.warn("event=charge_action_denied ticketId={} ticketNumber={} role={} status={}",
                    ticket.getId(), ticket.getTicketNumber(), role, status);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete charge items");
        }

        if (status == TicketStatus.NEW || status == TicketStatus.CANCELLED) {
            log.warn("event=charge_action_denied ticketId={} ticketNumber={} role={} status={}",
                    ticket.getId(), ticket.getTicketNumber(), role, status);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Charge items cannot be deleted in the current ticket status");
        }
    }

    private void validateChargeRequest(ChargeItemRequest request) {
        if (request.getAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is required");
        }

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
        }

        if (request.getAmount().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must have at most 2 decimal places");
        }

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }

        BigDecimal maxAmount = new BigDecimal("999999.99");
        if (amount.compareTo(maxAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be 999999.99 or less");
        }
    }

    private boolean isAdminRole(String role) {
        return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
    }

    private boolean isTicketOwner(Ticket ticket, String employeeId) {
        return employeeId != null && employeeId.equals(ticket.getPickedByEmployeeId());
    }
}
