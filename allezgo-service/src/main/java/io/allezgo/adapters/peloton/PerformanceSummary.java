package io.allezgo.adapters.peloton;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.allezgo.units.Seconds;
import java.util.List;

public record PerformanceSummary(
        @JsonProperty("average_summaries") List<Average> averages,
        Seconds duration,
        @JsonProperty("seconds_since_pedaling_start") List<Integer> ticks,
        @JsonProperty("metrics") List<Metric> metrics,
        @JsonProperty("segment_list") List<Ride.Segment> segments,
        @JsonProperty("summaries") List<Summary> summaries) {

    public record Average(
            @JsonProperty("display_name") String name, @JsonProperty("display_unit") String unit, int value) {}

    public record Metric(
            @JsonProperty("display_name") String name,
            @JsonProperty("display_unit") String unit,
            @JsonProperty("average_value") double average,
            @JsonProperty("max_value") double max,
            List<Double> values) {}

    public record Summary(
            @JsonProperty("display_name") String name, @JsonProperty("display_unit") String unit, int value) {}
}
