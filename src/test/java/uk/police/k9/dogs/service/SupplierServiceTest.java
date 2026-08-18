package uk.police.k9.dogs.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.police.k9.dogs.dto.SupplierRequest;
import uk.police.k9.dogs.dto.SupplierResponse;
import uk.police.k9.dogs.entity.Supplier;
import uk.police.k9.dogs.exception.ResourceConflictException;
import uk.police.k9.dogs.mapper.SupplierMapperImpl;
import uk.police.k9.dogs.repository.SupplierRepository;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("The suppliers dogs come from")
class SupplierServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T09:15:30Z");
    private static final long SUPPLIER_ID = 5L;

    @Mock
    private SupplierRepository repository;

    private SupplierService service() {
        return new SupplierService(repository, new SupplierMapperImpl(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("records a new breeder or kennels")
    void createsASupplier() {
        when(repository.findByNameIgnoreCaseAndDeletedAtIsNull("Ravenscroft Working Dogs"))
                .thenReturn(Optional.empty());
        when(repository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse response = service().create(new SupplierRequest("Ravenscroft Working Dogs",
                "Marie Ravenscroft", "kennels@ravenscroft.example", "01592 555 210", null));

        assertThat(response.name()).isEqualTo("Ravenscroft Working Dogs");
        assertThat(response.contactEmail()).isEqualTo("kennels@ravenscroft.example");
        assertThat(response.deleted()).isFalse();
    }

    @Test
    @DisplayName("refuses a name another active supplier already trades under")
    void rejectsDuplicateName() {
        when(repository.findByNameIgnoreCaseAndDeletedAtIsNull("Ravenscroft Working Dogs"))
                .thenReturn(Optional.of(supplier()));

        assertThatThrownBy(() -> service().create(
                new SupplierRequest("Ravenscroft Working Dogs", null, null, null, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Supplier with name 'Ravenscroft Working Dogs' already exists");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("marks a supplier as deleted rather than removing it, so its dogs still read correctly")
    void softDeletesASupplier() {
        Supplier existing = supplier();
        when(repository.findById(SUPPLIER_ID)).thenReturn(Optional.of(existing));

        service().delete(SUPPLIER_ID);

        assertThat(existing.getDeletedAt()).isEqualTo(NOW);
        verify(repository).update(existing);
    }

    @Test
    @DisplayName("refuses to edit a supplier that has been deleted")
    void rejectsEditingADeletedSupplier() {
        Supplier existing = supplier();
        existing.setDeletedAt(NOW);
        when(repository.findById(SUPPLIER_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().update(SUPPLIER_ID,
                new SupplierRequest("Ravenscroft Working Dogs", null, null, null, null), null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Supplier 5 has been deleted");
    }

    private static Supplier supplier() {
        Supplier supplier = new Supplier();
        supplier.setId(SUPPLIER_ID);
        supplier.setName("Ravenscroft Working Dogs");
        return supplier;
    }
}
