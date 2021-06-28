package io.allezgo.client;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public final class Endpoint {
    private final URI uri;

    private Endpoint(URI uri) {
        this.uri = uri;
    }

    public Endpoint path(String... path) {
        return new Endpoint(uri.resolve(Joiner.on('/').join(path)));
    }

    public Endpoint query(Map<String, Object> args) {
        Preconditions.checkArgument(uri.getQuery() == null, "Query string was already configured");

        Escaper escaper = UrlEscapers.urlFormParameterEscaper();
        String query =
                Joiner.on("&")
                        .withKeyValueSeparator("=")
                        .join(Maps.transformValues(args, a -> escaper.escape(String.valueOf(a))));

        try {
            URI newUri =
                    new URI(
                            uri.getScheme(),
                            uri.getUserInfo(),
                            uri.getHost(),
                            uri.getPort(),
                            uri.getPath(),
                            query,
                            uri.getFragment());
            return new Endpoint(newUri);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Illegal URI syntax", e);
        }
    }

    public URI uri() {
        return uri;
    }

    public static Endpoint of(URI base) {
        return new Endpoint(base);
    }
}
