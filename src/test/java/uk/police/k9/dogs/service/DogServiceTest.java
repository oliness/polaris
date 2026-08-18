package uk.police.k9.dogs.service;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.police.k9.dogs.dto.DogFilter;
import uk.police.k9.dogs.dto.DogResponse;
import uk.police.k9.dogs.entity.Dog;
import uk.police.k9.dogs.entity.DogStatus;
import uk.police.k9.dogs.entity.LeavingReason;
import uk.police.k9.dogs.entity.Supplier;
import uk.police.k9.dogs.exception.ResourceConflictException;
import uk.police.k9.dogs.exception.ResourceNotFoundException;
import uk.police.k9.dogs.mapper.DogMapper;
import uk.police.k9.dogs.mapper.DogMapperImpl;
import uk.police.k9.dogs.mapper.ReferenceDataMapperImpl;
import uk.police.k9.dogs.mapper.SupplierMapperImpl;
import uk.police.k9.dogs.repository.DogRepository;
import uk.police.k9.dogs.repository.DogStatusRepository;
import uk.police.k9.dogs.repository.LeavingReasonRepository;
import uk.police.k9.dogs.repository.SupplierRepository;
import uk.police.k9.dogs.support.DogRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules the register enforces, tested without a database or a server. The mappers are the real
 * generated ones, so the assertions are made against the response a caller would receive.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("The register of dogs")
class DogServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T09:15:30Z");
    private static final long SUPPLIER_ID = 1L;
    private static final long STATUS_ID = 2L;
    private static final long LEAVING_REASON_ID = 3L;
    private static final long DOG_ID = 4L;

    @Mock
    private DogRepository dogRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private DogStatusRepository dogStatusRepository;
    @Mock
    private LeavingReasonRepository leavingReasonRepository;
    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;
    @Captor
    private ArgumentCaptor<PredicateSpecification<Dog>> specificationCaptor;

    private final DogMapper dogMapper =
            new DogMapperImpl(new SupplierMapperImpl(), new ReferenceDataMapperImpl());

    private DogService service() {
        return new DogService(dogRepository, supplierRepository, dogStatusRepository,
                leavingReasonRepository, dogMapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("when a dog is registered")
    class Create {

        @Test
        @DisplayName("stores the details against the supplier and status that were named")
        void savesTheDog() {
            givenSupplierExists();
            givenStatusExists();
            when(dogRepository.save(any(Dog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DogResponse response = service().create(DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID)
                    .name("Baxter")
                    .badgeId("K9-1041")
                    .build());

            assertThat(response.name()).isEqualTo("Baxter");
            assertThat(response.supplier().name()).isEqualTo("Ravenscroft Working Dogs");
            assertThat(response.status().code()).isEqualTo("IN_SERVICE");
            assertThat(response.deleted()).isFalse();
            assertThat(response.leavingReason()).isNull();
        }

        @Test
        @DisplayName("is refused when the supplier is not on the register")
        void rejectsUnknownSupplier() {
            when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().create(DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID).build()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Supplier 1 does not exist");

            verify(dogRepository, never()).save(any());
        }

        @Test
        @DisplayName("is refused when the supplier has been deleted, as no new dog can come from it")
        void rejectsDeletedSupplier() {
            Supplier supplier = supplier();
            supplier.setDeletedAt(NOW);
            when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));

            assertThatThrownBy(() -> service().create(DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID).build()))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("Supplier 1 has been deleted");
        }

        @Test
        @DisplayName("is refused when the status has been retired")
        void rejectsDeletedStatus() {
            givenSupplierExists();
            DogStatus status = status();
            status.setDeletedAt(NOW);
            when(dogStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(status));

            assertThatThrownBy(() -> service().create(DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID).build()))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("Dog status 2 has been deleted");
        }

        @Test
        @DisplayName("is refused when another serving dog already carries the badge")
        void rejectsDuplicateBadge() {
            givenSupplierExists();
            givenStatusExists();
            Dog other = new Dog();
            other.setId(99L);
            when(dogRepository.findByBadgeIdIgnoreCaseAndDeletedAtIsNull("K9-1041"))
                    .thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service().create(
                    DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID).badgeId("K9-1041").build()))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessage("Dog with badgeId 'K9-1041' already exists");

            verify(dogRepository, never()).save(any());
        }

        @Test
        @DisplayName("records why a dog has already left, when it is registered after the fact")
        void resolvesLeavingReason() {
            givenSupplierExists();
            givenStatusExists();
            when(leavingReasonRepository.findById(LEAVING_REASON_ID)).thenReturn(Optional.of(leavingReason()));
            when(dogRepository.save(any(Dog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DogResponse response = service().create(DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID)
                    .left(LocalDate.of(2025, 3, 31), LEAVING_REASON_ID)
                    .build());

            assertThat(response.leavingDate()).isEqualTo(LocalDate.of(2025, 3, 31));
            assertThat(response.leavingReason().code()).isEqualTo("RETIRED_REHOUSED");
        }
    }

    @Nested
    @DisplayName("when a dog is updated")
    class Update {

        @Test
        @DisplayName("replaces the details it was given")
        void appliesTheChanges() {
            Dog existing = dog();
            when(dogRepository.findById(DOG_ID)).thenReturn(Optional.of(existing));
            givenSupplierExists();
            givenStatusExists();
            when(dogRepository.update(any(Dog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DogResponse response = service().update(DOG_ID, DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID)
                    .name("Baxter II")
                    .kennellingCharacteristic("Nervous around traffic")
                    .build(), null);

            assertThat(response.name()).isEqualTo("Baxter II");
            assertThat(response.kennellingCharacteristic()).isEqualTo("Nervous around traffic");
        }

        @Test
        @DisplayName("leaves a dog its own badge")
        void allowsADogToKeepItsBadge() {
            Dog existing = dog();
            existing.setBadgeId("K9-1041");
            when(dogRepository.findById(DOG_ID)).thenReturn(Optional.of(existing));
            givenSupplierExists();
            givenStatusExists();
            when(dogRepository.findByBadgeIdIgnoreCaseAndDeletedAtIsNull("K9-1041"))
                    .thenReturn(Optional.of(existing));
            when(dogRepository.update(any(Dog.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DogResponse response = service().update(DOG_ID,
                    DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID).badgeId("K9-1041").build(), null);

            assertThat(response.badgeId()).isEqualTo("K9-1041");
        }

        @Test
        @DisplayName("is refused for a dog that has been deleted, which is kept only for audit")
        void rejectsDeletedDog() {
            Dog existing = dog();
            existing.setDeletedAt(NOW);
            when(dogRepository.findById(DOG_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service().update(DOG_ID,
                    DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID).build(), null))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("Dog 4 has been deleted");

            verify(dogRepository, never()).update(any(Dog.class));
        }

        @Test
        @DisplayName("is refused when no dog has that identifier")
        void rejectsUnknownDog() {
            when(dogRepository.findById(DOG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().update(DOG_ID,
                    DogRequestBuilder.aDog(SUPPLIER_ID, STATUS_ID).build(), null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Dog 4 does not exist");
        }
    }

    @Nested
    @DisplayName("when a dog is deleted")
    class Delete {

        @Test
        @DisplayName("marks the record rather than removing it, so the audit trail survives")
        void stampsTheRecord() {
            Dog existing = dog();
            when(dogRepository.findById(DOG_ID)).thenReturn(Optional.of(existing));

            service().delete(DOG_ID);

            assertThat(existing.getDeletedAt()).isEqualTo(NOW);
            verify(dogRepository).update(existing);
        }

        @Test
        @DisplayName("changes nothing the second time, so repeating the request is safe")
        void isIdempotent() {
            Dog existing = dog();
            existing.setDeletedAt(Instant.parse("2026-01-01T00:00:00Z"));
            when(dogRepository.findById(DOG_ID)).thenReturn(Optional.of(existing));

            service().delete(DOG_ID);

            assertThat(existing.getDeletedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
            verify(dogRepository, never()).update(any(Dog.class));
        }

        @Test
        @DisplayName("is refused when no dog has that identifier")
        void rejectsUnknownDog() {
            when(dogRepository.findById(DOG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().delete(DOG_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("when dogs are listed")
    class Listing {

        @Test
        @DisplayName("leaves out the deleted ones unless they are asked for")
        void excludesDeletedByDefault() {
            when(dogRepository.findAll(specificationCaptor.capture(), any(Pageable.class)))
                    .thenReturn(Page.of(List.of(), Pageable.from(0, 20), 0L));

            service().list(DogFilter.empty(), false, Pageable.from(0, 20));

            assertThat(specificationCaptor.getValue())
                    .as("a criteria restricting the query to records that are not deleted")
                    .isNotNull();
        }

        @Test
        @DisplayName("applies no criteria at all when the deleted ones are wanted too")
        void includesDeletedWhenAsked() {
            when(dogRepository.findAll(specificationCaptor.capture(), any(Pageable.class)))
                    .thenReturn(Page.of(List.of(), Pageable.from(0, 20), 0L));

            service().list(DogFilter.empty(), true, Pageable.from(0, 20));

            assertThat(specificationCaptor.getValue()).isNull();
        }

        @Test
        @DisplayName("orders by name when the caller did not choose, so pages do not overlap")
        void appliesADefaultOrder() {
            when(dogRepository.findAll(anySpecification(), pageableCaptor.capture()))
                    .thenReturn(Page.of(List.of(), Pageable.from(0, 20), 0L));

            service().list(DogFilter.empty(), false, Pageable.from(0, 20));

            Pageable used = pageableCaptor.getValue();
            assertThat(used.isSorted()).isTrue();
            assertThat(used.getSort().getOrderBy())
                    .extracting(order -> order.getProperty())
                    .containsExactly("name", "id");
            assertThat(used.requestTotal())
                    .as("the total is needed to report totalElements")
                    .isTrue();
        }

        @Test
        @DisplayName("keeps the order the caller asked for")
        void keepsTheCallersOrder() {
            when(dogRepository.findAll(anySpecification(), pageableCaptor.capture()))
                    .thenReturn(Page.of(List.of(), Pageable.from(0, 20), 0L));

            service().list(DogFilter.empty(), false,
                    Pageable.from(0, 20, Sort.of(Sort.Order.desc("breed"))));

            assertThat(pageableCaptor.getValue().getSort().getOrderBy())
                    .extracting(order -> order.getProperty())
                    .containsExactly("breed");
        }
    }

    @Test
    @DisplayName("still hands over a deleted dog by identifier, flagged as deleted, for audit")
    void getReturnsDeletedDog() {
        Dog existing = dog();
        existing.setDeletedAt(NOW);
        when(dogRepository.findById(DOG_ID)).thenReturn(Optional.of(existing));

        DogResponse response = service().get(DOG_ID);

        assertThat(response.deleted()).isTrue();
        assertThat(response.deletedAt()).isEqualTo(NOW);
    }

    /**
     * Matches any search criteria, saying which kind it is.
     *
     * <p>Micronaut Data overloads {@code findAll}: a bare {@code any()} matches both
     * {@code (PredicateSpecification, Pageable)}, which returns a page, and
     * {@code (QuerySpecification, Sort)}, which returns a list - {@code Pageable} is a
     * {@code Sort}, so neither is the more specific. Naming the type picks the paged one.
     */
    private static PredicateSpecification<Dog> anySpecification() {
        return ArgumentMatchers.any();
    }

    private void givenSupplierExists() {
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier()));
    }

    private void givenStatusExists() {
        when(dogStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(status()));
    }

    private static Supplier supplier() {
        Supplier supplier = new Supplier();
        supplier.setId(SUPPLIER_ID);
        supplier.setName("Ravenscroft Working Dogs");
        return supplier;
    }

    private static DogStatus status() {
        DogStatus status = new DogStatus();
        status.setId(STATUS_ID);
        status.setCode("IN_SERVICE");
        status.setLabel("In Service");
        return status;
    }

    private static LeavingReason leavingReason() {
        LeavingReason reason = new LeavingReason();
        reason.setId(LEAVING_REASON_ID);
        reason.setCode("RETIRED_REHOUSED");
        reason.setLabel("Retired (Re-housed)");
        return reason;
    }

    private static Dog dog() {
        Dog dog = new Dog();
        dog.setId(DOG_ID);
        dog.setName("Baxter");
        dog.setBreed("German Shepherd");
        dog.setSupplier(supplier());
        dog.setStatus(status());
        dog.setBirthDate(LocalDate.of(2020, 3, 14));
        dog.setDateAcquired(LocalDate.of(2021, 1, 6));
        return dog;
    }
}
