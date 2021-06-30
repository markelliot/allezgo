package barista;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import java.util.Optional;

public final class Handlers {
    private SerDe serde;
    private Authz authz;

    public Handlers(SerDe serde, Authz authz) {
        this.serde = serde;
        this.authz = authz;
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
        return exchange -> illegalMethod(exchange);
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
        return exchange -> illegalMethod(exchange);
    }

    private <Request, Response> void openGet(
            Endpoints.Open<Request, Response> endpoint, HttpServerExchange exchange) {
        try {
            Response responseObj = endpoint.call(null);
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
