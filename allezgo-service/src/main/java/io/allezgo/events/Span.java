package io.allezgo.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Span(
        @JsonProperty("service_name") String serviceName,
        @JsonProperty("trace.span_id") String spanId,
        @JsonProperty("trace.parent_id") String parentId,
        @JsonProperty("trace.trace_id") String traceId,
        @JsonProperty("name") String opName,
        @JsonProperty("duration_ms") long durationMs) {}
