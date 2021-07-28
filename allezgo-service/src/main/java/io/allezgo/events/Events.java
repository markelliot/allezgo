package io.allezgo.events;

import barista.tracing.CompletedSpan;
import io.allezgo.client.Endpoint;
import io.allezgo.client.ObjectHttpClient;

public final class Events {

    private static final String BEELINE_API_KEY = System.getenv("BEELINE_API_KEY");
    private static final ObjectHttpClient client = new ObjectHttpClient();
    private static final Endpoint.Base events = Endpoint.of("https://api.honeycomb.io/1/events/");

    public static void record(Event event) {
        client.post(
                events.path(event.type()).header("X-Honeycomb-Team", BEELINE_API_KEY).build(),
                event,
                EventResponse.class);
    }

    public static void span(String serviceName, CompletedSpan span) {
        client.post(
                events.path("tracing")
                        .header("X-Honeycomb-Team", BEELINE_API_KEY)
                        .header("X-Honeycomb-Event-Time", span.start().toString())
                        .build(),
                new Span(
                        serviceName,
                        span.spanId(),
                        span.parentId().orElse(null),
                        span.traceId(),
                        span.opName(),
                        span.duration().toMillis()),
                EventResponse.class);
    }

    record EventResponse() {}
}
