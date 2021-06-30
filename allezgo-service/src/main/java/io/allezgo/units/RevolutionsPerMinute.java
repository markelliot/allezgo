package io.allezgo.units;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record RevolutionsPerMinute(@JsonValue int value) {
    @JsonCreator
    public static RevolutionsPerMinute of(int value) {
        return new RevolutionsPerMinute(value);
    }
}
