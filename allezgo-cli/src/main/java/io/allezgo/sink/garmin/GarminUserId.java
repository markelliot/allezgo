package io.allezgo.sink.garmin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record GarminUserId(@JsonValue String value) {
    @JsonCreator
    public static GarminUserId of(long userId) {
        return new GarminUserId(String.valueOf(userId));
    }

    @JsonCreator
    public static GarminUserId of(String userId) {
        return new GarminUserId(userId);
    }

    @Override
    public String toString() {
        return value;
    }
}
