package uk.police.k9.dogs.service;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.transaction.annotation.ReadOnly;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.dto.SupplierRequest;
import uk.police.k9.dogs.dto.SupplierResponse;
import uk.police.k9.dogs.entity.Supplier;
import uk.police.k9.dogs.exception.ResourceConflictException;
import uk.police.k9.dogs.exception.ResourceNotFoundException;
import uk.police.k9.dogs.mapper.SupplierMapper;
import uk.police.k9.dogs.repository.SupplierRepository;
import uk.police.k9.dogs.repository.spec.AuditedSpecifications;

import java.time.Clock;
import java.util.Objects;
import java.util.Set;

/**
 * The breeders and kennels the force takes dogs from. Deleting one retires it: dogs already
 * sourced from it keep pointing at it, but no new dog can be assigned to it.
 */
@Singleton
public class SupplierService {

    private static final String RESOURCE = "Supplier";
    private static final Sort DEFAULT_SORT = Sort.of(Sort.Order.asc("name"), Sort.Order.asc("id"));

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final Clock clock;

    public SupplierService(SupplierRepository supplierRepository,
                           SupplierMapper supplierMapper,
                           Clock clock) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
        this.clock = clock;
    }

    @ReadOnly
    public PagedResponse<SupplierResponse> list(boolean includeDeleted, Pageable pageable) {
        PredicateSpecification<Supplier> specification =
                includeDeleted ? null : AuditedSpecifications.notDeleted();
        return PagedResponse.from(
                supplierRepository.findAll(specification, Pagination.normalise(pageable, DEFAULT_SORT)),
                supplierMapper::toResponse);
    }

    @ReadOnly
    public SupplierResponse get(Long id) {
        return supplierMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        requireNameAvailable(request.name(), null);
        Supplier supplier = supplierMapper.toEntity(request);
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request,
                                   @Nullable Set<Long> expectedVersions) {
        Supplier supplier = findOrThrow(id);
        Updates.requireEditable(supplier, expectedVersions, RESOURCE);
        requireNameAvailable(request.name(), id);
        supplierMapper.applyTo(supplier, request);
        return supplierMapper.toResponse(supplierRepository.update(supplier));
    }

    /** Marks the supplier as deleted. Repeating the request changes nothing. */
    @Transactional
    public void delete(Long id) {
        Supplier supplier = findOrThrow(id);
        if (supplier.isDeleted()) {
            return;
        }
        supplier.setDeletedAt(clock.instant());
        supplierRepository.update(supplier);
    }

    private Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }

    private void requireNameAvailable(String name, Long currentSupplierId) {
        supplierRepository.findByNameIgnoreCaseAndDeletedAtIsNull(name)
                .filter(existing -> !Objects.equals(existing.getId(), currentSupplierId))
                .ifPresent(existing -> {
                    throw ResourceConflictException.duplicate(RESOURCE, "name", name);
                });
    }
}
