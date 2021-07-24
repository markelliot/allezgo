package io.allezgo.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class Forms {
    private Forms() {}

    public record MultipartUpload(String boundary, String content) {
        public static MultipartUpload of(String filename, String fileContent) {
            String boundary = "----" + Math.abs(ThreadLocalRandom.current().nextLong() / 2);
            return format(filename, fileContent, boundary);
        }

        @VisibleForTesting
        static MultipartUpload format(String filename, String fileContent, String boundary) {
            String marker = "--" + boundary + "\r\n";
            String header =
                    marker
                            + ("Content-Disposition: form-data; name=\"file\"; filename=\""
                                    + filename
                                    + "\"\r\n")
                            + "Content-Type: application/octet-stream\r\n"
                            + "\r\n";
            String terminator = "\r\n--" + boundary + "--";
            String content = header + fileContent + terminator;
            return new MultipartUpload(boundary, content);
        }
    }

    public static HttpRequest.BodyPublisher bodyPublisher(Map<String, String> params) {
        return HttpRequest.BodyPublishers.ofString(encode(params));
    }

    private static String encode(Map<String, String> params) {
        Escaper escaper = UrlEscapers.urlFormParameterEscaper();
        return Joiner.on("&")
                .withKeyValueSeparator("=")
                .join(Maps.transformValues(params, escaper::escape));
    }
}
