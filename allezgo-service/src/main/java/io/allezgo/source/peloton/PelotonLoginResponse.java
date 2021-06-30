package io.allezgo.source.peloton;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PelotonLoginResponse(
        @JsonProperty("session_id") String sessionId, @JsonProperty("user_id") String userId) {}
