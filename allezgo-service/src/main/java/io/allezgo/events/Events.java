package io.allezgo.events;

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

    record EventResponse() {}
}
