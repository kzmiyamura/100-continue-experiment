package com.example.continueexperiment.service;

import com.example.continueexperiment.model.LogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class ExperimentService {

    private final Deque<LogEntry> logEntries = new ConcurrentLinkedDeque<>();
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private volatile int bodyDelaySeconds;

    public ExperimentService(@Value("${experiment.body-delay-seconds:10}") int bodyDelaySeconds) {
        this.bodyDelaySeconds = bodyDelaySeconds;
    }

    public int getBodyDelaySeconds() {
        return bodyDelaySeconds;
    }

    public void setBodyDelaySeconds(int seconds) {
        this.bodyDelaySeconds = seconds;
        log.info("Updated body delay to {} seconds", seconds);
    }

    public LogEntry createEntry(String remoteAddr, String method, String contentType, Map<String, String> headers) {
        LogEntry entry = LogEntry.builder()
                .id(UUID.randomUUID().toString())
                .remoteAddr(remoteAddr)
                .method(method)
                .contentType(contentType)
                .headers(headers)
                .status(LogEntry.Status.WAITING)
                .headerReceivedAt(Instant.now())
                .build();

        addLog(entry);
        broadcastEvent("header_received", entry);
        return entry;
    }

    public void updateEntry(LogEntry entry) {
        // The entry is already in the deque by reference; just broadcast update
        broadcastEvent("update", entry);
    }

    public void addLog(LogEntry entry) {
        logEntries.addFirst(entry);
        // Keep only last 50
        while (logEntries.size() > 50) {
            logEntries.removeLast();
        }
    }

    public List<LogEntry> getLogs() {
        List<LogEntry> list = new ArrayList<>(logEntries);
        return list;
    }

    public SseEmitter createSseEmitter() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        sseEmitters.add(emitter);

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError(e -> sseEmitters.remove(emitter));

        // Send initial connected event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"SSE connected\"}"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event", e);
        }

        return emitter;
    }

    private void broadcastEvent(String eventName, LogEntry entry) {
        if (sseEmitters.isEmpty()) return;

        String data;
        try {
            data = objectMapper.writeValueAsString(entry);
        } catch (Exception e) {
            log.error("Failed to serialize log entry", e);
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        sseEmitters.removeAll(dead);
    }
}
