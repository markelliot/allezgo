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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length != 1) {
            consoleln("Missing command argument.");
            consoleln(
                    """
                    Valid commands:
                      latest   safely synchronizes the latest ride from Peloton to Garmin.
                               This command is safe in the sense that this operation will only sync the
                               ride if it has not already been synchronized.
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
        PelotonActivity lastPelotonRide =
                peloton.activities(0, 20).orElseThrow(HttpError::toException).data().stream()
                        .filter(
                                pa ->
                                        pa.fitnessDiscipline()
                                                .equals(PelotonActivity.FITNESS_DISCIPLINE_CYCLING))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Unable to find a ride in the last 20 Peloton activities"));

        consoleln("Found latest Peloton activity:");
        consoleln(lastPelotonRide.ride().get().title(), 2);
        consoleln(lastPelotonRide.ride().get().description(), 2);
        consoleln("https://members.onepeloton.com/profile/workouts/" + lastPelotonRide.id(), 2);

        Optional<GarminActivity> matchedGarminActivity =
                findMatchingGarminActivity(garmin, lastPelotonRide.tcxId());

        matchedGarminActivity.ifPresentOrElse(
                ga -> {
                    consoleln("Found matching Garmin activity:");
                    consoleln(ga.activityName(), 2);
                    consoleln(ga.description().orElse(""), 2);
                    consoleln("https://connect.garmin.com/modern/activity/" + ga.activityId(), 2);

                    // TODO(markelliot): maybe clean up title/description if different than target
                },
                () -> uploadRideToGarmin(peloton, garmin, lastPelotonRide));
    }

    private static void uploadRideToGarmin(
            PelotonClient peloton, GarminClient garmin, PelotonActivity lastPelotonRide) {
        Ride rideDetails =
                peloton.ride(lastPelotonRide.ride().get()).orElseThrow(HttpError::toException);
        PerformanceSummary metrics =
                peloton.metrics(lastPelotonRide.id()).orElseThrow(HttpError::toException);
        Tcx tcx = PelotonToTcx.convertToTcx(lastPelotonRide, rideDetails, metrics);

        GarminActivityId garminId = garmin.uploadTcx(tcx).orElseThrow(HttpError::toException);

        String title =
                rideDetails.ride().title() + " with " + rideDetails.ride().instructor().name();
        String description = rideDetails.ride().description();
        garmin.updateActivity(garminId, title, description).orElseThrow(HttpError::toException);

        garmin.setNamedGear(garminId, getActivityDate(lastPelotonRide), garmin.pelotonGear());

        consoleln("Created a new Garmin activity:");
        consoleln(title, 2);
        consoleln(description, 2);
        consoleln("https://connect.garmin.com/modern/activity/" + garminId, 2);
    }

    private static LocalDate getActivityDate(PelotonActivity lastPelotonRide) {
        return LocalDateTime.ofEpochSecond(lastPelotonRide.created(), 0, ZoneOffset.UTC)
                .toLocalDate();
    }

    /**
     * Iterates through Garmin activities until either:
     *
     * <ol>
     *   <li>we find this ride (or one within +/- 2 minutes of its start
     *   <li>we see at least one ride older than this one
     * </ol>
     */
    public static Optional<GarminActivity> findMatchingGarminActivity(
            GarminClient garmin, Instant activityStart) {
        int start = 0;
        while (true) {
            List<GarminActivity> activities =
                    garmin.activities(start, 10).orElseThrow(HttpError::toException);
            if (activities.isEmpty()) {
                // reached end of activities
                return Optional.empty();
            }
            for (GarminActivity ga : activities) {
                Instant gId = ga.tcxId();
                if (gId.isAfter(activityStart.minusSeconds(120L))
                        && gId.isBefore(activityStart.plusSeconds(120L))) {
                    return Optional.of(ga);
                }

                // reached an older activity, no need to continue looking
                if (gId.isBefore(activityStart)) {
                    return Optional.empty();
                }
            }
            start += 10;
        }
    }

    private static void consoleln(String line) {
        System.out.println(line);
    }

    private static void consoleln(String line, int indent) {
        System.out.print(line.indent(indent));
    }
}
