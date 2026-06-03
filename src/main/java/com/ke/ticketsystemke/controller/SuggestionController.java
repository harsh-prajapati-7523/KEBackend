package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.service.AccessService;
import com.ke.ticketsystemke.service.SuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Function;

@RestController
@RequestMapping("/volt/suggestions")
public class SuggestionController {

    private static final Logger log = LoggerFactory.getLogger(SuggestionController.class);

    private final SuggestionService suggestionService;
    private final AccessService accessService;

    public SuggestionController(SuggestionService suggestionService, AccessService accessService) {
        this.suggestionService = suggestionService;
        this.accessService = accessService;
    }

    @GetMapping("/product-types")
    public List<String> getProductTypes(
            @RequestParam(required = false) String query,
            Authentication authentication
    ) {
        return getSuggestions("productType", query, authentication, suggestionService::getProductTypes);
    }

    @GetMapping("/villages")
    public List<String> getVillages(
            @RequestParam(required = false) String query,
            Authentication authentication
    ) {
        return getSuggestions("villageOrArea", query, authentication, suggestionService::getVillages);
    }

    @GetMapping("/manufacturers")
    public List<String> getManufacturers(
            @RequestParam(required = false) String query,
            Authentication authentication
    ) {
        return getSuggestions("manufacturerOrBrandName", query, authentication, suggestionService::getManufacturers);
    }

    @GetMapping("/charge-descriptions")
    public List<String> getChargeDescriptions(
            @RequestParam(required = false) String query,
            Authentication authentication
    ) {
        return getSuggestions("chargeDescription", query, authentication, suggestionService::getChargeDescriptions);
    }

    private List<String> getSuggestions(
            String suggestionType,
            String query,
            Authentication authentication,
            Function<String, List<String>> suggestionFinder
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.USE_SMART_SUGGESTIONS);
        log.info("event=suggestion_requested suggestionType={} employeeId={}", suggestionType, employeeId);

        List<String> suggestions = suggestionFinder.apply(query);

        log.info("event=suggestion_returned suggestionType={} resultCount={} employeeId={}",
                suggestionType, suggestions.size(), employeeId);
        return suggestions;
    }
}
