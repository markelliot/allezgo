package io.allezgo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.HttpHeaders;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
            HttpResponse<String> httpResp =
                    httpClient.send(
                            newJsonRequest(endpoint)
                                    .POST(HttpRequest.BodyPublishers.ofString(reqStr))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            return convertResponse(httpResp, responseClass);
        } catch (IOException | InterruptedException e) {
            return Result.error(HttpError.of(e.getMessage()));
        }
    }

    public <Response> Result<Response, HttpError> get(
            Endpoint endpoint, Class<Response> responseClass) {
        try {
            HttpResponse<String> httpResp =
                    httpClient.send(
                            newJsonRequest(endpoint).GET().build(),
                            HttpResponse.BodyHandlers.ofString());
            return convertResponse(httpResp, responseClass);
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
                            .setHeader(HttpHeaders.ACCEPT, "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(upload.content()));
            endpoint.headers().forEach(requestBuilder::setHeader);
            HttpResponse<String> httpResp =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return convertResponse(httpResp, responseClass);
        } catch (IOException | InterruptedException e) {
            return Result.error(HttpError.of(e.getMessage()));
        }
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

    private <Response> Result<Response, HttpError> convertResponse(
            HttpResponse<String> httpResp, Class<Response> responseClass)
            throws JsonProcessingException {
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
                StringWriter sw = new StringWriter();
                jpe.printStackTrace(new PrintWriter(sw));
                return Result.error(HttpError.of("Unable to deserialize body: " + sw));
            }
        } else {
            return Result.error(HttpError.of(httpResp));
        }
    }
}
