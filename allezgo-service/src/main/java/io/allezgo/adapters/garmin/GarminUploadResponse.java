package io.allezgo.adapters.garmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GarminUploadResponse(DetailedImportResult detailedImportResult) {
    public record DetailedImportResult(String uploadId, List<Success> successes) {}

    public record Success(@JsonProperty("internalId") GarminActivityId activityId) {}
}
