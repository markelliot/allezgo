package barista;

import com.google.common.base.Joiner;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

import java.util.Optional;
import java.util.Set;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;

public final class Handlers {
    private static final HttpString ACCESS_CONTROL_ALLOW_ORIGIN =
            new HttpString("Access-Control-Allow-Origin");
    private static final String ORIGIN_ALL = "*";

    private static final HttpString ACCESS_CONTROL_ALLOW_METHODS =
            new HttpString("Access-Control-Allow-Methods");
    private static final String ALLOWED_METHODS = "GET, PUT, POST, DELETE";

    private static final HttpString ACCESS_CONTROL_MAX_AGE =
            new HttpString("Access-Control-Max-Age");
    private static final String ONE_DAY_IN_SECONDS = "86400";

    private static final HttpString ACCESS_CONTROL_REQUEST_HEADERS =
            new HttpString("Access-Control-Request-Headers");
    private static final HttpString ACCESS_CONTROL_ALLOW_HEADERS =
            new HttpString("Access-Control-Allow-Headers");

    private SerDe serde;
    private Authz authz;

    public Handlers(SerDe serde, Authz authz) {
        this.serde = serde;
        this.authz = authz;
    }

    private static final class DoOffIoThread implements HttpHandler {
        private final HttpHandler delegate;

        DoOffIoThread(HttpHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (exchange.isInIoThread()) {
                exchange.dispatch(this);
                return;
            }
            delegate.handleRequest(exchange);
        }
    }

    /**
     * Returns an {@link HttpHandler} that ensures {@code handler} performs work off of the XNIO
     * threads.
     */
    public static HttpHandler dispatchFromIoThread(HttpHandler handler) {
        return new DoOffIoThread(handler);
    }

    /**
     * Returns an {@link HttpHandler} that returns reasonable CORS allow headers for {@code
     * allowedOrigins}.
     */
    public static HttpHandler cors(HttpHandler handler, Set<String> allowedOrigins) {
        return exchange -> {
            if (allowedOrigins.contains(exchange.getRequestHeaders().getFirst(Headers.ORIGIN))) {
                exchange.getResponseHeaders()
                        .add(ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN_ALL)
                        .add(ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS)
                        .add(ACCESS_CONTROL_MAX_AGE, ONE_DAY_IN_SECONDS);

                if (exchange.getRequestHeaders().contains(ACCESS_CONTROL_REQUEST_HEADERS)) {
                    exchange.getResponseHeaders()
                            .add(
                                    ACCESS_CONTROL_ALLOW_HEADERS,
                                    Joiner.on(',')
                                            .join(
                                                    exchange.getRequestHeaders()
                                                            .get(ACCESS_CONTROL_REQUEST_HEADERS)));
                }
            }
            // swallow the request if it's an OPTIONS request
            if (!exchange.getRequestMethod().equals(Methods.OPTIONS)) {
                handler.handleRequest(exchange);
            }
        };
    }

    public <Request, Response> HttpHandler verifiedAuth(
            Endpoints.VerifiedAuth<Request, Response> endpoint) {
        switch (endpoint.method()) {
            case GET:
                return exchange -> authorizedGet(endpoint, exchange);
            case PUT:
            case POST:
                return exchange ->
                        exchange.getRequestReceiver()
                                .receiveFullString(
                                        (innerExchange, str) ->
                                                authorizedPost(
                                                        endpoint,
                                                        innerExchange,
                                                        new SerDe.ByteRepr(str)));
        }
        return this::illegalMethod;
    }

    public <Request, Response> HttpHandler open(Endpoints.Open<Request, Response> endpoint) {
        switch (endpoint.method()) {
            case GET:
                return exchange -> openGet(endpoint, exchange);
            case PUT:
            case POST:
                return exchange ->
                        exchange.getRequestReceiver()
                                .receiveFullString(
                                        (innerExchange, str) ->
                                                openPost(
                                                        endpoint,
                                                        innerExchange,
                                                        new SerDe.ByteRepr(str)));
        }
        return this::illegalMethod;
    }

    private <Request, Response> void openGet(
            Endpoints.Open<Request, Response> endpoint, HttpServerExchange exchange) {
        try {
            Response responseObj = endpoint.call(null);
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
            exchange.getResponseSender().send(serde.serialize(responseObj).raw());
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Server Error");
        }
    }

    private <Request, Response> void openPost(
            Endpoints.Open<Request, Response> endpoint,
            HttpServerExchange exchange,
            SerDe.ByteRepr body) {
        try {
            Request requestObj = serde.deserialize(body, endpoint.requestClass());
            Response responseObj = endpoint.call(requestObj);
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
            exchange.getResponseSender().send(serde.serialize(responseObj).raw());
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Server Error");
        }
    }

    private <Request, Response> void authorizedGet(
            Endpoints.VerifiedAuth<Request, Response> endpoint, HttpServerExchange exchange) {
        HeaderValues authzHeader = exchange.getRequestHeaders().get(Headers.AUTHORIZATION);
        if (authzHeader.size() != 1) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("Unauthorized: Missing authorization authToken");
            return;
        }

        AuthToken authToken = AuthTokens.fromAuthorizationHeader(authzHeader.getFirst());
        Optional<VerifiedAuthToken> verifiedAuthToken = authz.check(authToken);

        if (verifiedAuthToken.isEmpty()) {
            exchange.setStatusCode(403);
            exchange.getResponseSender()
                    .send("Forbidden: Invalid or expired authorization authToken");
            return;
        }

        try {
            Response responseObj = endpoint.call(verifiedAuthToken.get(), null);
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
            exchange.getResponseSender().send(serde.serialize(responseObj).raw());
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Server Error");
        }
    }

    private <Request, Response> void authorizedPost(
            Endpoints.VerifiedAuth<Request, Response> endpoint,
            HttpServerExchange exchange,
            SerDe.ByteRepr body) {
        HeaderValues authzHeader = exchange.getRequestHeaders().get(Headers.AUTHORIZATION);
        if (authzHeader.size() != 1) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("Unauthorized: Missing authorization authToken");
            return;
        }

        AuthToken authToken = AuthTokens.fromAuthorizationHeader(authzHeader.getFirst());
        Optional<VerifiedAuthToken> verifiedAuthToken = authz.check(authToken);

        if (verifiedAuthToken.isEmpty()) {
            exchange.setStatusCode(403);
            exchange.getResponseSender()
                    .send("Forbidden: Invalid or expired authorization authToken");
            return;
        }

        try {
            Request requestObj = serde.deserialize(body, endpoint.requestClass());
            Response responseObj = endpoint.call(verifiedAuthToken.get(), requestObj);
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, serde.contentType());
            exchange.getResponseSender().send(serde.serialize(responseObj).raw());
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Server Error");
        }
    }

    private void illegalMethod(HttpServerExchange exchange) {
        exchange.setStatusCode(405);
        exchange.getResponseSender().send("Unexpected method");
    }
}
