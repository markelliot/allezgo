package io.allezgo.sink.garmin;

import io.allezgo.config.Configuration;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GarminClientTests {
    @Test
    public void fetchLast10GarminActivitiesAndPrintTitleAndDescription() throws Exception {
        Configuration conf = Configuration.fromDefaultFile();
        GarminClient garmin = new GarminClient(conf.garmin());

        garmin.activities(0, 10)
                .orElseThrow()
                .forEach(
                        a -> {
                            System.out.println(a.tcxId() + ": " + a.activityName());
                            System.out.println(a.description().orElse("").indent(2));
                        });
    }

    @Test
    public void fetchLatestGarminActivityAndSetGearToPeloton() throws Exception {
        Configuration conf = Configuration.fromDefaultFile();
        GarminClient garmin = new GarminClient(conf.garmin());

        GarminActivity activity = garmin.activities(0, 10).orElseThrow().get(0);

        System.out.println(activity.tcxId() + ": " + activity.activityName());
        System.out.println(activity.description().orElse("").indent(2));

        List<GarminGear> availableGear =
                garmin.availableGear(activity.gearDate(), garmin.userId().orElseThrow())
                        .orElseThrow();
        for (GarminGear gear : availableGear) {
            if (gear.customMakeModel().equals("Peloton")) {
                System.out.println("Setting gear to Peloton");
                garmin.setGear(activity.activityId(), gear.gearId()).orElseThrow();
                break;
            }
        }
    }
}
