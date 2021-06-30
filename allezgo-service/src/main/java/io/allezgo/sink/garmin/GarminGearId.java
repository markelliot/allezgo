package io.allezgo.sink.garmin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record GarminGearId(@JsonValue String value) {
    @JsonCreator
    public static GarminGearId of(String value) {
        return new GarminGearId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
