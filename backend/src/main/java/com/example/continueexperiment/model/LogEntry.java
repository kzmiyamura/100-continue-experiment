package com.example.continueexperiment.model;

import java.time.Instant;
import java.util.Map;

public class LogEntry {

    public enum Status { WAITING, COMPLETED, ERROR, TIMEOUT }

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

    public LogEntry() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRemoteAddr() { return remoteAddr; }
    public void setRemoteAddr(String v) { this.remoteAddr = v; }
    public String getMethod() { return method; }
    public void setMethod(String v) { this.method = v; }
    public String getContentType() { return contentType; }
    public void setContentType(String v) { this.contentType = v; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> v) { this.headers = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }
    public Instant getHeaderReceivedAt() { return headerReceivedAt; }
    public void setHeaderReceivedAt(Instant v) { this.headerReceivedAt = v; }
    public Instant getBodyReceivedAt() { return bodyReceivedAt; }
    public void setBodyReceivedAt(Instant v) { this.bodyReceivedAt = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final LogEntry e = new LogEntry();
        public Builder id(String v) { e.id = v; return this; }
        public Builder remoteAddr(String v) { e.remoteAddr = v; return this; }
        public Builder method(String v) { e.method = v; return this; }
        public Builder contentType(String v) { e.contentType = v; return this; }
        public Builder headers(Map<String, String> v) { e.headers = v; return this; }
        public Builder body(String v) { e.body = v; return this; }
        public Builder status(Status v) { e.status = v; return this; }
        public Builder headerReceivedAt(Instant v) { e.headerReceivedAt = v; return this; }
        public Builder bodyReceivedAt(Instant v) { e.bodyReceivedAt = v; return this; }
        public Builder errorMessage(String v) { e.errorMessage = v; return this; }
        public LogEntry build() { return e; }
    }
}
