package io.allezgo.adapters.peloton;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.allezgo.units.Seconds;
import java.util.List;

public record RidePointer(
        RideId id,
        String title,
        String description,
        Seconds duration,
        @JsonProperty("pedaling_start_offset") Seconds pedalingStartOffset,
        @JsonProperty("pedaling_end_offset") Seconds pedalingEndOffset,
        @JsonProperty("pedaling_duration") Seconds pedalingDuration,
        List<String> metrics) {}
