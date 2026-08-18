package uk.police.k9.dogs.controller;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.police.k9.dogs.dto.DogResponse;
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.entity.Supplier;
import uk.police.k9.dogs.repository.DogRepository;
import uk.police.k9.dogs.repository.DogStatusRepository;
import uk.police.k9.dogs.repository.SupplierRepository;
import uk.police.k9.dogs.support.DogRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * The server root. It is not part of the API - every endpoint of that sits under
 * {@code /api/dogs} - but someone who opens the address the server logs on start-up is shown the
 * register rather than a 404.
 */
@MicronautTest(transactional = false)
@Property(name = "datasources.default.url", value = "jdbc:h2:mem:dogs-root;DB_CLOSE_DELAY=-1")
@DisplayName("GET /")
class RootApiTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    EmbeddedServer server;

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
    @DisplayName("lists the register to a caller who asks for the root")
    void listsTheRegister() {
        registerDog("Baxter");

        PagedResponse<DogResponse> page = client.retrieve(HttpRequest.GET("/"),
                Argument.of(PagedResponse.class, DogResponse.class));

        assertThat(page.content()).extracting(DogResponse::name).containsExactly("Baxter");
    }

    @Test
    @DisplayName("gets there by pointing at the dogs list, so the register keeps one address")
    void redirectsRatherThanServingASecondCopy() {
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
        configuration.setFollowRedirects(false);

        try (HttpClient direct = HttpClient.create(server.getURL(), configuration)) {
            HttpResponse<String> response =
                    direct.toBlocking().exchange(HttpRequest.GET("/"), String.class);

            assertThat(response.code()).isEqualTo(HttpStatus.SEE_OTHER.getCode());
            assertThat(response.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo(ApiPaths.DOGS);
        }
    }

    @Test
    @DisplayName("leaves an unknown path alone rather than claiming everything below the root")
    void doesNotClaimEveryPath() {
        assertThatExceptionOfType(HttpClientResponseException.class)
                .isThrownBy(() -> client.exchange(HttpRequest.GET("/dogs")))
                .satisfies(e -> assertThat(e.getStatus().getCode())
                        .isEqualTo(HttpStatus.NOT_FOUND.getCode()));
    }

    private void registerDog(String name) {
        Supplier supplier = new Supplier();
        supplier.setName("Ravenscroft Working Dogs");
        Long supplierId = supplierRepository.save(supplier).getId();
        Long statusId = dogStatusRepository
                .findByCodeIgnoreCaseAndDeletedAtIsNull("IN_SERVICE").orElseThrow().getId();

        client.exchange(HttpRequest.POST(ApiPaths.DOGS,
                DogRequestBuilder.aDog(supplierId, statusId).name(name).build()));
    }
}
