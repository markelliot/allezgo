package io.allezgo.units;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record Seconds(@JsonValue long value) {
    @JsonCreator
    public static Seconds of(long value) {
        return new Seconds(value);
    }
}
