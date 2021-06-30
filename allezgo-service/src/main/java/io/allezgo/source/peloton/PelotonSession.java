package io.allezgo.source.peloton;

public record PelotonSession(String sessionId, String userId) {
    public String toCookies() {
        return "peloton_session_id=" + sessionId;
    }
}
