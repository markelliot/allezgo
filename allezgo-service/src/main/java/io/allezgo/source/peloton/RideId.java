package io.allezgo.source.peloton;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record RideId(@JsonValue String value) {
    @JsonCreator
    public static RideId of(String id) {
        return new RideId(id);
    }
}
