package io.allezgo;

import com.markelliot.barista.Server;
import com.markelliot.barista.tracing.Spans;
import io.allezgo.endpoints.SyncPelotonToGarmin;
import io.allezgo.endpoints.SyncPelotonToGarminEndpoints;
import io.allezgo.events.Events;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("Unexpected command line arguments.");
            return;
        }

        Spans.register("honeycomb.io", cs -> Events.span("allezgo", cs));

        Server.builder()
                .disableTls() // our host provides this for us
                .endpoints(new SyncPelotonToGarminEndpoints(new SyncPelotonToGarmin()))
                .allowOrigin("https://allezgo.io")
                .allowOrigin("http://localhost:8080") // for development
                .tracingRate(1.0)
                .start();
    }
}
