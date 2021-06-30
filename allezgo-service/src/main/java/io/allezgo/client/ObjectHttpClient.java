package io.allezgo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.HttpHeaders;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class ObjectHttpClient {
    private static final ObjectMapper mapper =
            new ObjectMapper()
                    .registerModule(new GuavaModule())
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule())
                    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final HttpClient httpClient = HttpClient.newBuilder().build();

    public <Request, Response> Result<Response, HttpError> post(
            Endpoint endpoint, Request request, Class<Response> responseClass) {
        try {
            String reqStr = request != null ? mapper.writeValueAsString(request) : "";

            HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder()
                            .uri(endpoint.uri())
                            .setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                            .setHeader(HttpHeaders.ACCEPT, "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(reqStr));
            endpoint.headers().forEach(requestBuilder::setHeader);
            HttpResponse<String> httpResp =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (httpResp.statusCode() == 200) {
                return Result.ok(mapper.readValue(httpResp.body(), responseClass));
            }
            if (httpResp.statusCode() == 204) {
                return Result.ok(mapper.readValue("{}", responseClass));
            }
            return Result.error(HttpError.of(httpResp));
        } catch (IOException | InterruptedException e) {
            return Result.error(HttpError.of(e.getMessage()));
        }
    }

    public <Response> Result<Response, HttpError> get(
            Endpoint endpoint, Class<Response> responseClass) {
        try {
            HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder()
                            .uri(endpoint.uri())
                            .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .setHeader(HttpHeaders.ACCEPT, "application/json")
                            .GET();
            endpoint.headers().forEach(requestBuilder::setHeader);
            HttpResponse<String> httpResp =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (httpResp.statusCode() != 200) {
                return Result.error(HttpError.of(httpResp));
            }
            return Result.ok(mapper.readValue(httpResp.body(), responseClass));
        } catch (IOException | InterruptedException e) {
            return Result.error(HttpError.of(e.getMessage()));
        }
    }

    public <Response> Result<Response, HttpError> upload(
            Endpoint endpoint, String filename, String fileContent, Class<Response> responseClass) {
        Forms.MultipartUpload upload = Forms.MultipartUpload.of(filename, fileContent);

        try {
            HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder()
                            .uri(endpoint.uri())
                            .setHeader(
                                    HttpHeaders.CONTENT_TYPE,
                                    "multipart/form-data; boundary=" + upload.boundary())
                            .POST(HttpRequest.BodyPublishers.ofString(upload.content()));
            endpoint.headers().forEach(requestBuilder::setHeader);

            HttpResponse<String> httpResp =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (httpResp.statusCode() < 200 || 300 <= httpResp.statusCode()) {
                return Result.error(HttpError.of(httpResp));
            }
            return Result.ok(mapper.readValue(httpResp.body(), responseClass));
        } catch (IOException | InterruptedException e) {
            return Result.error(HttpError.of(e.getMessage()));
        }
    }
}
