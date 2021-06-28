package io.allezgo.units;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record Calories(@JsonValue int value) {
    @JsonCreator
    public static Calories of(int value) {
        return new Calories(value);
    }
}
