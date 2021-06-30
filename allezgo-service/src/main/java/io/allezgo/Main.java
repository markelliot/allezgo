package io.allezgo;

import barista.Authz;
import barista.Server;
import io.allezgo.endpoints.SyncPelotonToGarmin;
import java.time.Clock;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("Unexpected command line arguments.");
            return;
        }

        Authz authz = new Authz("secret", Clock.systemUTC());
        Server.builder()
                .disableTls() // our host provides this for us
                .authz(authz)
                .endpoint(new SyncPelotonToGarmin())
                .start();
    }
}
