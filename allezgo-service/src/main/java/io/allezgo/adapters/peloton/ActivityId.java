package io.allezgo.adapters.peloton;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ActivityId(@JsonValue String value) {
    @JsonCreator
    public static ActivityId of(String value) {
        return new ActivityId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
