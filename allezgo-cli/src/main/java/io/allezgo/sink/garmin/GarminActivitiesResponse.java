package io.allezgo.sink.garmin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

public record GarminActivitiesResponse(@JsonValue List<GarminActivity> activities) {
    @JsonCreator
    public static GarminActivitiesResponse of(List<GarminActivity> activities) {
        return new GarminActivitiesResponse(activities);
    }
}
