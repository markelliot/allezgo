package io.allezgo;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length != 1) {
            consoleln("Missing command argument.");
            consoleln(
                    """
                    Valid commands:
                      latest   synchronizes up to the last 30 days of Peloton rides to Garmin Connect.
                    """);
            return;
        }
        String command = args[0];

        if (!Configuration.defaultFile().exists()) {
            consoleln(
                    "Unable to find configuration file "
                            + Configuration.defaultFile().getAbsolutePath());
            consoleln("Please create this file and fill in your details, its format is:");
            consoleln(Configuration.renderExample().indent(4));
            return;
        }
        Configuration config = Configuration.fromDefaultFile();

        PelotonClient peloton = new PelotonClient(config.peloton());
        GarminClient garmin = new GarminClient(config.garmin());

        switch (command) {
            case "latest":
                latest(peloton, garmin);
                return;
            default:
                consoleln("Unknown command '" + command + "'");
                break;
        }
    }

    private static void latest(PelotonClient peloton, GarminClient garmin) {
        List<GarminActivity> garminActivitiesLastMonth =
                garmin.activitiesAsStream()
                        .takeWhile(ga -> ga.tcxId().isAfter(Instant.now().minus(Period.ofDays(30))))
                        .toList();

        List<PelotonActivity> pelotonActivitiesLastMonth =
                peloton.activitiesAsStream()
                        .takeWhile(pa -> pa.tcxId().isAfter(Instant.now().minus(Period.ofDays(30))))
                        .filter(
                                pa ->
                                        pa.fitnessDiscipline()
                                                .equals(PelotonActivity.FITNESS_DISCIPLINE_CYCLING))
                        .toList();

        for (PelotonActivity pelotonActivity : pelotonActivitiesLastMonth) {
            findOrSync(peloton, garmin, pelotonActivity, garminActivitiesLastMonth.stream());
            consoleln("");
        }
    }

    private static void findOrSync(
            PelotonClient peloton,
            GarminClient garmin,
            PelotonActivity pelotonRide,
            Stream<GarminActivity> garminActivities) {
        consoleln("Peloton Activity:");
        consoleln(getActivityDate(pelotonRide) + " " + pelotonRide.ride().get().title(), 2);
        consoleln(pelotonRide.ride().get().description(), 2);
        consoleln("https://members.onepeloton.com/profile/workouts/" + pelotonRide.id(), 2);

        Optional<GarminActivity> matchedGarminActivity =
                findMatchingGarminActivity(garminActivities, pelotonRide.tcxId());
        matchedGarminActivity.ifPresentOrElse(
                ga -> {
                    consoleln("Matching Garmin activity:");
                    consoleln(ga.activityName(), 2);
                    consoleln(ga.description().orElse(""), 2);
                    consoleln("https://connect.garmin.com/modern/activity/" + ga.activityId(), 2);

                    // TODO(markelliot): maybe clean up title/description if different than target
                },
                () -> {
                    GarminActivityId garminId = uploadRideToGarmin(peloton, garmin, pelotonRide);

                    consoleln("Created a new Garmin activity:");
                    consoleln(pelotonRide.ride().get().title(), 2);
                    consoleln(pelotonRide.ride().get().description(), 2);
                    consoleln("https://connect.garmin.com/modern/activity/" + garminId, 2);
                });
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
        return LocalDateTime.ofEpochSecond(lastPelotonRide.created(), 0, ZoneOffset.UTC)
                .toLocalDate();
    }

    /**
     * Iterates through Garmin activities until either:
     *
     * <ol>
     *   <li>we find this ride (exact start time or one within +/- 2 minutes of its start)
     *   <li>we see at least one ride older than this one
     * </ol>
     */
    private static Optional<GarminActivity> findMatchingGarminActivity(
            Stream<GarminActivity> garminActivities, Instant activityStart) {
        return garminActivities
                .takeWhile(ga -> ga.tcxId().isAfter(activityStart.minusSeconds(120L)))
                .filter(
                        ga -> {
                            Instant gId = ga.tcxId();
                            if (gId.isAfter(activityStart.minusSeconds(120L))
                                    && gId.isBefore(activityStart.plusSeconds(120L))) {
                                return true;
                            }
                            return false;
                        })
                .findFirst();
    }

    private static void consoleln(String line) {
        System.out.println(line);
    }

    private static void consoleln(String line, int indent) {
        System.out.print(line.indent(indent));
    }
}
