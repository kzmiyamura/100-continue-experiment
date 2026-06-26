package com.example.continueexperiment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataResponse {
    private String id;
    private String status;
    private String received;
    private String timestamp;
    private String headerReceivedAt;
    private String bodyReceivedAt;
    private long delayMs;
    private String errorMessage;
}
