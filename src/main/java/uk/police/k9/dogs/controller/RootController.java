package uk.police.k9.dogs.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;

import java.net.URI;

/**
 * A signpost from the server root to the register. Every endpoint of the API sits under
 * {@link ApiPaths#ROOT}, so the root itself answered {@code 404}, which reads as a broken
 * application to anyone opening the address the server logs on start-up.
 *
 * <p>It answers {@code 303 See Other} rather than the {@code 301} that {@code HttpResponse.redirect}
 * gives, because a browser caches a {@code 301} indefinitely and would go on following it even if
 * the root were later given something of its own to serve. It is hidden from the OpenAPI document,
 * being a convenience for a human typing an address rather than part of the published contract.
 */
@Controller
@Hidden
public class RootController {

    @Get
    public HttpResponse<?> index() {
        return HttpResponse.seeOther(URI.create(ApiPaths.DOGS));
    }
}
