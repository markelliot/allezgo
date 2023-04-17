package io.allezgo.adapters.garmin;

public record GarminSession(String sessionId, String garminSsoGuid, String loadBalancer) {
    public String toCookies() {
        return "__cflb=" + loadBalancer + "; SESSIONID=" + sessionId + "; GARMIN-SSO-GUID=" + garminSsoGuid;
    }
}
