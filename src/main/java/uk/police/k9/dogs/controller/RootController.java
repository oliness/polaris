package uk.police.k9.dogs.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Hidden;

import java.net.URI;

/**
 * A signpost from the server root to the register, so the address the server logs on start-up does
 * not answer {@code 404}. It is {@code 303 See Other} rather than the {@code 301} that
 * {@code HttpResponse.redirect} gives, which a browser would cache indefinitely, and it is hidden
 * from the OpenAPI document, being a convenience rather than part of the contract.
 */
@Controller
@Hidden
public class RootController {

    @Get
    public HttpResponse<?> index() {
        return HttpResponse.seeOther(URI.create(ApiPaths.DOGS));
    }
}
