package barista;

import barista.tls.TransportLayerSecurity;
import com.google.common.base.Preconditions;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Server {
    private final Undertow undertow;

    private Server(Undertow undertow) {
        this.undertow = undertow;

        Runtime.getRuntime().addShutdownHook(new Thread(undertow::stop));
    }

    public void join() {
        undertow.start();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int port = 8443;
        private Set<Endpoints.Open<?, ?>> openEndpoints = new LinkedHashSet<>();
        private Set<Endpoints.VerifiedAuth<?, ?>> authEndpoints = new LinkedHashSet<>();
        private SerDe serde = new SerDe.ObjectMapperSerDe();
        private Authz authz = null;

        private Builder() {}

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public <Request, Response> Builder endpoint(Endpoints.Open<Request, Response> endpoint) {
            openEndpoints.add(endpoint);
            return this;
        }

        public <Request, Response> Builder endpoint(
                Endpoints.VerifiedAuth<Request, Response> endpoint) {
            authEndpoints.add(endpoint);
            return this;
        }

        public Builder serde(SerDe serde) {
            this.serde = serde;
            return this;
        }

        public Builder authz(Authz authz) {
            this.authz = authz;
            return this;
        }

        public Server build() {
            Preconditions.checkNotNull(authz);

            Handlers handlers = new Handlers(serde, authz);

            RoutingHandler router = new RoutingHandler();
            openEndpoints.forEach(
                    e -> router.add(e.method().method(), e.template(), handlers.open(e)));
            authEndpoints.forEach(
                    e -> router.add(e.method().method(), e.template(), handlers.verifiedAuth(e)));
            router.setFallbackHandler(
                    exchange ->
                            exchange.setStatusCode(404)
                                    .getResponseSender()
                                    .send("Unknown API Endpoint"));

            return new Server(
                    Undertow.builder()
                            .addHttpsListener(
                                    port,
                                    "0.0.0.0",
                                    TransportLayerSecurity.createSslContext(
                                            Paths.get("var", "security")))
                            .setHandler(router)
                            .build());
        }
    }
}
