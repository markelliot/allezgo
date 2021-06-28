package io.allezgo.units;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record Watts(@JsonValue double value) {
    @JsonCreator
    public static Watts of(double value) {
        return new Watts(value);
    }

    @JsonCreator
    public static Watts of(int value) {
        return new Watts(value);
    }
}
