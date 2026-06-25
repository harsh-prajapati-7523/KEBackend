package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateTicketCategoryRequest;
import com.ke.ticketsystemke.dto.TicketCategoryResponse;
import com.ke.ticketsystemke.config.CacheNames;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TicketCategoryService {

    private static final Logger log = LoggerFactory.getLogger(TicketCategoryService.class);
    private static final String OTHER_CATEGORY_KEY = "OTHER";

    private final TicketCategoryRepository ticketCategoryRepository;

    public TicketCategoryService(TicketCategoryRepository ticketCategoryRepository) {
        this.ticketCategoryRepository = ticketCategoryRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.TICKET_CATEGORIES, key = "'all'")
    public List<TicketCategoryResponse> listCategories() {
        return ticketCategoryRepository.findAllByOrderBySortOrderAscCategoryKeyAsc()
                .stream()
                .map(TicketCategoryResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.TICKET_CATEGORIES, allEntries = true)
    public TicketCategoryResponse createCategory(CreateTicketCategoryRequest request, String actorEmployeeId) {
        String categoryKey = request.getCategoryKey().trim();
        if (ticketCategoryRepository.existsByCategoryKey(categoryKey)) {
            log.warn("event=ticket_category_create_denied actorEmployeeId={} categoryKey={} decision=duplicate_category_key",
                    actorEmployeeId, categoryKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category key already exists");
        }

        TicketCategoryConfig category = new TicketCategoryConfig();
        category.setCategoryKey(categoryKey);
        category.setDisplayName(request.getDisplayName().trim());
        category.setActive(request.getActive() == null || request.getActive());
        category.setSystemCategory(false);
        category.setSortOrder(request.getSortOrder());

        try {
            TicketCategoryConfig created = ticketCategoryRepository.saveAndFlush(category);
            log.info("event=ticket_category_created actorEmployeeId={} categoryId={} categoryKey={} targetActive={}",
                    actorEmployeeId, created.getId(), created.getCategoryKey(), created.isActive());
            return TicketCategoryResponse.from(created);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=ticket_category_create_denied actorEmployeeId={} categoryKey={} decision=duplicate_category_key",
                    actorEmployeeId, categoryKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category key already exists");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.TICKET_CATEGORIES, allEntries = true)
    public TicketCategoryResponse updateStatus(Long id, boolean active, String actorEmployeeId) {
        TicketCategoryConfig category = ticketCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (!active && OTHER_CATEGORY_KEY.equals(category.getCategoryKey())) {
            log.warn("event=ticket_category_update_denied actorEmployeeId={} categoryId={} categoryKey={} targetActive={} decision=other_protected",
                    actorEmployeeId, category.getId(), category.getCategoryKey(), active);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTHER category cannot be disabled");
        }

        category.setActive(active);
        TicketCategoryConfig saved = ticketCategoryRepository.save(category);
        log.info("event={} actorEmployeeId={} categoryId={} categoryKey={} targetActive={}",
                active ? "ticket_category_enabled" : "ticket_category_disabled",
                actorEmployeeId, saved.getId(), saved.getCategoryKey(), saved.isActive());
        return TicketCategoryResponse.from(saved);
    }
}
