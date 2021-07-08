package io.allezgo.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class FormsTests {
    @Test
    public void testMultipartFormatting() {
        Forms.MultipartUpload up =
                Forms.MultipartUpload.format("filename.tcx", "content", "012345");
        assertThat(up.boundary()).isEqualTo("012345");
        assertThat(up.content())
                .isEqualTo(
                        "--012345\r\n"
                                + "Content-Disposition: form-data; name=\"file\"; filename=\"filename.tcx\"\r\n"
                                + "Content-Type: application/octet-stream\r\n"
                                + "\r\n"
                                + "content\r\n"
                                + "--012345--");
    }

    @Test
    public void testBoundaryGeneration() {
        Forms.MultipartUpload up = Forms.MultipartUpload.of("filename.tcx", "content");
        assertThat(up.boundary()).matches(Pattern.compile("-----[0-9]+"));
    }
}
