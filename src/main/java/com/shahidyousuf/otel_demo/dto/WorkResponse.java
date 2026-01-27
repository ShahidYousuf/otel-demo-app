package com.shahidyousuf.otel_demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WorkResponse(
        String status,
        @JsonProperty("requested_ms") long requestedMs,
        @JsonProperty("actual_ms") long actualMs,
        @JsonProperty("request_id") String requestId
) {}
