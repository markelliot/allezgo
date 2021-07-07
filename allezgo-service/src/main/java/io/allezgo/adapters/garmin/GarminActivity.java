package io.allezgo.adapters.garmin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public record GarminActivity(
        GarminActivityId activityId,
        String activityName,
        Optional<String> description,
        long ownerId,
        String startTimeLocal,
        String startTimeGMT) {
    private static DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Instant tcxId() {
        return LocalDateTime.parse(startTimeGMT, parser).toInstant(ZoneOffset.UTC);
    }

    public LocalDate gearDate() {
        return LocalDate.ofInstant(tcxId(), ZoneId.of("UTC"));
    }
}
