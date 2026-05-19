package io.allezgo.adapters.garmin;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GarminGear(
        @JsonProperty("displayName") String name,
        String customMakeModel,
        @JsonProperty("uuid") GarminGearId gearId) {}
