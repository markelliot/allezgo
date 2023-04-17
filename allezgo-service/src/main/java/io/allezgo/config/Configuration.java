package io.allezgo.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;

public record Configuration(Peloton peloton, Garmin garmin) {
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final File DEFAULT_FILE = new File(System.getProperty("user.home"), ".allezgo");

    public static File defaultFile() {
        return DEFAULT_FILE;
    }

    public static Configuration fromDefaultFile() {
        try {
            return yamlMapper.readValue(DEFAULT_FILE, Configuration.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read default configuration file", e);
        }
    }

    public static boolean defaultFileExists() {
        return DEFAULT_FILE.exists();
    }

    public static String renderExample() {
        Configuration example = new Configuration(
                new Configuration.Peloton("<email>", "<password>"),
                new Configuration.Garmin("<email>", "<password>", "<name of Peloton gear>"));
        try {
            return yamlMapper.writeValueAsString(example);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to render example configuration file", e);
        }
    }

    public record Peloton(String email, String password) {}

    public record Garmin(String email, String password, String pelotonGear) {}
}
