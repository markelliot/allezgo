package io.allezgo.sink.garmin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

public record GarminGearResponse(@JsonValue List<GarminGear> gear) {
    @JsonCreator
    public static GarminGearResponse of(List<GarminGear> gear) {
        return new GarminGearResponse(gear);
    }
}
