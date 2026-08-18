package uk.police.k9.dogs.controller;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.police.k9.dogs.dto.GenderResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The genders endpoint. Gender is the one enumerated value on a dog that the force does not
 * maintain, so it is published read-only - a client can still build its drop-down from the API
 * rather than hard-coding the values.
 */
@MicronautTest
@DisplayName("GET /api/dogs/genders")
class GenderApiTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Test
    @DisplayName("publishes the permitted values with the codes a dog is created with")
    void listsGenders() {
        List<GenderResponse> genders = httpClient.toBlocking().retrieve(
                HttpRequest.GET(ApiPaths.GENDERS), Argument.listOf(GenderResponse.class));

        assertThat(genders).containsExactly(
                new GenderResponse("MALE", "Male"),
                new GenderResponse("FEMALE", "Female"));
    }
}
