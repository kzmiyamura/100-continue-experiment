package com.example.continueexperiment.controller;

import com.example.continueexperiment.model.DataResponse;
import com.example.continueexperiment.model.LogEntry;
import com.example.continueexperiment.service.ExperimentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class DataController {

    private static final Logger log = LoggerFactory.getLogger(DataController.class);
    private final ExperimentService experimentService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public DataController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping("/data")
    public ResponseEntity<?> receiveData(HttpServletRequest request) {
        // Collect headers
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }

        log.info("[{}] Headers received from {} - Content-Type: {}, Expect: {}",
                Instant.now(),
                request.getRemoteAddr(),
                request.getContentType(),
                request.getHeader("Expect"));

        // Create log entry - status WAITING
        LogEntry entry = experimentService.createEntry(
                request.getRemoteAddr(),
                request.getMethod(),
                request.getContentType(),
                headers
        );

        int delaySeconds = experimentService.getBodyDelaySeconds();
        long delayMs = delaySeconds * 1000L;

        log.info("[{}] Waiting {} seconds before reading body (entry id={})",
                Instant.now(), delaySeconds, entry.getId());

        // Delay before reading body - this is the experiment window
        // For requests with Expect: 100-continue, Tomcat sends 100 Continue
        // when getInputStream() is first called (or when body read begins)
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[{}] Starting to read body (entry id={})", Instant.now(), entry.getId());

        // Now read the body with a timeout
        String bodyText = null;
        try {
            InputStream is = request.getInputStream();
            byte[] bodyBytes = is.readAllBytes();
            bodyText = new String(bodyBytes, StandardCharsets.UTF_8);
            log.info("[{}] Body received ({} bytes): {}", Instant.now(), bodyBytes.length, bodyText);
        } catch (IOException e) {
            log.error("[{}] IOException reading body (entry id={}): {}", Instant.now(), entry.getId(), e.getMessage());

            entry.setStatus(LogEntry.Status.ERROR);
            entry.setErrorMessage("Body read failed: " + e.getMessage());
            experimentService.updateEntry(entry);

            DataResponse errResp = DataResponse.builder()
                    .id(entry.getId())
                    .status("error")
                    .errorMessage("Connection dropped or body not received: " + e.getMessage())
                    .timestamp(Instant.now().toString())
                    .headerReceivedAt(entry.getHeaderReceivedAt().toString())
                    .delayMs(delayMs)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errResp);
        }

        Instant bodyReceivedAt = Instant.now();

        // Parse message from body
        String message = bodyText;
        try {
            JsonNode json = objectMapper.readTree(bodyText);
            if (json.has("message")) {
                message = json.get("message").asText();
            }
        } catch (Exception e) {
            log.warn("Could not parse body as JSON, using raw text");
        }

        // Update entry
        entry.setBody(message);
        entry.setStatus(LogEntry.Status.COMPLETED);
        entry.setBodyReceivedAt(bodyReceivedAt);
        experimentService.updateEntry(entry);

        DataResponse response = DataResponse.builder()
                .id(entry.getId())
                .status("ok")
                .received(message)
                .timestamp(Instant.now().toString())
                .headerReceivedAt(entry.getHeaderReceivedAt().toString())
                .bodyReceivedAt(bodyReceivedAt.toString())
                .delayMs(delayMs)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogEntry>> getLogs() {
        return ResponseEntity.ok(experimentService.getLogs());
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return experimentService.createSseEmitter();
    }

    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> setConfig(@RequestBody Map<String, Object> body) {
        Object delayVal = body.get("delaySeconds");
        if (delayVal == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "delaySeconds is required"));
        }
        int delay = Integer.parseInt(delayVal.toString());
        if (delay < 0 || delay > 300) {
            return ResponseEntity.badRequest().body(Map.of("error", "delaySeconds must be between 0 and 300"));
        }
        experimentService.setBodyDelaySeconds(delay);
        return ResponseEntity.ok(Map.of("delaySeconds", delay, "message", "Config updated"));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of("delaySeconds", experimentService.getBodyDelaySeconds()));
    }
}
