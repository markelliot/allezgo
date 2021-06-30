package barista;

import io.undertow.util.HttpString;
import io.undertow.util.Methods;

public enum HttpMethod {
    GET(Methods.GET),
    PUT(Methods.PUT),
    POST(Methods.POST),
    DELETE(Methods.DELETE);

    private final HttpString method;

    private HttpMethod(HttpString method) {
        this.method = method;
    }

    HttpString method() {
        return method;
    }
}
