package io.allezgo.source.peloton;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PelotonLoginRequest(
        @JsonProperty("username_or_email") String email,
        @JsonProperty("password") String password,
        @JsonProperty("with_pubsub") boolean withPubSub) {
    static PelotonLoginRequest of(String email, String password) {
        return new PelotonLoginRequest(email, password, false);
    }
}
