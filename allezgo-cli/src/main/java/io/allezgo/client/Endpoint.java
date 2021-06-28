package io.allezgo.client;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public final class Endpoint {
    private final URI uri;
    private final ImmutableMap<String, String> query;

    private Endpoint(URI uri, Map<String, String> query) {
        Preconditions.checkState(query.isEmpty() || uri.getQuery() == null,
                "cannot combine query parameters and a URI that already contains a query string");
        this.uri = uri;
        this.query = ImmutableMap.copyOf(query);
    }

    public Endpoint path(String... path) {
        return new Endpoint(uri.resolve(Joiner.on('/').join(path)), query);
    }

    public Endpoint query(String key, Object value) {
        return new Endpoint(uri, ImmutableMap.<String, String>builder()
                .putAll(query)
                .put(key, UrlEscapers.urlFormParameterEscaper().escape(String.valueOf(value)))
                .build());
    }

    public URI uri() {
        String queryString = Joiner.on("&").withKeyValueSeparator("=").join(query);

        try {
            return
                    new URI(
                            uri.getScheme(),
                            uri.getUserInfo(),
                            uri.getHost(),
                            uri.getPort(),
                            uri.getPath(),
                            queryString,
                            uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Illegal URI syntax", e);
        }
    }

    public static Endpoint of(URI base) {
        return new Endpoint(base, ImmutableMap.of());
    }
}
