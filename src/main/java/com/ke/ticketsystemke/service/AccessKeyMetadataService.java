package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.AccessKeyMetadataResponse;
import com.ke.ticketsystemke.dto.CreateAccessKeyMetadataRequest;
import com.ke.ticketsystemke.dto.UpdateAccessKeyMetadataRequest;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class AccessKeyMetadataService {

    private static final Logger log = LoggerFactory.getLogger(AccessKeyMetadataService.class);
    private static final Pattern ACCESS_KEY_PATTERN = Pattern.compile("^[A-Z0-9_]{2,60}$");
    private static final int DISPLAY_NAME_MAX_LENGTH = 80;
    private static final int DESCRIPTION_MAX_LENGTH = 255;
    private static final int CATEGORY_MAX_LENGTH = 80;

    private final AccessKeyMetadataRepository accessKeyMetadataRepository;

    public AccessKeyMetadataService(AccessKeyMetadataRepository accessKeyMetadataRepository) {
        this.accessKeyMetadataRepository = accessKeyMetadataRepository;
    }

    @Transactional(readOnly = true)
    public List<AccessKeyMetadataResponse> listAccessKeys(String employeeId) {
        List<AccessKeyMetadataResponse> keys = accessKeyMetadataRepository.findAllByOrderBySortOrderAscAccessKeyAsc()
                .stream()
                .map(AccessKeyMetadataResponse::from)
                .toList();
        log.info("event=access_key_metadata_returned employeeId={} keyCount={}", employeeId, keys.size());
        return keys;
    }

    @Transactional
    public AccessKeyMetadataResponse createAccessKey(CreateAccessKeyMetadataRequest request, String employeeId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access key metadata request is required");
        }

        String accessKey = validateAccessKey(request.getAccessKey());
        if (isSystemEnumKey(accessKey) || accessKeyMetadataRepository.existsByAccessKey(accessKey)) {
            log.warn("event=access_key_metadata_create_denied employeeId={} accessKey={} result=duplicate", employeeId, accessKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Access key already exists");
        }

        AccessKeyMetadata metadata = new AccessKeyMetadata();
        metadata.setAccessKey(accessKey);
        metadata.setDisplayName(validateRequiredText(request.getDisplayName(), "Display name", DISPLAY_NAME_MAX_LENGTH));
        metadata.setDescription(validateOptionalText(request.getDescription(), "Description", DESCRIPTION_MAX_LENGTH));
        metadata.setCategory(validateRequiredText(request.getCategory(), "Category", CATEGORY_MAX_LENGTH));
        metadata.setActive(request.getActive() == null || request.getActive());
        metadata.setSystemKey(false);
        metadata.setProtectedKey(false);
        metadata.setSortOrder(request.getSortOrder());

        try {
            AccessKeyMetadata saved = accessKeyMetadataRepository.saveAndFlush(metadata);
            log.info("event=access_key_metadata_created employeeId={} accessKeyId={} accessKey={} active={} result=created",
                    employeeId, saved.getId(), saved.getAccessKey(), saved.isActive());
            return AccessKeyMetadataResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=access_key_metadata_create_denied employeeId={} accessKey={} result=duplicate", employeeId, accessKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Access key already exists");
        }
    }

    @Transactional
    public AccessKeyMetadataResponse updateAccessKey(Long id, UpdateAccessKeyMetadataRequest request, String employeeId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access key metadata request is required");
        }

        AccessKeyMetadata metadata = findMetadata(id);
        requireCustomEditable(metadata, employeeId, "update");

        metadata.setDisplayName(validateRequiredText(request.getDisplayName(), "Display name", DISPLAY_NAME_MAX_LENGTH));
        metadata.setDescription(validateOptionalText(request.getDescription(), "Description", DESCRIPTION_MAX_LENGTH));
        metadata.setCategory(validateRequiredText(request.getCategory(), "Category", CATEGORY_MAX_LENGTH));
        metadata.setSortOrder(request.getSortOrder());

        AccessKeyMetadata saved = accessKeyMetadataRepository.save(metadata);
        log.info("event=access_key_metadata_updated employeeId={} accessKeyId={} accessKey={} result=updated",
                employeeId, saved.getId(), saved.getAccessKey());
        return AccessKeyMetadataResponse.from(saved);
    }

    @Transactional
    public AccessKeyMetadataResponse updateAccessKeyState(Long id, Boolean active, String employeeId) {
        if (active == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active state is required");
        }

        AccessKeyMetadata metadata = findMetadata(id);
        requireCustomEditable(metadata, employeeId, active ? "enable" : "disable");

        metadata.setActive(active);
        AccessKeyMetadata saved = accessKeyMetadataRepository.save(metadata);
        log.info("event={} employeeId={} accessKeyId={} accessKey={} active={} result=updated",
                active ? "access_key_metadata_enabled" : "access_key_metadata_disabled",
                employeeId, saved.getId(), saved.getAccessKey(), saved.isActive());
        return AccessKeyMetadataResponse.from(saved);
    }

    private AccessKeyMetadata findMetadata(Long id) {
        return accessKeyMetadataRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Access key metadata not found"));
    }

    private void requireCustomEditable(AccessKeyMetadata metadata, String employeeId, String action) {
        if (metadata.isSystemKey() || metadata.isProtectedKey() || isSystemEnumKey(metadata.getAccessKey())) {
            log.warn("event=access_key_metadata_{}_denied employeeId={} accessKeyId={} accessKey={} result=protected",
                    action, employeeId, metadata.getId(), metadata.getAccessKey());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System or protected access keys cannot be changed");
        }
    }

    private String validateAccessKey(String value) {
        String accessKey = value == null ? "" : value.trim();
        if (!ACCESS_KEY_PATTERN.matcher(accessKey).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access key must contain 2 to 60 uppercase letters, digits, or underscores");
        }
        return accessKey;
    }

    private String validateRequiredText(String value, String fieldName, int maxLength) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        if (text.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must contain at most " + maxLength + " characters");
        }
        return text;
    }

    private String validateOptionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must contain at most " + maxLength + " characters");
        }
        return text.isEmpty() ? null : text;
    }

    private boolean isSystemEnumKey(String accessKey) {
        try {
            AccessKey.valueOf(accessKey);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
