package io.allezgo.adapters.tcx;

import com.google.common.collect.ImmutableMap;
import io.allezgo.units.BeatsPerMinute;
import io.allezgo.units.Calories;
import io.allezgo.units.Miles;
import io.allezgo.units.MilesPerHour;
import io.allezgo.units.RevolutionsPerMinute;
import io.allezgo.units.Seconds;
import io.allezgo.units.Watts;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TrainingCenterDatabase {

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static final String formatInstant(Instant time) {
        return formatter.format(OffsetDateTime.ofInstant(time, ZoneOffset.UTC));
    }

    private TrainingCenterDatabase() {}

    public record Lap(
            Instant startTime,
            Seconds totalSeconds,
            Calories calories,
            List<Trackpoint> trackpoints) {}

    public record Trackpoint(
            Instant time,
            BeatsPerMinute heartRate,
            RevolutionsPerMinute cadence,
            MilesPerHour speed,
            Watts power,
            Miles distance) {}

    private static String renderTrackpoint(Trackpoint trackpoint) {
        Map<String, Object> params =
                Map.of(
                        "{time}", formatInstant(trackpoint.time),
                        "{heartRate}", trackpoint.heartRate.value(),
                        "{cadence}", trackpoint.cadence.value(),
                        "{speed}", trackpoint.speed.toMetersPerSecond(),
                        "{power}", (int) trackpoint.power.value(),
                        "{distance}", trackpoint.distance.toMeters());
        return substitute(
                """
                <Trackpoint>
                  <Time>{time}</Time>
                  <DistanceMeters>{distance}</DistanceMeters>
                  <HeartRateBpm>
                    <Value>{heartRate}</Value>
                  </HeartRateBpm>
                  <Cadence>{cadence}</Cadence>
                  <Extensions>
                    <ns3:TPX>
                      <ns3:Speed>{speed}</ns3:Speed>
                      <ns3:Watts>{power}</ns3:Watts>
                    </ns3:TPX>
                  </Extensions>
                </Trackpoint>
                """,
                params);
    }

    public static String renderLaps(Lap lap) {
        double maximumSpeed = 0.0;
        double maximumHeartRateBpm = 0.0;
        double maximumWatts = 0.0;
        int maximumCadence = 0;

        double cadenceSum = 0.0;
        double heartRateSum = 0.0;
        double wattsSum = 0.0;

        for (Trackpoint tp : lap.trackpoints()) {
            if (tp.speed().value() > maximumSpeed) {
                maximumSpeed = tp.speed().value();
            }
            if (tp.heartRate().value() > maximumHeartRateBpm) {
                maximumHeartRateBpm = tp.heartRate().value();
            }
            if (tp.power().value() > maximumWatts) {
                maximumWatts = tp.power().value();
            }
            if (tp.cadence().value() > maximumCadence) {
                maximumCadence = tp.cadence().value();
            }

            // TODO(markelliot): assuming equal spaced ticks
            cadenceSum += tp.cadence().value();
            heartRateSum += tp.heartRate().value();
            wattsSum += tp.power().value();
        }

        double lapDistanceMeters =
                lap.trackpoints().get(lap.trackpoints().size() - 1).distance().toMeters()
                        - lap.trackpoints().get(1).distance().toMeters();

        double averageCadence = cadenceSum / lap.trackpoints().size();
        double averageHeartRateBpm = heartRateSum / lap.trackpoints().size();
        double averageSpeed = lapDistanceMeters / lap.totalSeconds().value();
        double averageWatts = wattsSum / lap.trackpoints().size();

        Map<String, Object> params =
                ImmutableMap.<String, Object>builder()
                        .put("{startTime}", formatInstant(lap.startTime))
                        .put("{totalTimeSeconds}", lap.totalSeconds.value())
                        .put("{distance}", lapDistanceMeters)
                        .put("{maximumSpeed}", maximumSpeed)
                        .put("{calories}", lap.calories().value())
                        .put("{averageHeartRateBpm}", (int) averageHeartRateBpm)
                        .put("{maximumHeartRateBpm}", (int) maximumHeartRateBpm)
                        .put("{maximumWatts}", maximumWatts)
                        .put("{averageWatts}", averageWatts)
                        .put("{averageSpeed}", averageSpeed)
                        .put("{averageCadence}", averageCadence)
                        .put("{maximumCadence}", maximumCadence)
                        .put(
                                "{trackpoints}",
                                lap.trackpoints.stream()
                                        .map(TrainingCenterDatabase::renderTrackpoint)
                                        .collect(Collectors.joining())
                                        .indent(4))
                        .build();
        return substitute(
                """
                <Lap StartTime="{startTime}">
                  <TotalTimeSeconds>{totalTimeSeconds}</TotalTimeSeconds>
                  <DistanceMeters>{distance}</DistanceMeters>
                  <MaximumSpeed>{maximumSpeed}</MaximumSpeed>
                  <Calories>{calories}</Calories>
                  <AverageHeartRateBpm>
                    <Value>{averageHeartRateBpm}</Value>
                  </AverageHeartRateBpm>
                  <MaximumHeartRateBpm>
                    <Value>{maximumHeartRateBpm}</Value>
                  </MaximumHeartRateBpm>
                  <Intensity>Active</Intensity>
                  <Cadence>{averageCadence}</Cadence>
                  <TriggerMethod>Manual</TriggerMethod>
                  <Track>
                {trackpoints}
                  </Track>
                  <Extensions>
                    <ns3:LX>
                      <ns3:AvgSpeed>{averageSpeed}</ns3:AvgSpeed>
                      <ns3:MaxBikeCadence>{maximumCadence}</ns3:MaxBikeCadence>
                      <ns3:AvgWatts>{averageWatts}</ns3:AvgWatts>
                      <ns3:MaxWatts>{maximumWatts}</ns3:MaxWatts>
                    </ns3:LX>
                  </Extensions>
                </Lap>
                """,
                params);
    }

    public static Tcx render(String className, Instant start, List<Lap> laps) {
        Map<String, Object> params =
                Map.of(
                        "{id}",
                        formatInstant(start),
                        "{laps}",
                        laps.stream()
                                .map(TrainingCenterDatabase::renderLaps)
                                .collect(Collectors.joining())
                                .indent(6),
                        "{notes}",
                        className);
        return Tcx.of(
                substitute(
                        """
                    <?xml version='1.0' encoding='UTF-8'?>
                    <TrainingCenterDatabase
                      xsi:schemaLocation="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd"
                      xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
                      xmlns:ns2="http://www.garmin.com/xmlschemas/UserProfile/v2"
                      xmlns:ns3="http://www.garmin.com/xmlschemas/ActivityExtension/v2"
                      xmlns:ns4="http://www.garmin.com/xmlschemas/ProfileExtension/v1"
                      xmlns:ns5="http://www.garmin.com/xmlschemas/ActivityGoals/v1"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                      <Activities>
                        <Activity Sport="Biking">
                          <Id>{id}</Id>
                    {laps}
                          <Notes>{notes}</Notes>
                        </Activity>
                      </Activities>
                    </TrainingCenterDatabase>
                    """,
                        params));
    }

    private static String substitute(String string, Map<String, Object> substitutions) {
        String result = string;
        for (Map.Entry<String, Object> substitution : substitutions.entrySet()) {
            result =
                    result.replaceAll(
                            Pattern.quote(substitution.getKey()),
                            String.valueOf(substitution.getValue()));
        }
        return result;
    }
}
