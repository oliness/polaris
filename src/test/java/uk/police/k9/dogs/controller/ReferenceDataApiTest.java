package uk.police.k9.dogs.controller;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.dto.ReferenceDataRequest;
import uk.police.k9.dogs.dto.ReferenceDataResponse;
import uk.police.k9.dogs.exception.ApiError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The endpoints behind the enumerated values a dog carries. Statuses and leaving reasons are the
 * same endpoint over different tables, so these tests cover both.
 */
@MicronautTest(transactional = false)
@Property(name = "datasources.default.url", value = "jdbc:h2:mem:reference-api;DB_CLOSE_DELAY=-1")
@DisplayName("The enumerated values a dog can hold")
class ReferenceDataApiTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    private BlockingHttpClient client;

    @BeforeEach
    void setUp() {
        client = httpClient.toBlocking();
    }

    @Test
    @DisplayName("lists the statuses the force recognises, in the order they should be shown")
    void listsTheSeededStatuses() {
        PagedResponse<ReferenceDataResponse> page = list(ApiPaths.STATUSES, "");

        assertThat(page.content()).extracting(ReferenceDataResponse::code)
                .containsExactly("IN_TRAINING", "IN_SERVICE", "RETIRED", "LEFT");
        assertThat(page.content()).extracting(ReferenceDataResponse::label)
                .containsExactly("In Training", "In Service", "Retired", "Left");
    }

    @Test
    @DisplayName("lists the reasons a dog can leave the force")
    void listsTheSeededLeavingReasons() {
        PagedResponse<ReferenceDataResponse> page = list(ApiPaths.LEAVING_REASONS, "");

        assertThat(page.content()).extracting(ReferenceDataResponse::code)
                .containsExactly("TRANSFERRED", "RETIRED_PUT_DOWN", "KIA", "REJECTED",
                        "RETIRED_REHOUSED", "DIED");
    }

    @Test
    @DisplayName("takes a new value the force has started using")
    void addsAValue() {
        HttpResponse<ReferenceDataResponse> response = client.exchange(
                HttpRequest.POST(ApiPaths.STATUSES, new ReferenceDataRequest("STAND_DOWN",
                        "Stand Down", "Temporarily withdrawn from duty.", 25)),
                ReferenceDataResponse.class);

        assertThat(response.code()).isEqualTo(HttpStatus.CREATED.getCode());
        ReferenceDataResponse status = response.body();
        assertThat(status.code()).isEqualTo("STAND_DOWN");
        assertThat(list(ApiPaths.STATUSES, "").content()).extracting(ReferenceDataResponse::code)
                .containsExactly("IN_TRAINING", "IN_SERVICE", "STAND_DOWN", "RETIRED", "LEFT");

        client.exchange(HttpRequest.DELETE(ApiPaths.STATUSES + "/" + status.id()));
    }

    @Test
    @DisplayName("renames a value without losing the dogs that hold it")
    void updatesAValue() {
        Long id = create("REST_DAY", "Rest Day");

        ReferenceDataResponse updated = client.retrieve(HttpRequest.PUT(ApiPaths.STATUSES + "/" + id,
                new ReferenceDataRequest("REST_DAY", "Rest Day (Kennelled)", null, 60)),
                ReferenceDataResponse.class);

        assertThat(updated.label()).isEqualTo("Rest Day (Kennelled)");
        assertThat(updated.version()).isEqualTo(1);

        client.exchange(HttpRequest.DELETE(ApiPaths.STATUSES + "/" + id));
    }

    @Test
    @DisplayName("refuses an update whose If-Match someone else has already moved on from")
    void refusesAStalePrecondition() {
        Long id = create("REST_DAY", "Rest Day");
        String staleTag = client.exchange(HttpRequest.GET(ApiPaths.STATUSES + "/" + id),
                ReferenceDataResponse.class).getHeaders().get(HttpHeaders.ETAG);

        client.exchange(HttpRequest.PUT(ApiPaths.STATUSES + "/" + id,
                new ReferenceDataRequest("REST_DAY", "Rest Day (Kennelled)", null, 60)));

        ApiError error = failure(HttpStatus.PRECONDITION_FAILED,
                HttpRequest.PUT(ApiPaths.STATUSES + "/" + id,
                                new ReferenceDataRequest("REST_DAY", "Something Else", null, 60))
                        .header(HttpHeaders.IF_MATCH, staleTag));

        assertThat(error.message()).isEqualTo("Dog status " + id
                + " has changed since you read it. Fetch it again and re-apply the change.");

        client.exchange(HttpRequest.DELETE(ApiPaths.STATUSES + "/" + id));
    }

    @Test
    @DisplayName("retires a value: gone from the list, still in the database")
    void retiresAValue() {
        Long id = create("PROBATION", "Probation");

        HttpResponse<?> response = client.exchange(HttpRequest.DELETE(ApiPaths.STATUSES + "/" + id));

        assertThat(response.code()).isEqualTo(HttpStatus.NO_CONTENT.getCode());
        assertThat(list(ApiPaths.STATUSES, "").content())
                .extracting(ReferenceDataResponse::code).doesNotContain("PROBATION");
        assertThat(list(ApiPaths.STATUSES, "?includeDeleted=true").content())
                .filteredOn(value -> "PROBATION".equals(value.code()))
                .singleElement()
                .satisfies(value -> assertThat(value.deleted()).isTrue());
    }

    @Test
    @DisplayName("refuses a code another active value already uses")
    void refusesADuplicateCode() {
        ApiError error = failure(HttpStatus.CONFLICT, HttpRequest.POST(ApiPaths.STATUSES,
                new ReferenceDataRequest("IN_SERVICE", "In Service (duplicate)", null, 20)));

        assertThat(error.message()).isEqualTo("Dog status with code 'IN_SERVICE' already exists");
    }

    @Test
    @DisplayName("insists on a code a client can rely on, not free text")
    void refusesAMalformedCode() {
        ApiError error = failure(HttpStatus.BAD_REQUEST, HttpRequest.POST(ApiPaths.STATUSES,
                new ReferenceDataRequest("in service", "In Service", null, 20)));

        assertThat(error.details()).extracting(ApiError.FieldError::field).containsExactly("code");
        assertThat(error.details().getFirst().message())
                .isEqualTo("must be upper case letters, digits and underscores, starting with a letter");
    }

    @Test
    @DisplayName("maintains leaving reasons the same way")
    void maintainsLeavingReasons() {
        HttpResponse<ReferenceDataResponse> response = client.exchange(
                HttpRequest.POST(ApiPaths.LEAVING_REASONS, new ReferenceDataRequest("SOLD",
                        "Sold", "Sold to a partner agency.", 70)),
                ReferenceDataResponse.class);

        assertThat(response.code()).isEqualTo(HttpStatus.CREATED.getCode());
        assertThat(list(ApiPaths.LEAVING_REASONS, "").content())
                .extracting(ReferenceDataResponse::code).contains("SOLD");

        client.exchange(HttpRequest.DELETE(ApiPaths.LEAVING_REASONS + "/" + response.body().id()));
    }

    @Test
    @DisplayName("reports an identifier that was never issued")
    void reportsAnUnknownValue() {
        ApiError error = failure(HttpStatus.NOT_FOUND, HttpRequest.GET(ApiPaths.STATUSES + "/9999"));

        assertThat(error.message()).isEqualTo("Dog status 9999 does not exist");
    }

    private Long create(String code, String label) {
        return client.retrieve(HttpRequest.POST(ApiPaths.STATUSES,
                new ReferenceDataRequest(code, label, null, 50)), ReferenceDataResponse.class).id();
    }

    @SuppressWarnings("unchecked")
    private PagedResponse<ReferenceDataResponse> list(String path, String query) {
        return client.retrieve(HttpRequest.GET(path + query),
                Argument.of(PagedResponse.class, ReferenceDataResponse.class));
    }

    private ApiError failure(HttpStatus expected, HttpRequest<?> request) {
        try {
            client.exchange(request, Argument.STRING);
            throw new AssertionError("Expected the request to fail with " + expected);
        } catch (HttpClientResponseException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(expected.getCode());
            return e.getResponse().getBody(ApiError.class).orElseThrow();
        }
    }
}
