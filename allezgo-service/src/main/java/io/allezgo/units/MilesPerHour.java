package io.allezgo.units;

import com.fasterxml.jackson.annotation.JsonCreator;

public record MilesPerHour(@JsonCreator double value) {
    @JsonCreator
    public static MilesPerHour of(double value) {
        return new MilesPerHour(value);
    }

    public double toMetersPerSecond() {
        return value * 0.44704;
    }
}
