package io.allezgo.pipes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.allezgo.sink.tcx.Tcx;
import io.allezgo.sink.tcx.TrainingCenterDatabase;
import io.allezgo.source.peloton.PelotonActivity;
import io.allezgo.source.peloton.PerformanceSummary;
import io.allezgo.source.peloton.Ride;
import io.allezgo.units.BeatsPerMinute;
import io.allezgo.units.Calories;
import io.allezgo.units.Miles;
import io.allezgo.units.MilesPerHour;
import io.allezgo.units.RevolutionsPerMinute;
import io.allezgo.units.Seconds;
import io.allezgo.units.Watts;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PelotonToTcx {
    private PelotonToTcx() {}

    public static Tcx convertToTcx(
            PelotonActivity activity, Ride ride, PerformanceSummary metrics) {
        ImmutableMap<String, PerformanceSummary.Metric> metricMap =
                Maps.uniqueIndex(metrics.metrics(), PerformanceSummary.Metric::name);
        ImmutableMap<String, PerformanceSummary.Summary> summaryMap =
                Maps.uniqueIndex(metrics.summaries(), PerformanceSummary.Summary::name);

        Instant start = Instant.ofEpochSecond(activity.startTime());

        List<TrainingCenterDatabase.Lap> laps = new ArrayList<>(metrics.segments().size());

        List<Ride.Segment> segmentsByStartTime =
                metrics.segments().stream()
                        .sorted(Comparator.comparing(s -> s.startTime().value()))
                        .toList();

        double cumulativeDistance = 0.0;
        for (Ride.Segment segment : segmentsByStartTime) {
            List<TrainingCenterDatabase.Trackpoint> trackpoints =
                    new ArrayList<>(metrics.ticks().size());

            // TODO(markelliot): this assumes ticks are seconds, which isn't quite right
            int startTick = (int) segment.startTime().value();
            int endTick =
                    Math.min(
                            startTick + (int) segment.length().value() - 1,
                            metrics.ticks().size() - 1);

            if (startTick == 0) {
                // fudge a 0th point because Peloton counts from a tick value of 1s but Garmin
                // won't count moving time until it sees at least the first tick
                // (we use non-zero HR/Cadence/Speed/Power because picking 0 for these things might
                //  be a litte weird)
                trackpoints.add(
                        new TrainingCenterDatabase.Trackpoint(
                                start.plusSeconds(0),
                                BeatsPerMinute.of(
                                        metricMap.get("Heart Rate").values().get(0).intValue()),
                                RevolutionsPerMinute.of(
                                        metricMap.get("Cadence").values().get(0).intValue()),
                                MilesPerHour.of(metricMap.get("Speed").values().get(0)),
                                Watts.of(metricMap.get("Output").values().get(0)),
                                Miles.of(0.0)));
            }

            for (int i = startTick; i <= endTick; i++) {
                double seconds =
                        metrics.ticks().get(i) - ((i > 0) ? metrics.ticks().get(i - 1) : 0);
                cumulativeDistance += metricMap.get("Speed").values().get(i) * seconds / 3600.0;

                trackpoints.add(
                        new TrainingCenterDatabase.Trackpoint(
                                start.plusSeconds(metrics.ticks().get(i)),
                                BeatsPerMinute.of(
                                        metricMap.get("Heart Rate").values().get(i).intValue()),
                                RevolutionsPerMinute.of(
                                        metricMap.get("Cadence").values().get(i).intValue()),
                                MilesPerHour.of(metricMap.get("Speed").values().get(i)),
                                Watts.of(metricMap.get("Output").values().get(i)),
                                Miles.of(cumulativeDistance)));
            }

            Instant lapStart = start.plusSeconds(metrics.ticks().get(startTick));
            Seconds duration =
                    Seconds.of(
                            (long) metrics.ticks().get(endTick)
                                    - metrics.ticks().get(startTick)
                                    + 1);

            // TODO(markelliot): assume linear calorie assignment, which is pretty bogus -- we
            // should work out how Peloton converts effort to calories (HR?) and then do that
            // calculation ourselves
            Calories caloriePortion =
                    Calories.of(
                            (int)
                                    (summaryMap.get("Calories").value()
                                            * (double) duration.value()
                                            / (double) ride.ride().duration().value()));

            TrainingCenterDatabase.Lap lap =
                    new TrainingCenterDatabase.Lap(lapStart, duration, caloriePortion, trackpoints);

            laps.add(lap);
        }

        return TrainingCenterDatabase.render(
                ride.ride().title()
                        + " with "
                        + ride.ride().instructor().name()
                        + "\n"
                        + ride.ride().description(),
                start,
                laps);
    }
}
