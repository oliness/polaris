package uk.police.k9.dogs.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@code ETag} and {@code If-Match} headers that carry a record's version over HTTP.
 *
 * <p>A record's {@code version} column is its entity tag: it changes on every update and on
 * nothing else, which is exactly what a strong validator has to promise.
 */
final class ETags {

    private static final String ANY = "*";
    private static final String WEAK_PREFIX = "W/";

    private ETags() {
    }

    /** The entity tag for a record at this version, e.g. {@code "3"}. */
    static String of(Long version) {
        return "\"" + version + "\"";
    }

    static <T> HttpResponse<T> ok(T body, Long version) {
        return HttpResponse.ok(body).header(HttpHeaders.ETAG, of(version));
    }

    static <T> HttpResponse<T> created(T body, URI location, Long version) {
        return HttpResponse.created(body, location).header(HttpHeaders.ETAG, of(version));
    }

    /**
     * The versions an {@code If-Match} header will accept, or {@code null} when the caller set no
     * precondition. {@code *} asks only that the record exist, which by the time it has been found
     * is no constraint at all.
     *
     * <p>A tag this API could never have issued cannot match anything, so it is dropped rather
     * than rejected - leaving an empty set, which fails the precondition. That covers a weak tag
     * too: {@code If-Match} compares strongly, so {@code W/"3"} matches nothing at all.
     */
    @Nullable
    static Set<Long> expectedVersions(@Nullable String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank() || ANY.equals(ifMatch.trim())) {
            return null;
        }
        return Arrays.stream(ifMatch.split(","))
                .map(ETags::version)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    private static Long version(String tag) {
        String trimmed = tag.trim();
        if (trimmed.startsWith(WEAK_PREFIX) || trimmed.length() < 2
                || !trimmed.startsWith("\"") || !trimmed.endsWith("\"")) {
            return null;
        }
        try {
            return Long.valueOf(trimmed.substring(1, trimmed.length() - 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
