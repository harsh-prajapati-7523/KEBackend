package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.AddCategoryFieldConfigRequest;
import com.ke.ticketsystemke.dto.CategoryFieldConfigResponse;
import com.ke.ticketsystemke.dto.TicketFormFieldResponse;
import com.ke.ticketsystemke.dto.TicketFormFieldOptionResponse;
import com.ke.ticketsystemke.dto.TicketFormFieldsResponse;
import com.ke.ticketsystemke.dto.UpdateCategoryFieldConfigRequest;
import com.ke.ticketsystemke.entity.CategoryFieldConfig;
import com.ke.ticketsystemke.entity.DropdownSource;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketFieldDefinition;
import com.ke.ticketsystemke.entity.TicketFieldType;
import com.ke.ticketsystemke.repository.CategoryFieldConfigRepository;
import com.ke.ticketsystemke.repository.DropdownOptionRepository;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
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
public class CategoryFieldConfigService {

    private static final Logger log = LoggerFactory.getLogger(CategoryFieldConfigService.class);

    private final CategoryFieldConfigRepository categoryFieldConfigRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final TicketFieldDefinitionRepository ticketFieldDefinitionRepository;
    private final DropdownOptionRepository dropdownOptionRepository;

    public CategoryFieldConfigService(
            CategoryFieldConfigRepository categoryFieldConfigRepository,
            TicketCategoryRepository ticketCategoryRepository,
            TicketFieldDefinitionRepository ticketFieldDefinitionRepository,
            DropdownOptionRepository dropdownOptionRepository
    ) {
        this.categoryFieldConfigRepository = categoryFieldConfigRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.ticketFieldDefinitionRepository = ticketFieldDefinitionRepository;
        this.dropdownOptionRepository = dropdownOptionRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryFieldConfigResponse> listConfigs(Long categoryId) {
        validateCategoryExists(categoryId);
        return categoryFieldConfigRepository.findAllByCategoryIdSorted(categoryId)
                .stream()
                .map(CategoryFieldConfigResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketFormFieldsResponse getFormFields(Long categoryId) {
        TicketCategoryConfig category = findCategory(categoryId);
        if (!category.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inactive category cannot be used for ticket creation");
        }

        List<TicketFormFieldResponse> fields = categoryFieldConfigRepository.findRenderableFormFieldsByCategoryId(categoryId)
                .stream()
                .map(this::toFormFieldResponse)
                .toList();

        return new TicketFormFieldsResponse(
                category.getId(),
                category.getCategoryKey(),
                category.getDisplayName(),
                fields
        );
    }

    @Transactional
    public CategoryFieldConfigResponse addConfig(
            Long categoryId,
            AddCategoryFieldConfigRequest request,
            String actorEmployeeId
    ) {
        TicketCategoryConfig category = findCategory(categoryId);
        TicketFieldDefinition fieldDefinition = ticketFieldDefinitionRepository.findById(request.getFieldDefinitionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket field definition not found"));

        if (!fieldDefinition.isActive()) {
            log.warn("event=category_field_config_invalid_request actorEmployeeId={} categoryId={} fieldDefinitionId={} decision=inactive_field",
                    actorEmployeeId, categoryId, fieldDefinition.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inactive field cannot be mapped");
        }

        if (categoryFieldConfigRepository.existsByCategoryIdAndFieldDefinitionId(categoryId, fieldDefinition.getId())) {
            log.warn("event=category_field_config_duplicate_denied actorEmployeeId={} categoryId={} fieldDefinitionId={} decision=duplicate_mapping",
                    actorEmployeeId, categoryId, fieldDefinition.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Field is already mapped to this category");
        }

        CategoryFieldConfig config = new CategoryFieldConfig();
        config.setCategory(category);
        config.setFieldDefinition(fieldDefinition);
        config.setRequired(request.getRequired() == null ? fieldDefinition.isDefaultRequired() : request.getRequired());
        config.setVisible(request.getVisible() == null || request.getVisible());
        config.setSortOrder(request.getSortOrder());

        try {
            CategoryFieldConfig saved = categoryFieldConfigRepository.saveAndFlush(config);
            log.info("event=category_field_config_added actorEmployeeId={} categoryId={} fieldDefinitionId={} configId={} visible={} required={} sortOrder={}",
                    actorEmployeeId, categoryId, fieldDefinition.getId(), saved.getId(), saved.isVisible(), saved.isRequired(), saved.getSortOrder());
            return CategoryFieldConfigResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=category_field_config_duplicate_denied actorEmployeeId={} categoryId={} fieldDefinitionId={} decision=duplicate_mapping",
                    actorEmployeeId, categoryId, fieldDefinition.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Field is already mapped to this category");
        }
    }

    @Transactional
    public CategoryFieldConfigResponse updateConfig(
            Long categoryId,
            Long configId,
            UpdateCategoryFieldConfigRequest request,
            String actorEmployeeId
    ) {
        if (request.getRequired() == null && request.getVisible() == null && request.getSortOrder() == null) {
            log.warn("event=category_field_config_invalid_request actorEmployeeId={} categoryId={} configId={} decision=no_fields",
                    actorEmployeeId, categoryId, configId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one mapping field is required");
        }

        CategoryFieldConfig config = findConfigForCategory(categoryId, configId);
        if (request.getRequired() != null) {
            config.setRequired(request.getRequired());
        }
        if (request.getVisible() != null) {
            config.setVisible(request.getVisible());
        }
        if (request.getSortOrder() != null) {
            config.setSortOrder(request.getSortOrder());
        }

        CategoryFieldConfig saved = categoryFieldConfigRepository.save(config);
        log.info("event=category_field_config_updated actorEmployeeId={} categoryId={} fieldDefinitionId={} configId={} visible={} required={} sortOrder={}",
                actorEmployeeId, categoryId, saved.getFieldDefinition().getId(), saved.getId(), saved.isVisible(), saved.isRequired(), saved.getSortOrder());
        return CategoryFieldConfigResponse.from(saved);
    }

    @Transactional
    public CategoryFieldConfigResponse updateStatus(
            Long categoryId,
            Long configId,
            boolean visible,
            String actorEmployeeId
    ) {
        CategoryFieldConfig config = findConfigForCategory(categoryId, configId);
        config.setVisible(visible);
        CategoryFieldConfig saved = categoryFieldConfigRepository.save(config);
        log.info("event={} actorEmployeeId={} categoryId={} fieldDefinitionId={} configId={} visible={}",
                visible ? "category_field_config_shown" : "category_field_config_hidden",
                actorEmployeeId, categoryId, saved.getFieldDefinition().getId(), saved.getId(), saved.isVisible());
        return CategoryFieldConfigResponse.from(saved);
    }

    private void validateCategoryExists(Long categoryId) {
        findCategory(categoryId);
    }

    private TicketCategoryConfig findCategory(Long categoryId) {
        return ticketCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private CategoryFieldConfig findConfigForCategory(Long categoryId, Long configId) {
        validateCategoryExists(categoryId);
        return categoryFieldConfigRepository.findByIdAndCategoryId(configId, categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category field config not found"));
    }

    private TicketFormFieldResponse toFormFieldResponse(CategoryFieldConfig config) {
        TicketFieldDefinition fieldDefinition = config.getFieldDefinition();
        if (fieldDefinition.getFieldType() != TicketFieldType.DROPDOWN) {
            return TicketFormFieldResponse.from(config);
        }

        DropdownSource dropdownSource = fieldDefinition.getDropdownSource();
        List<TicketFormFieldOptionResponse> options = dropdownSource == null
                ? List.of()
                : dropdownOptionRepository.findAllBySourceIdAndActiveTrueOrderBySortOrderAscDisplayValueAscIdAsc(dropdownSource.getId())
                        .stream()
                        .map(TicketFormFieldOptionResponse::from)
                        .toList();
        return TicketFormFieldResponse.from(config, options);
    }
}
