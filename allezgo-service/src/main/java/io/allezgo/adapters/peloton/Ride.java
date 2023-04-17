package io.allezgo.adapters.peloton;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.allezgo.units.Calories;
import io.allezgo.units.KiloJoules;
import io.allezgo.units.Miles;
import io.allezgo.units.Seconds;
import io.allezgo.units.Watts;
import java.util.List;

public record Ride(RideDescription ride, Averages averages, Segments segments) {
    public record Averages(
            @JsonProperty("average_total_work") KiloJoules totalWork,
            @JsonProperty("average_distance") Miles distance,
            @JsonProperty("average_calories") Calories calories,
            @JsonProperty("average_avg_power") Watts averagePower,
            @JsonProperty("average_avg_speed") double averageSpeed,
            @JsonProperty("average_avg_cadence") int averageCadence,
            @JsonProperty("average_avg_resistence") int averageResistence) {}

    public record RideDescription(
            RideId id,
            String title,
            String description,
            Seconds duration,
            @JsonProperty("pedaling_start_offset") Seconds pedalingStartOffset,
            @JsonProperty("pedaling_end_offset") Seconds pedalingEndOffset,
            @JsonProperty("pedaling_duration") Seconds pedalingDuration,
            List<String> metrics,
            Instructor instructor) {

        public String titleWithInstructor() {
            return title + " with " + instructor.name();
        }
    }

    public record Instructor(String name) {}

    public record Segments(@JsonProperty("segment_list") List<Segment> segments) {}

    public record Segment(String name, @JsonProperty("start_time_offset") Seconds startTime, Seconds length) {}
}
