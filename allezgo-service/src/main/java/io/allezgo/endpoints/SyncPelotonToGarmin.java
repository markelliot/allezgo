package io.allezgo.endpoints;

import barista.Endpoints;
import barista.HttpMethod;
import com.google.common.base.Strings;
import io.allezgo.client.HttpError;
import io.allezgo.config.Configuration;
import io.allezgo.pipes.PelotonToTcx;
import io.allezgo.sink.garmin.GarminActivity;
import io.allezgo.sink.garmin.GarminActivityId;
import io.allezgo.sink.garmin.GarminClient;
import io.allezgo.sink.tcx.Tcx;
import io.allezgo.source.peloton.PelotonActivity;
import io.allezgo.source.peloton.PelotonClient;
import io.allezgo.source.peloton.PerformanceSummary;
import io.allezgo.source.peloton.Ride;
import io.allezgo.source.peloton.RidePointer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class SyncPelotonToGarmin
        implements Endpoints.Open<SyncPelotonToGarmin.Request, SyncPelotonToGarmin.Response> {

    public SyncPelotonToGarmin() {}

    @Override
    public Class<Request> requestClass() {
        return Request.class;
    }

    @Override
    public String template() {
        return "/api/synchronize/peloton-to-garmin";
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.PUT;
    }

    @Override
    public Response call(Request request) {
        List<String> failingArgs = checkArgs(request);
        if (!failingArgs.isEmpty()) {
            return new Response(null, "Some required fields were missing or empty: " + failingArgs);
        }
        if (request.numDaysToSync < 0 || request.numDaysToSync > 30) {
            return new Response(null, "numDaysToSync must be between 1 and 30");
        }

        PelotonClient peloton =
                new PelotonClient(
                        new Configuration.Peloton(request.pelotonEmail, request.pelotonPassword));

        if (!peloton.validateLogin()) {
            return new Response(null, "Unable to login to Peloton with the provided credentials");
        }

        GarminClient garmin =
                new GarminClient(
                        new Configuration.Garmin(
                                request.garminEmail,
                                request.garminPassword,
                                request.garminPelotonGearName));

        if (!garmin.validateLogin()) {
            return new Response(null, "Unable to login to Garmin with the provided credentials");
        }

        return new Response(syncLastNDays(peloton, garmin, request.numDaysToSync), null);
    }

    private static List<String> checkArgs(Request request) {
        List<String> nulls = new ArrayList<>();
        if (Strings.isNullOrEmpty(request.pelotonEmail)) {
            nulls.add("pelotonEmail");
        }
        if (Strings.isNullOrEmpty(request.pelotonPassword)) {
            nulls.add("pelotonPassword");
        }
        if (Strings.isNullOrEmpty(request.garminEmail)) {
            nulls.add("garminEmail");
        }
        if (Strings.isNullOrEmpty(request.garminPassword)) {
            nulls.add("garminPassword");
        }
        if (Strings.isNullOrEmpty(request.garminPelotonGearName)) {
            nulls.add("garminPelotonGearName");
        }
        return nulls;
    }

    public record Request(
            String pelotonEmail,
            String pelotonPassword,
            String garminEmail,
            String garminPassword,
            String garminPelotonGearName,
            int numDaysToSync) {}

    public record SyncRecord(
            LocalDate activityDate,
            String title,
            String description,
            String pelotonLink,
            String garminLink,
            boolean wasCreated) {}

    public record Response(List<SyncRecord> result, String error) {}

    private static List<SyncRecord> syncLastNDays(
            PelotonClient peloton, GarminClient garmin, int numDays) {
        Instant thirtyDaysAgo = Instant.now().minus(Period.ofDays(numDays));

        List<GarminActivity> garminActivitiesLastMonth =
                garmin.activitiesAsStream()
                        .takeWhile(ga -> ga.tcxId().isAfter(thirtyDaysAgo))
                        .toList();

        return peloton.activitiesAsStream()
                .takeWhile(pa -> pa.tcxId().isAfter(thirtyDaysAgo))
                .filter(
                        pelotonActivity ->
                                pelotonActivity
                                        .fitnessDiscipline()
                                        .equals(PelotonActivity.FITNESS_DISCIPLINE_CYCLING))
                .filter(pelotonActivity -> pelotonActivity.ride().isPresent())
                .map(
                        pelotonActivity ->
                                findOrSync(
                                        peloton,
                                        garmin,
                                        pelotonActivity,
                                        pelotonActivity.ride().get(),
                                        garminActivitiesLastMonth.stream()))
                .toList();
    }

    private static SyncRecord findOrSync(
            PelotonClient peloton,
            GarminClient garmin,
            PelotonActivity pelotonRide,
            RidePointer ridePointer,
            Stream<GarminActivity> garminActivities) {
        Optional<GarminActivity> matchedGarminActivity =
                findMatchingGarminActivity(garminActivities, pelotonRide.tcxId());

        GarminActivityId garminActivityId =
                matchedGarminActivity
                        .map(GarminActivity::activityId)
                        .orElseGet(() -> uploadRideToGarmin(peloton, garmin, pelotonRide));

        return new SyncRecord(
                getActivityDate(pelotonRide),
                ridePointer.title(),
                ridePointer.description(),
                "https://members.onepeloton.com/profile/workouts/" + pelotonRide.id(),
                "https://connect.garmin.com/modern/activity/" + garminActivityId,
                matchedGarminActivity.isEmpty());
    }

    private static GarminActivityId uploadRideToGarmin(
            PelotonClient peloton, GarminClient garmin, PelotonActivity lastPelotonRide) {
        Ride rideDetails =
                peloton.ride(lastPelotonRide.ride().get()).orElseThrow(HttpError::toException);
        PerformanceSummary metrics =
                peloton.metrics(lastPelotonRide.id()).orElseThrow(HttpError::toException);
        Tcx tcx = PelotonToTcx.convertToTcx(lastPelotonRide, rideDetails, metrics);

        GarminActivityId garminActivityId =
                garmin.uploadTcx(tcx).orElseThrow(HttpError::toException);

        String title = rideDetails.ride().titleWithInstructor();
        String description = rideDetails.ride().description();
        garmin.updateActivity(garminActivityId, title, description)
                .orElseThrow(HttpError::toException);

        garmin.setNamedGear(
                garminActivityId, getActivityDate(lastPelotonRide), garmin.pelotonGear());

        return garminActivityId;
    }

    private static LocalDate getActivityDate(PelotonActivity lastPelotonRide) {
        // TODO(markelliot): pretty sure Peloton rides are in rider-local time not UTC, so this is
        //   probably wrong in some edge cases
        return LocalDateTime.ofEpochSecond(lastPelotonRide.created(), 0, ZoneOffset.UTC)
                .toLocalDate();
    }

    /**
     * For all Garmin activities in the last month find a ride with exactly the same start or within
     * +/- 2 minutes of the start.
     */
    private static Optional<GarminActivity> findMatchingGarminActivity(
            Stream<GarminActivity> garminActivities, Instant activityStart) {
        return garminActivities
                .takeWhile(ga -> ga.tcxId().isAfter(activityStart.minusSeconds(120L)))
                .filter(
                        ga -> {
                            Instant gId = ga.tcxId();
                            return gId.isAfter(activityStart.minusSeconds(120L))
                                    && gId.isBefore(activityStart.plusSeconds(120L));
                        })
                .findFirst();
    }
}
