package io.allezgo.units;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record BeatsPerMinute(@JsonValue int value) {
    @JsonCreator
    public static BeatsPerMinute of(int value) {
        return new BeatsPerMinute(value);
    }
}
