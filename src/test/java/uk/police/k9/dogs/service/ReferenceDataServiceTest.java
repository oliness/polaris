package uk.police.k9.dogs.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.police.k9.dogs.dto.ReferenceDataRequest;
import uk.police.k9.dogs.dto.ReferenceDataResponse;
import uk.police.k9.dogs.entity.DogStatus;
import uk.police.k9.dogs.exception.ResourceConflictException;
import uk.police.k9.dogs.exception.ResourceNotFoundException;
import uk.police.k9.dogs.mapper.ReferenceDataMapperImpl;
import uk.police.k9.dogs.repository.DogStatusRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The rules shared by the maintainable lookups, exercised through the dog statuses. */
@ExtendWith(MockitoExtension.class)
@DisplayName("A maintainable lookup")
class ReferenceDataServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T09:15:30Z");
    private static final long STATUS_ID = 7L;

    @Mock
    private DogStatusRepository repository;

    private DogStatusService service() {
        return new DogStatusService(repository, new ReferenceDataMapperImpl(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("adds a value the force did not have before")
    void createsAValue() {
        when(repository.findByCodeIgnoreCaseAndDeletedAtIsNull("STAND_DOWN")).thenReturn(Optional.empty());
        when(repository.save(any(DogStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReferenceDataResponse response = service().create(new ReferenceDataRequest(
                "STAND_DOWN", "Stand Down", "Temporarily withdrawn from duty.", 25));

        assertThat(response.code()).isEqualTo("STAND_DOWN");
        assertThat(response.label()).isEqualTo("Stand Down");
        assertThat(response.displayOrder()).isEqualTo(25);
        assertThat(response.deleted()).isFalse();
    }

    @Test
    @DisplayName("sorts last when no position was given")
    void defaultsTheDisplayOrder() {
        when(repository.findByCodeIgnoreCaseAndDeletedAtIsNull("STAND_DOWN")).thenReturn(Optional.empty());
        when(repository.save(any(DogStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReferenceDataResponse response = service().create(
                new ReferenceDataRequest("STAND_DOWN", "Stand Down", null, null));

        assertThat(response.displayOrder()).isZero();
    }

    @Test
    @DisplayName("refuses a code another active value already uses")
    void rejectsDuplicateCode() {
        DogStatus existing = status();
        when(repository.findByCodeIgnoreCaseAndDeletedAtIsNull("IN_SERVICE")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().create(
                new ReferenceDataRequest("IN_SERVICE", "In Service", null, 20)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Dog status with code 'IN_SERVICE' already exists");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("lets a value keep its own code when it is edited")
    void allowsAValueToKeepItsCode() {
        DogStatus existing = status();
        when(repository.findById(STATUS_ID)).thenReturn(Optional.of(existing));
        when(repository.findByCodeIgnoreCaseAndDeletedAtIsNull("IN_SERVICE")).thenReturn(Optional.of(existing));
        when(repository.update(any(DogStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReferenceDataResponse response = service().update(STATUS_ID,
                new ReferenceDataRequest("IN_SERVICE", "Operational", null, 20), null);

        assertThat(response.label()).isEqualTo("Operational");
    }

    @Test
    @DisplayName("refuses to edit a value that has been retired")
    void rejectsEditingARetiredValue() {
        DogStatus existing = status();
        existing.setDeletedAt(NOW);
        when(repository.findById(STATUS_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().update(STATUS_ID,
                new ReferenceDataRequest("IN_SERVICE", "In Service", null, 20), null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Dog status 7 has been deleted");
    }

    @Test
    @DisplayName("retires a value by marking it, so dogs that hold it still read correctly")
    void retiresAValue() {
        DogStatus existing = status();
        when(repository.findById(STATUS_ID)).thenReturn(Optional.of(existing));

        service().delete(STATUS_ID);

        assertThat(existing.getDeletedAt()).isEqualTo(NOW);
        verify(repository).update(existing);
    }

    @Test
    @DisplayName("is unchanged by retiring it twice")
    void deleteIsIdempotent() {
        DogStatus existing = status();
        existing.setDeletedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(repository.findById(STATUS_ID)).thenReturn(Optional.of(existing));

        service().delete(STATUS_ID);

        verify(repository, never()).update(any(DogStatus.class));
    }

    @Test
    @DisplayName("reports an identifier that was never issued")
    void rejectsUnknownValue() {
        when(repository.findById(STATUS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(STATUS_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Dog status 7 does not exist");
    }

    private static DogStatus status() {
        DogStatus status = new DogStatus();
        status.setId(STATUS_ID);
        status.setCode("IN_SERVICE");
        status.setLabel("In Service");
        status.setDisplayOrder(20);
        return status;
    }
}
