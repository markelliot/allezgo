package io.allezgo.client;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.UrlEscapers;
import java.net.URI;
import java.net.URISyntaxException;

public final class Endpoint {
    private final URI uri;
    private final ImmutableMap<String, String> headers;

    private Endpoint(URI uri, ImmutableMap<String, String> query, ImmutableMap<String, String> headers) {
        String queryString = Joiner.on("&").withKeyValueSeparator("=").join(query);
        try {
            this.uri = new URI(
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
        this.headers = headers;
    }

    public URI uri() {
        return uri;
    }

    public ImmutableMap<String, String> headers() {
        return headers;
    }

    public static Endpoint.Base of(String base) {
        return new Base(URI.create(base));
    }

    public static final class Base {
        private final URI base;

        private Base(URI base) {
            Preconditions.checkArgument(base.getQuery() == null,
                    "query parameters must be added using the Builder");
            this.base = base;
        }

        public Builder path(String... path) {
            return new Builder(base.resolve(Joiner.on('/').join(path)));
        }
    }

    public static final class Builder {
        private final URI uri;
        private final ImmutableMap.Builder<String, String> queryArgs = ImmutableMap.builder();
        private final ImmutableMap.Builder<String, String> headers = ImmutableMap.builder();

        private Builder(URI uri) {
            this.uri = uri;
        }

        public <T> Builder query(String key, T value) {
            queryArgs.put(key, UrlEscapers.urlFormParameterEscaper().escape(String.valueOf(value)));
            return this;
        }

        public <T> Builder header(String header, T value) {
            headers.put(header, String.valueOf(value));
            return this;
        }

        public Endpoint build() {
            return new Endpoint(uri, queryArgs.build(), headers.build());
        }
    }
}
