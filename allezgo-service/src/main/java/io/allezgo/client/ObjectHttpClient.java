package io.allezgo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.HttpHeaders;
import com.markelliot.result.Result;
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
        return convertRequest(request)
                .flatMapResult(body -> send(newJsonRequest(endpoint).POST(body).build()))
                .flatMapResult(response -> convertResponse(response, responseClass));
    }

    public <Response> Result<Response, HttpError> get(
            Endpoint endpoint, Class<Response> responseClass) {
        return send(newJsonRequest(endpoint).GET().build())
                .flatMapResult(response -> convertResponse(response, responseClass));
    }

    public <Response> Result<Response, HttpError> upload(
            Endpoint endpoint, String filename, String fileContent, Class<Response> responseClass) {
        Forms.MultipartUpload upload = Forms.MultipartUpload.of(filename, fileContent);

        HttpRequest.Builder requestBuilder =
                HttpRequest.newBuilder()
                        .uri(endpoint.uri())
                        .setHeader(
                                HttpHeaders.CONTENT_TYPE,
                                "multipart/form-data; boundary=" + upload.boundary())
                        .setHeader(HttpHeaders.ACCEPT, "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(upload.content()));
        endpoint.headers().forEach(requestBuilder::setHeader);

        return send(requestBuilder.build())
                .flatMapResult(httpResp -> convertResponse(httpResp, responseClass));
    }

    private HttpRequest.Builder newJsonRequest(Endpoint endpoint) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(endpoint.uri())
                        .setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                        .setHeader(HttpHeaders.ACCEPT, "application/json");
        endpoint.headers().forEach(builder::setHeader);
        return builder;
    }

    private <Request> Result<HttpRequest.BodyPublisher, HttpError> convertRequest(Request request) {
        if (request == null) {
            return Result.ok(HttpRequest.BodyPublishers.ofString(""));
        }
        try {
            return Result.ok(
                    HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request)));
        } catch (JsonProcessingException jpe) {
            return HttpError.of("Unable to serialize request body", jpe);
        }
    }

    private Result<HttpResponse<String>, HttpError> send(HttpRequest httpRequest) {
        try {
            return Result.ok(httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException | InterruptedException e) {
            return HttpError.of("Error while making HTTP request", e);
        }
    }

    private <Response> Result<Response, HttpError> convertResponse(
            HttpResponse<String> httpResp, Class<Response> responseClass) {
        if (200 <= httpResp.statusCode() && httpResp.statusCode() < 300) {
            String body = httpResp.body();
            // protect against a few cases of 204 (or incorrectly not-204) empty
            // responses that might otherwise trip up the ObjectMapper
            if (body.isBlank()) {
                body = "{}";
            }
            try {
                return Result.ok(mapper.readValue(body, responseClass));
            } catch (JsonProcessingException jpe) {
                return HttpError.of("Unable to deserialize body", jpe);
            }
        } else {
            return HttpError.of(httpResp);
        }
    }
}
