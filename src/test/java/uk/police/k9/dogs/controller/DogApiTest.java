package uk.police.k9.dogs.controller;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.police.k9.dogs.dto.DogRequest;
import uk.police.k9.dogs.dto.DogResponse;
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.entity.Supplier;
import uk.police.k9.dogs.exception.ApiError;
import uk.police.k9.dogs.repository.DogRepository;
import uk.police.k9.dogs.repository.DogStatusRepository;
import uk.police.k9.dogs.repository.LeavingReasonRepository;
import uk.police.k9.dogs.repository.SupplierRepository;
import uk.police.k9.dogs.support.DogRequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dogs endpoint, exercised over HTTP against the real database. Each test starts from an empty
 * register - the dog and supplier tables are cleared, leaving the seeded reference data - so what
 * a list returns can be asserted exactly. The database is this class's own.
 */
@MicronautTest(transactional = false)
@Property(name = "datasources.default.url", value = "jdbc:h2:mem:dogs-api;DB_CLOSE_DELAY=-1")
@DisplayName("GET/POST/PUT/DELETE /api/dogs/dogs")
class DogApiTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    DogRepository dogRepository;
    @Inject
    SupplierRepository supplierRepository;
    @Inject
    DogStatusRepository dogStatusRepository;
    @Inject
    LeavingReasonRepository leavingReasonRepository;

    private BlockingHttpClient client;
    private Long supplierId;
    private Long otherSupplierId;
    private Long inServiceId;
    private Long inTrainingId;
    private Long rehousedId;

    @BeforeEach
    void setUp() {
        client = httpClient.toBlocking();
        dogRepository.deleteAll();
        supplierRepository.deleteAll();
        supplierId = supplier("Ravenscroft Working Dogs");
        otherSupplierId = supplier("Ashcombe Malinois");
        inServiceId = statusId("IN_SERVICE");
        inTrainingId = statusId("IN_TRAINING");
        rehousedId = leavingReasonId("RETIRED_REHOUSED");
    }

    @Test
    @DisplayName("registers a dog and says where to find it")
    void registersADog() {
        DogRequest request = DogRequestBuilder.aDog(supplierId, inServiceId)
                .name("Baxter")
                .breed("German Shepherd")
                .badgeId("K9-1041")
                .kennellingCharacteristic("Settles quickly")
                .build();

        HttpResponse<DogResponse> response = client.exchange(
                HttpRequest.POST(ApiPaths.DOGS, request), DogResponse.class);

        assertThat(response.code()).isEqualTo(HttpStatus.CREATED.getCode());
        DogResponse dog = response.body();
        assertThat(response.getHeaders().get(HttpHeaders.LOCATION))
                .isEqualTo(ApiPaths.DOGS + "/" + dog.id());
        assertThat(dog.name()).isEqualTo("Baxter");
        assertThat(dog.badgeId()).isEqualTo("K9-1041");
        assertThat(dog.gender().code()).isEqualTo("MALE");
        assertThat(dog.supplier().name()).isEqualTo("Ravenscroft Working Dogs");
        assertThat(dog.status().code()).isEqualTo("IN_SERVICE");
        assertThat(dog.deleted()).isFalse();
        assertThat(dog.createdAt()).isNotNull();
        assertThat(dogRepository.findById(dog.id())).isPresent();
    }

    @Test
    @DisplayName("returns a dog by its identifier")
    void returnsADog() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Nala").build());

        DogResponse dog = client.retrieve(HttpRequest.GET(ApiPaths.DOGS + "/" + id), DogResponse.class);

        assertThat(dog.id()).isEqualTo(id);
        assertThat(dog.name()).isEqualTo("Nala");
    }

    @Test
    @DisplayName("lists the dogs on the register")
    void listsDogs() {
        create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Baxter").build());
        create(DogRequestBuilder.aDog(supplierId, inTrainingId).name("Nala").build());

        PagedResponse<DogResponse> page = listDogs("");

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(DogResponse::name).containsExactly("Baxter", "Nala");
    }

    @Test
    @DisplayName("returns an empty list, not a missing one, when nothing matches")
    void returnsAnEmptyContentArray() {
        String body = client.retrieve(HttpRequest.GET(ApiPaths.DOGS), String.class);

        assertThat(body).contains("\"content\":[]");
    }

    @Test
    @DisplayName("splits the results into pages")
    void pagesTheResults() {
        List.of("Alfie", "Bruno", "Cass", "Dexter", "Echo")
                .forEach(name -> create(DogRequestBuilder.aDog(supplierId, inServiceId).name(name).build()));

        PagedResponse<DogResponse> first = listDogs("?size=2");
        PagedResponse<DogResponse> last = listDogs("?size=2&page=2");

        assertThat(first.totalElements()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.page()).isZero();
        assertThat(first.content()).extracting(DogResponse::name).containsExactly("Alfie", "Bruno");
        assertThat(last.page()).isEqualTo(2);
        assertThat(last.content()).extracting(DogResponse::name).containsExactly("Echo");
    }

    @Test
    @DisplayName("orders the results the way the caller asked")
    void sortsTheResults() {
        create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Alfie").build());
        create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Bruno").build());

        PagedResponse<DogResponse> page = listDogs("?sort=name,desc");

        assertThat(page.content()).extracting(DogResponse::name).containsExactly("Bruno", "Alfie");
    }

    @ParameterizedTest(name = "[{index}] {0} matches {1}")
    @CsvSource(delimiter = '|', value = {
            "{\"name\":\"bax\"}                                     | Baxter",
            "{\"name\":\"BAXTER\"}                                  | Baxter",
            "{\"breed\":\"malinois\"}                               | Nala",
            "{\"supplier\":\"ashcombe\"}                            | Nala",
            "{\"breed\":\"shepherd\",\"supplier\":\"ravenscroft\"} | Baxter"
    })
    @DisplayName("searches on name, breed and supplier")
    void filtersDogs(String filter, String expected) {
        create(DogRequestBuilder.aDog(supplierId, inServiceId)
                .name("Baxter").breed("German Shepherd").build());
        create(DogRequestBuilder.aDog(otherSupplierId, inServiceId)
                .name("Nala").breed("Belgian Malinois").build());

        PagedResponse<DogResponse> page = filter(filter);

        assertThat(page.content()).extracting(DogResponse::name).containsExactly(expected);
    }

    @Test
    @DisplayName("combines search terms, so a dog has to match all of them")
    void combinesFilterTerms() {
        create(DogRequestBuilder.aDog(supplierId, inServiceId)
                .name("Baxter").breed("German Shepherd").build());

        assertThat(filter("{\"name\":\"baxter\",\"breed\":\"malinois\"}").content()).isEmpty();
    }

    @Test
    @DisplayName("treats a wildcard in a search term as an ordinary character")
    void treatsWildcardsLiterally() {
        create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Baxter").build());

        assertThat(filter("{\"name\":\"%\"}").content()).isEmpty();
    }

    @Test
    @DisplayName("rejects a search term that is not one of the three it knows")
    void rejectsAnUnknownFilterTerm() {
        ApiError error = failure(HttpStatus.BAD_REQUEST,
                HttpRequest.GET(UriBuilder.of(ApiPaths.DOGS).queryParam("filter", "{\"colour\":\"black\"}").build()));

        assertThat(error.details()).isNotNull();
        assertThat(error.details()).extracting(ApiError.FieldError::field).containsExactly("filter");
        assertThat(error.details().getFirst().message()).contains("name, breed, supplier");
    }

    @Test
    @DisplayName("keeps a deleted dog out of the list but leaves the record in the database")
    void deletingADogHidesItWithoutRemovingIt() {
        Long kept = create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Baxter").build());
        Long removed = create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Nala").build());

        HttpResponse<?> response = client.exchange(HttpRequest.DELETE(ApiPaths.DOGS + "/" + removed));

        assertThat(response.code()).isEqualTo(HttpStatus.NO_CONTENT.getCode());
        assertThat(listDogs("").content()).extracting(DogResponse::id).containsExactly(kept);
        assertThat(dogRepository.findById(removed))
                .as("the row is still there, for audit")
                .hasValueSatisfying(dog -> assertThat(dog.getDeletedAt()).isNotNull());
    }

    @Test
    @DisplayName("includes deleted dogs when they are asked for, flagged as deleted")
    void listsDeletedDogsOnRequest() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Nala").build());
        client.exchange(HttpRequest.DELETE(ApiPaths.DOGS + "/" + id));

        PagedResponse<DogResponse> page = listDogs("?includeDeleted=true");

        assertThat(page.content()).singleElement().satisfies(dog -> {
            assertThat(dog.deleted()).isTrue();
            assertThat(dog.deletedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("still returns a deleted dog to anyone holding its identifier")
    void returnsADeletedDogById() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Nala").build());
        client.exchange(HttpRequest.DELETE(ApiPaths.DOGS + "/" + id));

        DogResponse dog = client.retrieve(HttpRequest.GET(ApiPaths.DOGS + "/" + id), DogResponse.class);

        assertThat(dog.deleted()).isTrue();
    }

    @Test
    @DisplayName("can be asked to delete the same dog twice")
    void deleteIsIdempotent() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inServiceId).build());

        client.exchange(HttpRequest.DELETE(ApiPaths.DOGS + "/" + id));
        HttpResponse<?> second = client.exchange(HttpRequest.DELETE(ApiPaths.DOGS + "/" + id));

        assertThat(second.code()).isEqualTo(HttpStatus.NO_CONTENT.getCode());
    }

    @Test
    @DisplayName("replaces a dog's details")
    void updatesADog() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inTrainingId).name("Nala").build());

        DogResponse updated = client.retrieve(HttpRequest.PUT(ApiPaths.DOGS + "/" + id,
                DogRequestBuilder.aDog(supplierId, inServiceId)
                        .name("Nala")
                        .badgeId("K9-2002")
                        .kennellingCharacteristic("Kennel alone")
                        .build()), DogResponse.class);

        assertThat(updated.status().code()).isEqualTo("IN_SERVICE");
        assertThat(updated.badgeId()).isEqualTo("K9-2002");
        assertThat(updated.kennellingCharacteristic()).isEqualTo("Kennel alone");
        assertThat(updated.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("hands out an entity tag to send back with the next update")
    void publishesAnEntityTag() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inTrainingId).name("Nala").build());

        HttpResponse<DogResponse> response = client.exchange(
                HttpRequest.GET(ApiPaths.DOGS + "/" + id), DogResponse.class);

        assertThat(response.getHeaders().get(HttpHeaders.ETAG))
                .isEqualTo("\"" + response.body().version() + "\"");
    }

    @Test
    @DisplayName("applies an update whose If-Match still matches, and moves the tag on")
    void appliesAMatchingPrecondition() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inTrainingId).name("Nala").build());
        String etag = etagOf(id);

        HttpResponse<DogResponse> response = client.exchange(
                HttpRequest.PUT(ApiPaths.DOGS + "/" + id,
                                DogRequestBuilder.aDog(supplierId, inServiceId).name("Nala").build())
                        .header(HttpHeaders.IF_MATCH, etag), DogResponse.class);

        assertThat(response.body().status().code()).isEqualTo("IN_SERVICE");
        assertThat(response.getHeaders().get(HttpHeaders.ETAG)).isNotEqualTo(etag);
    }

    @Test
    @DisplayName("refuses an update whose If-Match someone else has already moved on from")
    void refusesAStalePrecondition() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inTrainingId).name("Nala").build());
        String staleTag = etagOf(id);

        // Someone else gets their change in first, which moves the dog on a version.
        client.exchange(HttpRequest.PUT(ApiPaths.DOGS + "/" + id,
                DogRequestBuilder.aDog(supplierId, inServiceId).name("Nala").build()));

        ApiError error = failure(HttpStatus.PRECONDITION_FAILED,
                HttpRequest.PUT(ApiPaths.DOGS + "/" + id,
                                DogRequestBuilder.aDog(supplierId, inTrainingId).name("Rufus").build())
                        .header(HttpHeaders.IF_MATCH, staleTag));

        assertThat(error.message()).isEqualTo(
                "Dog " + id + " has changed since you read it. Fetch it again and re-apply the change.");
        assertThat(client.retrieve(HttpRequest.GET(ApiPaths.DOGS + "/" + id), DogResponse.class))
                .as("the first change survives; the second never landed")
                .satisfies(dog -> assertThat(dog.status().code()).isEqualTo("IN_SERVICE"));
    }

    @Test
    @DisplayName("updates unconditionally when If-Match is absent or a wildcard")
    void updatesWithoutAPrecondition() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inTrainingId).name("Nala").build());

        DogResponse updated = client.retrieve(HttpRequest.PUT(ApiPaths.DOGS + "/" + id,
                        DogRequestBuilder.aDog(supplierId, inServiceId).name("Nala").build())
                .header(HttpHeaders.IF_MATCH, "*"), DogResponse.class);

        assertThat(updated.status().code()).isEqualTo("IN_SERVICE");
    }

    @Test
    @DisplayName("records a dog leaving the force, with the reason")
    void recordsADogLeaving() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Rufus").build());

        DogResponse updated = client.retrieve(HttpRequest.PUT(ApiPaths.DOGS + "/" + id,
                DogRequestBuilder.aDog(supplierId, statusId("LEFT"))
                        .name("Rufus")
                        .left(LocalDate.of(2025, 3, 31), rehousedId)
                        .build()), DogResponse.class);

        assertThat(updated.leavingDate()).isEqualTo(LocalDate.of(2025, 3, 31));
        assertThat(updated.leavingReason().code()).isEqualTo("RETIRED_REHOUSED");
    }

    @Test
    @DisplayName("refuses a badge another serving dog already carries")
    void refusesADuplicateBadge() {
        create(DogRequestBuilder.aDog(supplierId, inServiceId).name("Baxter").badgeId("K9-1041").build());

        ApiError error = failure(HttpStatus.CONFLICT, HttpRequest.POST(ApiPaths.DOGS,
                DogRequestBuilder.aDog(supplierId, inServiceId).name("Nala").badgeId("K9-1041").build()));

        assertThat(error.message()).isEqualTo("Dog with badgeId 'K9-1041' already exists");
    }

    @Test
    @DisplayName("refuses to change a dog that has been deleted")
    void refusesToUpdateADeletedDog() {
        Long id = create(DogRequestBuilder.aDog(supplierId, inServiceId).build());
        client.exchange(HttpRequest.DELETE(ApiPaths.DOGS + "/" + id));

        ApiError error = failure(HttpStatus.CONFLICT, HttpRequest.PUT(ApiPaths.DOGS + "/" + id,
                DogRequestBuilder.aDog(supplierId, inServiceId).build()));

        assertThat(error.message()).contains("has been deleted and is retained for audit only");
    }

    @Test
    @DisplayName("reports the fields that were rejected")
    void reportsValidationFailures() {
        DogRequest request = new DogRequest("", "German Shepherd", supplierId, null, null,
                LocalDate.of(2020, 3, 14), LocalDate.of(2021, 1, 6), inServiceId, null, null, null);

        ApiError error = failure(HttpStatus.BAD_REQUEST, HttpRequest.POST(ApiPaths.DOGS, request));

        assertThat(error.message()).isEqualTo("The request failed validation");
        assertThat(error.details()).extracting(ApiError.FieldError::field)
                .containsExactly("gender", "name");
        assertThat(error.path()).isEqualTo(ApiPaths.DOGS);
    }

    @Test
    @DisplayName("reports a leaving date with no reason as the rule it broke")
    void reportsCrossFieldFailures() {
        DogRequest request = DogRequestBuilder.aDog(supplierId, inServiceId)
                .leavingDate(LocalDate.of(2025, 3, 31))
                .build();

        ApiError error = failure(HttpStatus.BAD_REQUEST, HttpRequest.POST(ApiPaths.DOGS, request));

        assertThat(error.message()).isEqualTo("leavingReasonId is required when leavingDate is set");
    }

    @Test
    @DisplayName("reports a supplier that is not on the register")
    void reportsAnUnknownSupplier() {
        ApiError error = failure(HttpStatus.NOT_FOUND, HttpRequest.POST(ApiPaths.DOGS,
                DogRequestBuilder.aDog(9999L, inServiceId).build()));

        assertThat(error.message()).isEqualTo("Supplier 9999 does not exist");
    }

    @Test
    @DisplayName("refuses to source a new dog from a supplier that has been deleted")
    void refusesADeletedSupplier() {
        client.exchange(HttpRequest.DELETE(ApiPaths.SUPPLIERS + "/" + supplierId));

        ApiError error = failure(HttpStatus.CONFLICT, HttpRequest.POST(ApiPaths.DOGS,
                DogRequestBuilder.aDog(supplierId, inServiceId).build()));

        assertThat(error.message()).contains("Supplier " + supplierId + " has been deleted");
    }

    @Test
    @DisplayName("refuses to give a dog a status the force has retired")
    void refusesARetiredStatus() {
        Long retired = statusId("RETIRED");
        client.exchange(HttpRequest.DELETE(ApiPaths.STATUSES + "/" + retired));

        ApiError error = failure(HttpStatus.CONFLICT, HttpRequest.POST(ApiPaths.DOGS,
                DogRequestBuilder.aDog(supplierId, retired).build()));

        assertThat(error.message()).contains("Dog status " + retired + " has been deleted");
    }

    @Test
    @DisplayName("reports an identifier that was never issued")
    void reportsAnUnknownDog() {
        ApiError error = failure(HttpStatus.NOT_FOUND, HttpRequest.GET(ApiPaths.DOGS + "/9999"));

        assertThat(error.message()).isEqualTo("Dog 9999 does not exist");
        assertThat(error.status()).isEqualTo(404);
        assertThat(error.error()).isEqualTo("Not Found");
        assertThat(error.timestamp()).isNotNull();
    }

    private String etagOf(Long id) {
        return client.exchange(HttpRequest.GET(ApiPaths.DOGS + "/" + id), DogResponse.class)
                .getHeaders().get(HttpHeaders.ETAG);
    }

    private Long create(DogRequest request) {
        return client.retrieve(HttpRequest.POST(ApiPaths.DOGS, request), DogResponse.class).id();
    }

    @SuppressWarnings("unchecked")
    private PagedResponse<DogResponse> listDogs(String query) {
        return client.retrieve(HttpRequest.GET(ApiPaths.DOGS + query),
                Argument.of(PagedResponse.class, DogResponse.class));
    }

    @SuppressWarnings("unchecked")
    private PagedResponse<DogResponse> filter(String filter) {
        return client.retrieve(
                HttpRequest.GET(UriBuilder.of(ApiPaths.DOGS).queryParam("filter", filter).build()),
                Argument.of(PagedResponse.class, DogResponse.class));
    }

    /**
     * Runs a request expected to fail and returns the error body, so a test can say what the API
     * should have answered rather than how the client reports it.
     */
    private ApiError failure(HttpStatus expected, HttpRequest<?> request) {
        try {
            client.exchange(request, Argument.STRING);
            throw new AssertionError("Expected the request to fail with " + expected);
        } catch (HttpClientResponseException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(expected.getCode());
            return e.getResponse().getBody(ApiError.class).orElseThrow();
        }
    }

    private Long supplier(String name) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        return supplierRepository.save(supplier).getId();
    }

    private Long statusId(String code) {
        return dogStatusRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(code).orElseThrow().getId();
    }

    private Long leavingReasonId(String code) {
        return leavingReasonRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(code).orElseThrow().getId();
    }
}
