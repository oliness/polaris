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
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.police.k9.dogs.dto.DogResponse;
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.dto.SupplierRequest;
import uk.police.k9.dogs.dto.SupplierResponse;
import uk.police.k9.dogs.exception.ApiError;
import uk.police.k9.dogs.repository.DogRepository;
import uk.police.k9.dogs.repository.DogStatusRepository;
import uk.police.k9.dogs.repository.SupplierRepository;
import uk.police.k9.dogs.support.DogRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The suppliers endpoint. A supplier is a record of its own because more than one dog can come from
 * the same breeder, so the interesting behaviour is what happens to those dogs when it is deleted.
 */
@MicronautTest(transactional = false)
@Property(name = "datasources.default.url", value = "jdbc:h2:mem:suppliers-api;DB_CLOSE_DELAY=-1")
@DisplayName("GET/POST/PUT/DELETE /api/dogs/suppliers")
class SupplierApiTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    DogRepository dogRepository;
    @Inject
    SupplierRepository supplierRepository;
    @Inject
    DogStatusRepository dogStatusRepository;

    private BlockingHttpClient client;

    @BeforeEach
    void setUp() {
        client = httpClient.toBlocking();
        dogRepository.deleteAll();
        supplierRepository.deleteAll();
    }

    @Test
    @DisplayName("adds a supplier and says where to find it")
    void addsASupplier() {
        HttpResponse<SupplierResponse> response = client.exchange(
                HttpRequest.POST(ApiPaths.SUPPLIERS, new SupplierRequest("Ravenscroft Working Dogs",
                        "Marie Ravenscroft", "kennels@ravenscroft.example", "01592 555 210",
                        "Ravenscroft Farm, Kinross, KY13 9XX")),
                SupplierResponse.class);

        assertThat(response.code()).isEqualTo(HttpStatus.CREATED.getCode());
        SupplierResponse supplier = response.body();
        assertThat(response.getHeaders().get(HttpHeaders.LOCATION))
                .isEqualTo(ApiPaths.SUPPLIERS + "/" + supplier.id());
        assertThat(supplier.name()).isEqualTo("Ravenscroft Working Dogs");
        assertThat(supplier.contactEmail()).isEqualTo("kennels@ravenscroft.example");
        assertThat(supplier.deleted()).isFalse();
    }

    @Test
    @DisplayName("lists suppliers by name")
    void listsSuppliers() {
        create("Northgate Kennels");
        create("Ashcombe Malinois");

        PagedResponse<SupplierResponse> page = list("");

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(SupplierResponse::name)
                .containsExactly("Ashcombe Malinois", "Northgate Kennels");
    }

    @Test
    @DisplayName("replaces a supplier's details")
    void updatesASupplier() {
        Long id = create("Northgate Kennels");

        SupplierResponse updated = client.retrieve(HttpRequest.PUT(ApiPaths.SUPPLIERS + "/" + id,
                new SupplierRequest("Northgate Kennels", "Declan Barr", "enquiries@northgate.example",
                        "0191 555 8842", null)), SupplierResponse.class);

        assertThat(updated.contactName()).isEqualTo("Declan Barr");
        assertThat(updated.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("refuses an update whose If-Match someone else has already moved on from")
    void refusesAStalePrecondition() {
        Long id = create("Northgate Kennels");
        String staleTag = client.exchange(HttpRequest.GET(ApiPaths.SUPPLIERS + "/" + id),
                SupplierResponse.class).getHeaders().get(HttpHeaders.ETAG);

        client.exchange(HttpRequest.PUT(ApiPaths.SUPPLIERS + "/" + id,
                new SupplierRequest("Northgate Kennels", "Declan Barr", null, null, null)));

        ApiError error = failure(HttpStatus.PRECONDITION_FAILED,
                HttpRequest.PUT(ApiPaths.SUPPLIERS + "/" + id,
                                new SupplierRequest("Northgate Kennels", "Someone Else", null, null, null))
                        .header(HttpHeaders.IF_MATCH, staleTag));

        assertThat(error.message()).isEqualTo("Supplier " + id
                + " has changed since you read it. Fetch it again and re-apply the change.");
        assertThat(client.retrieve(HttpRequest.GET(ApiPaths.SUPPLIERS + "/" + id),
                SupplierResponse.class).contactName())
                .as("the first change survives; the second never landed")
                .isEqualTo("Declan Barr");
    }

    @Test
    @DisplayName("refuses a name another active supplier already trades under, whatever the case")
    void refusesADuplicateName() {
        create("Northgate Kennels");

        ApiError error = failure(HttpStatus.CONFLICT, HttpRequest.POST(ApiPaths.SUPPLIERS,
                new SupplierRequest("NORTHGATE KENNELS", null, null, null, null)));

        assertThat(error.message()).isEqualTo("Supplier with name 'NORTHGATE KENNELS' already exists");
    }

    @Test
    @DisplayName("keeps a deleted supplier out of the list, and its dogs pointing at it")
    void deletingASupplierLeavesItsDogsIntact() {
        Long id = create("Northgate Kennels");
        Long statusId = dogStatusRepository.findByCodeIgnoreCaseAndDeletedAtIsNull("IN_SERVICE")
                .orElseThrow().getId();
        DogResponse dog = client.retrieve(HttpRequest.POST(ApiPaths.DOGS,
                DogRequestBuilder.aDog(id, statusId).name("Rufus").build()), DogResponse.class);

        client.exchange(HttpRequest.DELETE(ApiPaths.SUPPLIERS + "/" + id));

        assertThat(list("").content()).isEmpty();
        assertThat(list("?includeDeleted=true").content()).extracting(SupplierResponse::deleted)
                .containsExactly(true);
        DogResponse stillThere = client.retrieve(
                HttpRequest.GET(ApiPaths.DOGS + "/" + dog.id()), DogResponse.class);
        assertThat(stillThere.supplier().name()).isEqualTo("Northgate Kennels");
        assertThat(stillThere.supplier().deleted()).isTrue();
    }

    @Test
    @DisplayName("still returns a deleted supplier to anyone holding its identifier")
    void returnsADeletedSupplierById() {
        Long id = create("Northgate Kennels");
        client.exchange(HttpRequest.DELETE(ApiPaths.SUPPLIERS + "/" + id));

        SupplierResponse supplier = client.retrieve(
                HttpRequest.GET(ApiPaths.SUPPLIERS + "/" + id), SupplierResponse.class);

        assertThat(supplier.deleted()).isTrue();
        assertThat(supplierRepository.findById(id))
                .as("the row is still in the database, for audit")
                .isPresent();
    }

    @Test
    @DisplayName("reports the fields that were rejected")
    void reportsValidationFailures() {
        ApiError error = failure(HttpStatus.BAD_REQUEST, HttpRequest.POST(ApiPaths.SUPPLIERS,
                new SupplierRequest(" ", null, "not-an-email", null, null)));

        assertThat(error.details()).extracting(ApiError.FieldError::field)
                .containsExactly("contactEmail", "name");
    }

    @Test
    @DisplayName("reports an identifier that was never issued")
    void reportsAnUnknownSupplier() {
        ApiError error = failure(HttpStatus.NOT_FOUND, HttpRequest.GET(ApiPaths.SUPPLIERS + "/9999"));

        assertThat(error.message()).isEqualTo("Supplier 9999 does not exist");
    }

    private Long create(String name) {
        return client.retrieve(HttpRequest.POST(ApiPaths.SUPPLIERS,
                new SupplierRequest(name, null, null, null, null)), SupplierResponse.class).id();
    }

    @SuppressWarnings("unchecked")
    private PagedResponse<SupplierResponse> list(String query) {
        return client.retrieve(HttpRequest.GET(ApiPaths.SUPPLIERS + query),
                Argument.of(PagedResponse.class, SupplierResponse.class));
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
