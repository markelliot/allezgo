package io.allezgo.source.peloton;

import com.google.common.base.Suppliers;
import io.allezgo.client.Endpoint;
import io.allezgo.client.HttpError;
import io.allezgo.client.ObjectHttpClient;
import io.allezgo.client.Result;
import io.allezgo.config.Configuration;

import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;

public final class PelotonClient {
    private static final Endpoint base = Endpoint.of(URI.create("https://api.onepeloton.com/"));
    private static final ObjectHttpClient client = new ObjectHttpClient();

    private Supplier<PelotonSession> session;

    public PelotonClient(Configuration.Peloton conf) {
        this.session =
                Suppliers.memoize(
                        () ->
                                login(conf.email(), conf.password())
                                        .orElseThrow(HttpError::toException));
    }

    private Result<PelotonSession, HttpError> login(String email, String password) {
        Result<PelotonLoginResponse, HttpError> response =
                client.post(
                        base.path("auth", "login"),
                        PelotonLoginRequest.of(email, password),
                        PelotonLoginResponse.class);

        return response.mapResult(r -> new PelotonSession(r.sessionId(), r.userId()));
    }

    public Result<PelotonActivities, HttpError> activities(int page, int limit) {
        return client.get(
                base.path("api", "user", session.get().userId(), "workouts")
                        .query("joins", "ride")
                        .query("limit", limit)
                        .query("page", page),
                PelotonActivities.class,
                "cookie",
                session.get().toCookies());
    }

    public Result<Ride, HttpError> ride(RidePointer ride) {
        return client.get(
                base.path("api", "ride", ride.id().value(), "details")
                        .query("stream_source", "multichannel"),
                Ride.class,
                "cookie",
                session.get().toCookies());
    }

    public Result<PerformanceSummary, HttpError> metrics(ActivityId activityId) {
        return client.get(
                base.path("api", "workout", activityId.value(), "performance_graph")
                        .query("every_n", 1),
                PerformanceSummary.class,
                "cookie",
                session.get().toCookies());
    }
}
