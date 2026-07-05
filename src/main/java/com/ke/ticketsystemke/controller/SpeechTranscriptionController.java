package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.service.AccessService;
import com.ke.ticketsystemke.speech.SpeechTranscriptionFailureReason;
import com.ke.ticketsystemke.speech.SpeechTranscriptionResponse;
import com.ke.ticketsystemke.speech.SpeechTranscriptionService;
import com.ke.ticketsystemke.speech.SpeechTranscriptionUnavailableException;
import com.ke.ticketsystemke.speech.SpeechUnavailableResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/volt/speech")
public class SpeechTranscriptionController {

    private final SpeechTranscriptionService speechTranscriptionService;
    private final AccessService accessService;

    public SpeechTranscriptionController(
            SpeechTranscriptionService speechTranscriptionService,
            AccessService accessService
    ) {
        this.speechTranscriptionService = speechTranscriptionService;
        this.accessService = accessService;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribe(
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(name = "durationSeconds") Integer durationSeconds,
            Authentication authentication
    ) {
        accessService.requireAllowed(authentication.getName(), AccessKey.CREATE_TICKET);
        try {
            SpeechTranscriptionResponse response = speechTranscriptionService.transcribe(audio, durationSeconds);
            return ResponseEntity.ok(response);
        } catch (SpeechTranscriptionUnavailableException ex) {
            HttpStatus status = ex.getReason() == SpeechTranscriptionFailureReason.LIMIT_REACHED
                    ? HttpStatus.TOO_MANY_REQUESTS
                    : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status)
                    .body(new SpeechUnavailableResponse(ex.getMessage(), true));
        }
    }
}
