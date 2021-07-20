package io.allezgo;

import barista.Server;
import barista.authz.Authz;
import io.allezgo.endpoints.SyncPelotonToGarmin;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("Unexpected command line arguments.");
            return;
        }

        Authz authz = Authz.denyAll();
        Server.builder()
                .disableTls() // our host provides this for us
                .authz(authz)
                .endpoint(new SyncPelotonToGarmin())
                .allowOrigin("https://allezgo.io")
                .allowOrigin("http://localhost:8080") // for development
                .start();
    }
}
