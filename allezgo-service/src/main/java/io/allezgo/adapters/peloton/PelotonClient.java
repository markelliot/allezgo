package io.allezgo.adapters.peloton;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.markelliot.result.Result;
import io.allezgo.client.Endpoint;
import io.allezgo.client.HttpError;
import io.allezgo.client.ObjectHttpClient;
import io.allezgo.config.Configuration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class PelotonClient {
    private static final Endpoint.Base base = Endpoint.of("https://api.onepeloton.com/");
    private static final ObjectHttpClient client = new ObjectHttpClient();

    private Supplier<PelotonSession> session;

    public PelotonClient(Configuration.Peloton conf) {
        this.session =
                Suppliers.memoize(() -> login(conf.email(), conf.password()).orElseThrow(HttpError::toException));
    }

    private Result<PelotonSession, HttpError> login(String email, String password) {
        Result<PelotonLoginResponse, HttpError> response = client.post(
                base.path("auth", "login").build(),
                PelotonLoginRequest.of(email, password),
                PelotonLoginResponse.class);

        return response.mapResult(r -> new PelotonSession(r.sessionId(), r.userId()));
    }

    public boolean validateLogin() {
        // TODO(markelliot): we need to clean up the way the client works, the
        // exceptions-for-control-flow is evidence of bad composition
        try {
            Preconditions.checkNotNull(this.session.get());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Result<PelotonActivities, HttpError> activities(int page, int limit) {
        return client.get(
                base.path("api", "user", session.get().userId(), "workouts")
                        .query("joins", "ride")
                        .query("limit", limit)
                        .query("page", page)
                        .header("cookie", session.get().toCookies())
                        .build(),
                PelotonActivities.class);
    }

    public Stream<PelotonActivity> activitiesAsStream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<>() {
                            private PelotonActivities activities =
                                    activities(0, 100).orElseThrow(HttpError::toException);
                            private Iterator<PelotonActivity> currentPageIter =
                                    activities.data().iterator();
                            private int page = 0;

                            @Override
                            public boolean hasNext() {
                                return currentPageIter.hasNext() || page < activities.pageCount();
                            }

                            @Override
                            public PelotonActivity next() {
                                if (currentPageIter.hasNext()) {
                                    return currentPageIter.next();
                                }
                                page++;
                                activities = activities(page, 100).orElseThrow(HttpError::toException);
                                currentPageIter = activities.data().iterator();
                                return currentPageIter.next();
                            }
                        },
                        Spliterator.IMMUTABLE),
                false);
    }

    public Result<Ride, HttpError> ride(RidePointer ride) {
        return client.get(
                base.path("api", "ride", ride.id().value(), "details")
                        .query("stream_source", "multichannel")
                        .header("cookie", session.get().toCookies())
                        .build(),
                Ride.class);
    }

    public Result<PerformanceSummary, HttpError> metrics(ActivityId activityId) {
        return client.get(
                base.path("api", "workout", activityId.value(), "performance_graph")
                        .query("every_n", 1)
                        .header("cookie", session.get().toCookies())
                        .build(),
                PerformanceSummary.class);
    }
}
