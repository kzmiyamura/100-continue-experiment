package com.example.continueexperiment.model;

public class DataResponse {
    private String id;
    private String status;
    private String received;
    private String timestamp;
    private String headerReceivedAt;
    private String bodyReceivedAt;
    private long delayMs;
    private String errorMessage;

    public DataResponse() {}

    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getReceived() { return received; }
    public void setReceived(String v) { this.received = v; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String v) { this.timestamp = v; }
    public String getHeaderReceivedAt() { return headerReceivedAt; }
    public void setHeaderReceivedAt(String v) { this.headerReceivedAt = v; }
    public String getBodyReceivedAt() { return bodyReceivedAt; }
    public void setBodyReceivedAt(String v) { this.bodyReceivedAt = v; }
    public long getDelayMs() { return delayMs; }
    public void setDelayMs(long v) { this.delayMs = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final DataResponse r = new DataResponse();
        public Builder id(String v) { r.id = v; return this; }
        public Builder status(String v) { r.status = v; return this; }
        public Builder received(String v) { r.received = v; return this; }
        public Builder timestamp(String v) { r.timestamp = v; return this; }
        public Builder headerReceivedAt(String v) { r.headerReceivedAt = v; return this; }
        public Builder bodyReceivedAt(String v) { r.bodyReceivedAt = v; return this; }
        public Builder delayMs(long v) { r.delayMs = v; return this; }
        public Builder errorMessage(String v) { r.errorMessage = v; return this; }
        public DataResponse build() { return r; }
    }
}
