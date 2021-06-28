package io.allezgo.source.peloton;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.allezgo.units.Watts;
import java.time.Instant;
import java.util.Optional;

public record PelotonActivity(
        ActivityId id,
        @JsonProperty("peloton_id") String pelotonId,
        @JsonProperty("user_id") String userId,
        String name,
        String status,
        long created,
        @JsonProperty("device_type") String deviceType,
        String platform,
        @JsonProperty("created_at") long createdAt,
        @JsonProperty("device_time_created_at") long deviceTimeCreatedAt,
        @JsonProperty("start_time") long startTime,
        @JsonProperty("end_time") long endTime,
        @JsonProperty("fitness_discipline") String fitnessDiscipline,
        @JsonProperty("workout_type") String workoutType,
        @JsonProperty("metrics_type") String metricsType,
        @JsonProperty("has_leaderboard_metrics") boolean hasLeaderboardMetrics,
        @JsonProperty("has_pedaling_metrics") boolean hasPedalingMetrics,
        @JsonProperty("is_total_work_personal_record") boolean isTotalWorkPersonalRecord,
        String timezone,
        Watts totalWork,
        Optional<RidePointer> ride) {

    public static String FITNESS_DISCIPLINE_CYCLING = "cycling";

    public Instant tcxId() {
        return Instant.ofEpochSecond(startTime);
    }
}
