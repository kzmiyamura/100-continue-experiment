package com.example.continueexperiment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    public enum Status {
        WAITING, COMPLETED, ERROR, TIMEOUT
    }

    private String id;
    private String remoteAddr;
    private String method;
    private String contentType;
    private Map<String, String> headers;
    private String body;
    private Status status;
    private Instant headerReceivedAt;
    private Instant bodyReceivedAt;
    private String errorMessage;
}
