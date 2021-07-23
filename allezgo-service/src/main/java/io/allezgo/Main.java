package io.allezgo;

import barista.Server;
import barista.authz.Authz;
import io.allezgo.endpoints.SyncPelotonToGarmin;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("Unexpected command line arguments.");
            return;
        }

        // TODO(markelliot): move this into Barista
        configureLogging();

        Authz authz = Authz.denyAll();
        Server.builder()
                .disableTls() // our host provides this for us
                .authz(authz)
                .endpoint(new SyncPelotonToGarmin())
                .allowOrigin("https://allezgo.io")
                .allowOrigin("http://localhost:8080") // for development
                .start();
    }

    private static final String STDOUT = "stdout";

    private static void configureLogging() {
        ConfigurationBuilder<BuiltConfiguration> builder =
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.ERROR)
                        .setConfigurationName("barista");

        AppenderComponentBuilder appenderBuilder =
                builder.newAppender(STDOUT, "CONSOLE")
                        .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
                        .add(
                                builder.newLayout("PatternLayout")
                                        .addAttribute(
                                                "pattern", "%d [%t] %-5level: %msg%n%throwable"))
                        .add(
                                builder.newFilter(
                                                "MarkerFilter",
                                                Filter.Result.DENY,
                                                Filter.Result.NEUTRAL)
                                        .addAttribute("marker", "FLOW"));

        builder.add(appenderBuilder)
                .add(
                        builder.newLogger("org.apache.logging.log4j", Level.DEBUG)
                                .add(builder.newAppenderRef(STDOUT))
                                .addAttribute("additivity", false))
                .add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef(STDOUT)));

        Configurator.initialize(builder.build());
    }
}
