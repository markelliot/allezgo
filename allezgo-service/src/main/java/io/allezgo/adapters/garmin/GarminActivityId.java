package io.allezgo.adapters.garmin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record GarminActivityId(@JsonValue String value) {
    @JsonCreator
    public static GarminActivityId of(String value) {
        return new GarminActivityId(value);
    }

    @JsonCreator
    public static GarminActivityId of(long value) {
        return new GarminActivityId(String.valueOf(value));
    }

    public long asLong() {
        return Long.parseLong(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
