package barista;

public final class Endpoints {
    private interface Endpoint<Request> {
        Class<Request> requestClass();

        String template();

        HttpMethod method();
    }

    public interface VerifiedAuth<Request, Response> extends Endpoint<Request> {
        Response call(VerifiedAuthToken authToken, Request request);
    }

    public interface Open<Request, Response> extends Endpoint<Request> {
        Response call(Request request);
    }
}
