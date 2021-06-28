package io.allezgo.units;

import com.fasterxml.jackson.annotation.JsonCreator;

public record Miles(@JsonCreator double value) {
    @JsonCreator
    public static Miles of(double value) {
        return new Miles(value);
    }

    public double toMeters() {
        return value * 1609.34;
    }
}
