package io.allezgo.adapters.peloton;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record PelotonActivities(
        int total,
        int count,
        int limit,
        int page,
        @JsonProperty("page_count") int pageCount,
        Map<String, String> summary,
        List<PelotonActivity> data) {}
