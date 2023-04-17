package io.allezgo.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PelotonToGarminSyncEvent(
        String type,
        @JsonProperty("peloton-user") String pelotonEmail,
        @JsonProperty("garmin-user") String garminEmail,
        @JsonProperty("num-days") int numDaysToSync)
        implements Event {
    public static PelotonToGarminSyncEvent of(String pelotonEmail, String garminEmail, int numDays) {
        return new PelotonToGarminSyncEvent("ptg", pelotonEmail, garminEmail, numDays);
    }
}
