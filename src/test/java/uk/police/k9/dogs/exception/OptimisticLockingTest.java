package uk.police.k9.dogs.exception;

import io.micronaut.context.annotation.Property;
import io.micronaut.data.exceptions.OptimisticLockException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.police.k9.dogs.entity.Dog;
import uk.police.k9.dogs.entity.DogStatus;
import uk.police.k9.dogs.entity.Gender;
import uk.police.k9.dogs.entity.Supplier;
import uk.police.k9.dogs.repository.DogRepository;
import uk.police.k9.dogs.repository.DogStatusRepository;
import uk.police.k9.dogs.repository.SupplierRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The database's own guard against two people editing one record at once. A caller who sends back
 * the version it read is refused earlier, by the service; this is the race that gets past that,
 * which needs two transactions interleaved and so is driven through the repository, not HTTP.
 */
@MicronautTest(transactional = false)
@Property(name = "datasources.default.url", value = "jdbc:h2:mem:locking;DB_CLOSE_DELAY=-1")
@DisplayName("Two people editing the same record")
class OptimisticLockingTest {

    @Inject
    DogRepository dogRepository;
    @Inject
    SupplierRepository supplierRepository;
    @Inject
    DogStatusRepository dogStatusRepository;
    @Inject
    OptimisticLockExceptionHandler handler;

    @Test
    @DisplayName("is refused for whoever writes second, rather than losing the first change")
    void staleWriteIsRejected() {
        Dog ours = dogRepository.save(dog());

        // Someone else reads the same dog and saves their change first.
        Dog theirs = dogRepository.findById(ours.getId()).orElseThrow();
        theirs.setName("Nala");
        dogRepository.update(theirs);

        // Our copy still carries the version we read, which is no longer the current one.
        ours.setName("Rufus");

        assertThatThrownBy(() -> dogRepository.update(ours))
                .isInstanceOf(OptimisticLockException.class);

        assertThat(dogRepository.findById(ours.getId()))
                .as("the first change survives; the second never landed")
                .hasValueSatisfying(dog -> {
                    assertThat(dog.getName()).isEqualTo("Nala");
                    assertThat(dog.getVersion()).isEqualTo(1L);
                });
    }

    @Test
    @DisplayName("is reported as 409, telling the caller to fetch the record again")
    void isReportedAsConflict() {
        HttpResponse<?> response = handler.handle(
                HttpRequest.PUT("/api/dogs/dogs/1", ""),
                new OptimisticLockException("Execute update returned unexpected row count"));

        assertThat(response.code()).isEqualTo(HttpStatus.CONFLICT.getCode());
        assertThat(response.getBody(ApiError.class)).hasValueSatisfying(error -> {
            assertThat(error.status()).isEqualTo(409);
            assertThat(error.error()).isEqualTo("Conflict");
            assertThat(error.message()).isEqualTo(
                    "The record was changed by someone else. Fetch it again and re-apply the change.");
            assertThat(error.path()).isEqualTo("/api/dogs/dogs/1");
        });
    }

    private Dog dog() {
        Supplier supplier = new Supplier();
        supplier.setName("Ravenscroft Working Dogs");

        DogStatus status = dogStatusRepository
                .findByCodeIgnoreCaseAndDeletedAtIsNull("IN_SERVICE").orElseThrow();

        Dog dog = new Dog();
        dog.setName("Baxter");
        dog.setBreed("German Shepherd");
        dog.setSupplier(supplierRepository.save(supplier));
        dog.setStatus(status);
        dog.setGender(Gender.MALE);
        dog.setBirthDate(LocalDate.of(2020, 3, 14));
        dog.setDateAcquired(LocalDate.of(2021, 1, 6));
        return dog;
    }
}
