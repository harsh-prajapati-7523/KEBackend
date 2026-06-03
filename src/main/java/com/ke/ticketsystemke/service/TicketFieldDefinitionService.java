package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateTicketFieldDefinitionRequest;
import com.ke.ticketsystemke.dto.TicketFieldDefinitionResponse;
import com.ke.ticketsystemke.entity.DropdownSource;
import com.ke.ticketsystemke.entity.TicketFieldDefinition;
import com.ke.ticketsystemke.entity.TicketFieldType;
import com.ke.ticketsystemke.repository.DropdownSourceRepository;
import com.ke.ticketsystemke.repository.TicketFieldDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TicketFieldDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(TicketFieldDefinitionService.class);

    private final TicketFieldDefinitionRepository ticketFieldDefinitionRepository;
    private final DropdownSourceRepository dropdownSourceRepository;

    public TicketFieldDefinitionService(
            TicketFieldDefinitionRepository ticketFieldDefinitionRepository,
            DropdownSourceRepository dropdownSourceRepository
    ) {
        this.ticketFieldDefinitionRepository = ticketFieldDefinitionRepository;
        this.dropdownSourceRepository = dropdownSourceRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketFieldDefinitionResponse> listFieldDefinitions() {
        return ticketFieldDefinitionRepository.findAllByOrderBySortOrderAscFieldKeyAsc()
                .stream()
                .map(TicketFieldDefinitionResponse::from)
                .toList();
    }

    @Transactional
    public TicketFieldDefinitionResponse createFieldDefinition(
            CreateTicketFieldDefinitionRequest request,
            String actorEmployeeId
    ) {
        String fieldKey = request.getFieldKey().trim();
        if (ticketFieldDefinitionRepository.existsByFieldKey(fieldKey)) {
            log.warn("event=ticket_field_create_denied actorEmployeeId={} fieldKey={} decision=duplicate_field_key",
                    actorEmployeeId, fieldKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Field key already exists");
        }

        TicketFieldDefinition fieldDefinition = new TicketFieldDefinition();
        fieldDefinition.setFieldKey(fieldKey);
        fieldDefinition.setDisplayName(request.getDisplayName().trim());
        fieldDefinition.setFieldType(request.getFieldType());
        fieldDefinition.setActive(request.getActive() == null || request.getActive());
        fieldDefinition.setSystemField(false);
        fieldDefinition.setHelpText(trimToNull(request.getHelpText()));
        fieldDefinition.setDefaultRequired(request.getDefaultRequired() != null && request.getDefaultRequired());
        fieldDefinition.setSortOrder(request.getSortOrder());

        try {
            TicketFieldDefinition created = ticketFieldDefinitionRepository.saveAndFlush(fieldDefinition);
            log.info("event=ticket_field_created actorEmployeeId={} fieldId={} fieldKey={} fieldType={} targetActive={}",
                    actorEmployeeId, created.getId(), created.getFieldKey(), created.getFieldType(), created.isActive());
            return TicketFieldDefinitionResponse.from(created);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=ticket_field_create_denied actorEmployeeId={} fieldKey={} decision=duplicate_field_key",
                    actorEmployeeId, fieldKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Field key already exists");
        }
    }

    @Transactional
    public TicketFieldDefinitionResponse updateStatus(Long id, boolean active, String actorEmployeeId) {
        TicketFieldDefinition fieldDefinition = ticketFieldDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket field definition not found"));

        fieldDefinition.setActive(active);
        TicketFieldDefinition saved = ticketFieldDefinitionRepository.save(fieldDefinition);
        log.info("event={} actorEmployeeId={} fieldId={} fieldKey={} fieldType={} targetActive={}",
                active ? "ticket_field_enabled" : "ticket_field_disabled",
                actorEmployeeId, saved.getId(), saved.getFieldKey(), saved.getFieldType(), saved.isActive());
        return TicketFieldDefinitionResponse.from(saved);
    }

    @Transactional
    public TicketFieldDefinitionResponse updateDropdownSource(
            Long id,
            Long dropdownSourceId,
            String actorEmployeeId
    ) {
        TicketFieldDefinition fieldDefinition = ticketFieldDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket field definition not found"));

        if (fieldDefinition.getFieldType() != TicketFieldType.DROPDOWN) {
            log.warn("event=ticket_field_dropdown_source_denied actorEmployeeId={} fieldId={} fieldKey={} decision=non_dropdown_field",
                    actorEmployeeId, fieldDefinition.getId(), fieldDefinition.getFieldKey());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dropdown source can only be assigned to DROPDOWN fields");
        }

        DropdownSource dropdownSource = null;
        if (dropdownSourceId != null) {
            dropdownSource = dropdownSourceRepository.findById(dropdownSourceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dropdown source not found"));
        }

        fieldDefinition.setDropdownSource(dropdownSource);
        TicketFieldDefinition saved = ticketFieldDefinitionRepository.save(fieldDefinition);
        log.info("event=ticket_field_dropdown_source_updated actorEmployeeId={} fieldId={} fieldKey={} sourceId={}",
                actorEmployeeId, saved.getId(), saved.getFieldKey(), dropdownSourceId);
        return TicketFieldDefinitionResponse.from(saved);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
