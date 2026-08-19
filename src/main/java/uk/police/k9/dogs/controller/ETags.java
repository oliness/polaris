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
 * The {@code ETag} and {@code If-Match} headers that carry a record's version over HTTP. The
 * {@code version} column changes on every update and on nothing else, as a strong validator must.
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
     * The versions an {@code If-Match} will accept, or {@code null} when no precondition was set.
     * A tag this API could not have issued is dropped rather than rejected, leaving an empty set
     * that fails the precondition - which covers weak tags too, {@code If-Match} comparing strongly.
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
