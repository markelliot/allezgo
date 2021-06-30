package io.allezgo.source.peloton;

import io.allezgo.config.Configuration;
import io.allezgo.pipes.PelotonToTcx;
import io.allezgo.sink.tcx.Tcx;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;

final class PelotonClientTests {

    public static final Path TCX_FILE = Path.of(System.getProperty("user.home"), "activity.tcx");

    @Test
    void fetchLastActivityAndWriteTcxFile() throws Exception {
        PelotonClient client = new PelotonClient(Configuration.fromDefaultFile().peloton());
        PelotonActivities activities = client.activities(0, 10).orElseThrow();

        PelotonActivity activity =
                activities.data().stream()
                        .filter(a -> a.ride().isPresent())
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Last 10 activities do not include a ride"));

        RidePointer ridePtr = activity.ride().get();
        Ride ride = client.ride(ridePtr).orElseThrow();
        PerformanceSummary metrics = client.metrics(activity.id()).orElseThrow();

        System.out.println("Found Ride " + activity.tcxId() + ":");
        System.out.print(
                (ride.ride().title() + " with " + ride.ride().instructor().name()).indent(2));
        System.out.print(ride.ride().description().indent(2));
        System.out.println();

        Tcx tcxFile = PelotonToTcx.convertToTcx(activity, ride, metrics);
        Files.writeString(TCX_FILE, tcxFile.value(), StandardOpenOption.TRUNCATE_EXISTING);
        System.out.print("Wrote " + TCX_FILE);
    }
}
