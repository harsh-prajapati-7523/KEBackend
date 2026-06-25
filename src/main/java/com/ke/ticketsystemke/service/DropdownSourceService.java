package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateDropdownOptionRequest;
import com.ke.ticketsystemke.dto.CreateDropdownSourceRequest;
import com.ke.ticketsystemke.dto.DropdownOptionResponse;
import com.ke.ticketsystemke.dto.DropdownSourceResponse;
import com.ke.ticketsystemke.config.CacheNames;
import com.ke.ticketsystemke.entity.DropdownOption;
import com.ke.ticketsystemke.entity.DropdownSource;
import com.ke.ticketsystemke.entity.DropdownSourceType;
import com.ke.ticketsystemke.repository.DropdownOptionRepository;
import com.ke.ticketsystemke.repository.DropdownSourceRepository;
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
public class DropdownSourceService {

    private static final Logger log = LoggerFactory.getLogger(DropdownSourceService.class);

    private final DropdownSourceRepository dropdownSourceRepository;
    private final DropdownOptionRepository dropdownOptionRepository;

    public DropdownSourceService(
            DropdownSourceRepository dropdownSourceRepository,
            DropdownOptionRepository dropdownOptionRepository
    ) {
        this.dropdownSourceRepository = dropdownSourceRepository;
        this.dropdownOptionRepository = dropdownOptionRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.DROPDOWN_SOURCES, key = "'all'")
    public List<DropdownSourceResponse> listSources() {
        return dropdownSourceRepository.findAllByOrderByDisplayNameAscSourceKeyAscIdAsc()
                .stream()
                .map(DropdownSourceResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DROPDOWN_SOURCES, allEntries = true)
    public DropdownSourceResponse createSource(CreateDropdownSourceRequest request, String actorEmployeeId) {
        if (request.getSourceType() != null && request.getSourceType() != DropdownSourceType.MANUAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only MANUAL dropdown sources are supported");
        }

        String sourceKey = request.getSourceKey().trim();
        if (dropdownSourceRepository.existsBySourceKey(sourceKey)) {
            log.warn("event=dropdown_source_create_denied actorEmployeeId={} sourceKey={} decision=duplicate_source_key",
                    actorEmployeeId, sourceKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dropdown source key already exists");
        }

        DropdownSource source = new DropdownSource();
        source.setSourceKey(sourceKey);
        source.setDisplayName(request.getDisplayName().trim());
        source.setSourceType(DropdownSourceType.MANUAL);
        source.setActive(request.getActive() == null || request.getActive());
        source.setSystemSource(false);

        try {
            DropdownSource saved = dropdownSourceRepository.saveAndFlush(source);
            log.info("event=dropdown_source_created actorEmployeeId={} sourceId={} sourceKey={} targetActive={}",
                    actorEmployeeId, saved.getId(), saved.getSourceKey(), saved.isActive());
            return DropdownSourceResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=dropdown_source_create_denied actorEmployeeId={} sourceKey={} decision=duplicate_source_key",
                    actorEmployeeId, sourceKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dropdown source key already exists");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DROPDOWN_SOURCES, allEntries = true)
    public DropdownSourceResponse updateSourceStatus(Long sourceId, boolean active, String actorEmployeeId) {
        DropdownSource source = findSource(sourceId);
        source.setActive(active);
        DropdownSource saved = dropdownSourceRepository.save(source);
        log.info("event={} actorEmployeeId={} sourceId={} sourceKey={} targetActive={}",
                active ? "dropdown_source_enabled" : "dropdown_source_disabled",
                actorEmployeeId, saved.getId(), saved.getSourceKey(), saved.isActive());
        return DropdownSourceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.DROPDOWN_OPTIONS, key = "#sourceId")
    public List<DropdownOptionResponse> listOptions(Long sourceId) {
        validateSourceExists(sourceId);
        return dropdownOptionRepository.findAllBySourceIdOrderBySortOrderAscDisplayValueAscIdAsc(sourceId)
                .stream()
                .map(DropdownOptionResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DROPDOWN_OPTIONS, key = "#sourceId")
    public DropdownOptionResponse createOption(Long sourceId, CreateDropdownOptionRequest request, String actorEmployeeId) {
        DropdownSource source = findSource(sourceId);
        String optionKey = request.getOptionKey().trim();
        if (dropdownOptionRepository.existsBySourceIdAndOptionKey(sourceId, optionKey)) {
            log.warn("event=dropdown_option_create_denied actorEmployeeId={} sourceId={} optionKey={} decision=duplicate_option_key",
                    actorEmployeeId, sourceId, optionKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dropdown option key already exists for this source");
        }

        DropdownOption option = new DropdownOption();
        option.setSource(source);
        option.setOptionKey(optionKey);
        option.setDisplayValue(request.getDisplayValue().trim());
        option.setActive(request.getActive() == null || request.getActive());
        option.setSortOrder(request.getSortOrder());

        try {
            DropdownOption saved = dropdownOptionRepository.saveAndFlush(option);
            log.info("event=dropdown_option_created actorEmployeeId={} sourceId={} optionId={} optionKey={} targetActive={} sortOrder={}",
                    actorEmployeeId, sourceId, saved.getId(), saved.getOptionKey(), saved.isActive(), saved.getSortOrder());
            return DropdownOptionResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=dropdown_option_create_denied actorEmployeeId={} sourceId={} optionKey={} decision=duplicate_option_key",
                    actorEmployeeId, sourceId, optionKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dropdown option key already exists for this source");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DROPDOWN_OPTIONS, key = "#sourceId")
    public DropdownOptionResponse updateOptionStatus(Long sourceId, Long optionId, boolean active, String actorEmployeeId) {
        validateSourceExists(sourceId);
        DropdownOption option = dropdownOptionRepository.findByIdAndSourceId(optionId, sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dropdown option not found"));

        option.setActive(active);
        DropdownOption saved = dropdownOptionRepository.save(option);
        log.info("event={} actorEmployeeId={} sourceId={} optionId={} optionKey={} targetActive={}",
                active ? "dropdown_option_enabled" : "dropdown_option_disabled",
                actorEmployeeId, sourceId, saved.getId(), saved.getOptionKey(), saved.isActive());
        return DropdownOptionResponse.from(saved);
    }

    private void validateSourceExists(Long sourceId) {
        findSource(sourceId);
    }

    private DropdownSource findSource(Long sourceId) {
        return dropdownSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dropdown source not found"));
    }
}
