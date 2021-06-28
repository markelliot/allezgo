package io.allezgo.sink.garmin;

import com.google.common.base.Suppliers;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.allezgo.client.Endpoint;
import io.allezgo.client.Forms;
import io.allezgo.client.HttpError;
import io.allezgo.client.ObjectHttpClient;
import io.allezgo.client.Result;
import io.allezgo.config.Configuration;
import io.allezgo.sink.tcx.Tcx;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class GarminClient {
    private static final ObjectHttpClient client = new ObjectHttpClient();
    private static final Endpoint.Base base = Endpoint.of("https://connect.garmin.com/modern/");

    private static final URI APP_URI = URI.create("https://connect.garmin.com/modern/");
    private static final URI LOGIN_URI =
            URI.create(
                    "https://sso.garmin.com/sso/signin"
                            + "?service=https://connect.garmin.com/modern"
                            + "&clientId=GarminConnect"
                            + "&gauthHost=https://sso.garmin.com/sso&consumeServiceTicket=false");
    private static final String GARMIN_SSO = "https://sso.garmin.com";

    private final Supplier<GarminSession> session;
    private final String pelotonGear;

    public GarminClient(Configuration.Garmin conf) {
        this.session =
                Suppliers.memoize(
                        () ->
                                login(conf.email(), conf.password())
                                        .orElseThrow(HttpError::toException));
        this.pelotonGear = conf.pelotonGear();
    }

    public String pelotonGear() {
        return pelotonGear;
    }

    /**
     * Performs a Garmin login.
     *
     * <p>Garmin logins consist of the following sequence:
     *
     * <ol>
     *   <li>POST username and password to sso.garmin.com as if submitted by an embedded login form.
     *       Note that we need to use the embedded form as otherwise the submission requires a CSRF
     *       token that needs to be read by loading the login HTML and extracting from the rendered
     *       login form. We avoid all the normal browser protections for CSRF anyway since this is
     *       Java and not a browser.
     *       <p>Also note that the login URI includes a query parameter {@code
     *       consumeServiceTicket=false} that seems to discard the need for us to do anything
     *       besides the next steps of following redirects (without the parameter the redirects no
     *       longer guide the initialization process).
     *   <li>Initialize the token and identify our load balancer by navigating to
     *       connect.garmin.com/modern with all the cookies we obtained from the first step.
     *   <li>Follow some number of redirects that, in turn, seem to activate our token and prime one
     *       of the load balancers to accept that token.
     *   <li>Extract the three cookies we need to have a live session with the Garmin API
     *       <ul>
     *         <li>{@code __cflb}: the load balancer that will accept the session
     *         <li>{@code SESSIONID}: the actual session
     *         <li>{@code GARMIN-SSO-GUID}: what appears to be a user-identifying token that goes
     *             with {@code SESSIONID}
     *       </ul>
     * </ol>
     *
     * <p>The original implementation of this method tried to explicitly manage the cookies passed
     * from domain to domain, but ultimately degraded to passing all the cookies seen up to the next
     * page load, so this method dedicates a client and a cookie handler for each invocation and
     * simply grabs the final three cookies to load a session object rather than something more
     * precise.
     */
    public Result<GarminSession, HttpError> login(String email, String password) {
        CookieManager cookieHandler =
                new CookieManager(/* InMemoryCookieStore */ null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookieHandler).build();

        try {
            HttpResponse<String> loginResp =
                    client.send(
                            HttpRequest.newBuilder()
                                    .uri(LOGIN_URI)
                                    .setHeader(HttpHeaders.ORIGIN, GARMIN_SSO)
                                    .setHeader(
                                            HttpHeaders.CONTENT_TYPE,
                                            MediaType.FORM_DATA.toString())
                                    .POST(
                                            Forms.bodyPublisher(
                                                    Map.of(
                                                            "username",
                                                            email,
                                                            "password",
                                                            password,
                                                            "_eventId",
                                                            "submit",
                                                            "embed",
                                                            "true")))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            if (loginResp.statusCode() != 200) {
                return Result.error(HttpError.of(loginResp, "Error while logging in"));
            }

            int count = 0;
            URI location = APP_URI;
            while (true) {
                HttpResponse<String> initTicketResp =
                        client.send(
                                HttpRequest.newBuilder().uri(location).GET().build(),
                                HttpResponse.BodyHandlers.ofString());

                if (initTicketResp.statusCode() == 200) {
                    break;
                }

                count++;
                if (count > 7) {
                    return Result.error(
                            HttpError.of("Failed to initialize token after 7 redirects"));
                }

                // follow redirects
                Optional<String> newLoc = initTicketResp.headers().firstValue("location");
                if (newLoc.isEmpty()) {
                    break;
                }

                location = URI.create(newLoc.get());
            }
        } catch (IOException | InterruptedException e) {
            return Result.error(HttpError.of(e.getMessage()));
        }

        Optional<String> sessionId = findCookie(cookieHandler, "SESSIONID");
        Optional<String> garminSsoGuid = findCookie(cookieHandler, "GARMIN-SSO-GUID");
        Optional<String> cflb = findCookieOnDomain(cookieHandler, "__cflb", "connect.garmin.com");
        if (sessionId.isEmpty() || garminSsoGuid.isEmpty() || cflb.isEmpty()) {
            return Result.error(
                    HttpError.of("Missing required cookies from login process, login failed"));
        }
        return Result.ok(new GarminSession(sessionId.get(), garminSsoGuid.get(), cflb.get()));
    }

    private Optional<String> findCookie(CookieManager cookieHandler, String cookieName) {
        return cookieHandler.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals(cookieName))
                .findFirst()
                .map(HttpCookie::getValue);
    }

    private Optional<String> findCookieOnDomain(
            CookieManager cookieHandler, String cookieName, String domain) {
        return cookieHandler.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals(cookieName))
                .filter(c -> c.getDomain().equals(domain))
                .findFirst()
                .map(HttpCookie::getValue);
    }

    public Result<GarminActivityId, HttpError> uploadTcx(Tcx tcx) {
        Result<GarminUploadResponse, HttpError> maybeResult = rawUploadTcx(tcx);
        if (maybeResult.isError()) {
            return maybeResult.mapResult(u -> null);
        }
        GarminUploadResponse result = maybeResult.result();
        List<GarminUploadResponse.Success> successes = result.detailedImportResult().successes();
        if (successes.size() != 1) {
            return Result.error(
                    HttpError.of("Upload was a success but the response contained no information"));
        }
        return Result.ok(successes.get(0).activityId());
    }

    public Result<GarminUploadResponse, HttpError> rawUploadTcx(Tcx tcx) {
        return client.upload(
                base.path("proxy", "upload-service", "upload", ".tcx")
                        .header("nk", "NT")
                        .header("cookie", session.get().toCookies())
                        .build(),
                "activity.tcx",
                tcx.value(),
                GarminUploadResponse.class);
    }

    public Result<GarminUpdateActivityResponse, HttpError> updateActivity(
            GarminActivityId activityId, String title, String description) {
        Result<GarminUpdateActivityResponse, HttpError> resp =
                updateActivityField(
                        activityId,
                        new GarminUpdateActivityTitleRequest(activityId.asLong(), title));

        if (resp.isError()) {
            return resp;
        }

        // TODO(markelliot): ideally we'd make a single request so that this is transactional
        return updateActivityField(
                activityId,
                new GarminUpdateActivityDescriptionRequest(activityId.asLong(), description));
    }

    private Result<GarminUpdateActivityResponse, HttpError> updateActivityField(
            GarminActivityId activityId, Object request) {
        return client.post(
                base.path("proxy", "activity-service", "activity", activityId.value())
                        .header("x-http-method-override", "PUT")
                        .header("nk", "NT")
                        .header("cookie", session.get().toCookies())
                        .build(),
                request,
                GarminUpdateActivityResponse.class);
    }

    public Result<List<GarminActivity>, HttpError> activities(int start, int limit) {
        return client.get(
                        base.path(
                                        "proxy",
                                        "activitylist-service",
                                        "activities",
                                        "search",
                                        "activities")
                                .query("limit", limit)
                                .query("start", start)
                                .header("nk", "NT")
                                .header("cookie", session.get().toCookies())
                                .build(),
                        GarminActivitiesResponse.class)
                .mapResult(GarminActivitiesResponse::activities);
    }

    public Result<List<GarminGear>, HttpError> gear(GarminActivityId activityId) {
        return client.get(
                        base.path("proxy", "gear-service", "gear", "filterGear")
                                .query("activityId", activityId)
                                .header("nk", "NT")
                                .header("cookie", session.get().toCookies())
                                .build(),
                        GarminGearResponse.class)
                .mapResult(GarminGearResponse::gear);
    }

    public Result<List<GarminGear>, HttpError> availableGear(LocalDate date, GarminUserId userId) {
        return client.get(
                        base.path("proxy", "gear-service", "gear", "filterGear")
                                .query("availableGearDate", date)
                                .query("userProfilePk", userId)
                                .header("nk", "NT")
                                .header("cookie", session.get().toCookies())
                                .build(),
                        GarminGearResponse.class)
                .mapResult(GarminGearResponse::gear);
    }

    public Result<GarminGear, HttpError> setNamedGear(
            GarminActivityId activityId, LocalDate activityDate, String gearName) {
        Result<GarminUserId, HttpError> userId = userId();
        if (userId.isError()) {
            return Result.error(userId.error());
        }

        Result<List<GarminGear>, HttpError> availableGear =
                availableGear(activityDate, userId.result());
        if (availableGear.isError()) {
            return Result.error(availableGear.error());
        }

        Optional<GarminGear> desiredGear =
                availableGear.result().stream()
                        .filter(g -> g.customMakeModel().equals(gearName))
                        .findFirst();

        if (desiredGear.isEmpty()) {
            return Result.error(HttpError.of("Cannot find requested gear"));
        }

        return setGear(activityId, desiredGear.get().gearId());
    }

    public Result<GarminGear, HttpError> setGear(
            GarminActivityId activityId, GarminGearId gearUuid) {
        GarminGear desiredGear = null;
        for (GarminGear gear : gear(activityId).orElseThrow(HttpError::toException)) {
            if (!gear.gearId().equals(gearUuid)) {
                Result<GarminGear, HttpError> deleteResult = deleteGear(activityId, gear.gearId());
                if (deleteResult.isError()) {
                    return deleteResult.mapResult(_r -> null);
                }
            } else {
                desiredGear = gear;
            }
        }
        if (desiredGear == null) {
            return addGear(activityId, gearUuid);
        }
        return Result.ok(desiredGear);
    }

    private Result<GarminGear, HttpError> deleteGear(
            GarminActivityId activityId, GarminGearId gearUuid) {
        return client.post(
                base.path(
                                "proxy",
                                "gear-service",
                                "gear",
                                "unlink",
                                gearUuid.value(),
                                "activity",
                                activityId.value())
                        .header("nk", "NT")
                        .header("x-http-method-override", "PUT")
                        .header("cookie", session.get().toCookies())
                        .build(),
                null,
                GarminGear.class);
    }

    private Result<GarminGear, HttpError> addGear(
            GarminActivityId activityId, GarminGearId gearUuid) {
        return client.post(
                base.path(
                                "proxy",
                                "gear-service",
                                "gear",
                                "link",
                                gearUuid.value(),
                                "activity",
                                activityId.value())
                        .header("nk", "NT")
                        .header("x-http-method-override", "PUT")
                        .header("cookie", session.get().toCookies())
                        .build(),
                null,
                GarminGear.class);
    }

    public Result<GarminUserId, HttpError> userId() {
        return client.get(
                        base.path("proxy", "userprofile-service", "userprofile", "location")
                                .header("nk", "NT")
                                .header("cookie", session.get().toCookies())
                                .build(),
                        GarminUserLocationResponse.class)
                .mapResult(GarminUserLocationResponse::userProfileId);
    }
}
