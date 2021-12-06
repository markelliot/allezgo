package io.allezgo.client;

import com.markelliot.result.Result;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.http.HttpResponse;
import java.util.Optional;

public record HttpError(int status, String body, Optional<String> comment) {
    public static <T> Result<T, HttpError> of(HttpResponse<String> resp, String comment) {
        return Result.error(new HttpError(resp.statusCode(), resp.body(), Optional.of(comment)));
    }

    public static <T> Result<T, HttpError> of(HttpResponse<String> resp) {
        return Result.error(new HttpError(resp.statusCode(), resp.body(), Optional.empty()));
    }

    public static <T> Result<T, HttpError> of(String comment) {
        return Result.error(new HttpError(500, "<>", Optional.of(comment)));
    }

    public static <T> Result<T, HttpError> of(String comment, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return Result.error(new HttpError(500, "<>", Optional.of(comment + ": " + sw)));
    }

    public IllegalStateException toException() {
        return new IllegalStateException(comment.orElse("Error") + ": " + status + ": " + body);
    }
}
