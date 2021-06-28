package io.allezgo.units;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record KiloJoules(@JsonValue int value) {
    @JsonCreator
    public static KiloJoules of(int value) {
        return new KiloJoules(value);
    }
}
