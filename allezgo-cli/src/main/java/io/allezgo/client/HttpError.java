package io.allezgo.client;

import java.net.http.HttpResponse;
import java.util.Optional;

public record HttpError(int status, String body, Optional<String> comment) {
    public static HttpError of(HttpResponse<String> resp, String comment) {
        return new HttpError(resp.statusCode(), resp.body(), Optional.of(comment));
    }

    public static HttpError of(HttpResponse<String> resp) {
        return new HttpError(resp.statusCode(), resp.body(), Optional.empty());
    }

    public static HttpError of(String comment) {
        return new HttpError(500, "<>", Optional.of(comment));
    }

    public IllegalStateException toException() {
        return new IllegalStateException(comment.orElse("Error") + ": " + status + ": " + body);
    }
}
