package uk.police.k9.dogs.service;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.transaction.annotation.ReadOnly;
import io.micronaut.transaction.annotation.Transactional;
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.dto.ReferenceDataRequest;
import uk.police.k9.dogs.dto.ReferenceDataResponse;
import uk.police.k9.dogs.entity.ReferenceDataEntity;
import uk.police.k9.dogs.exception.ResourceConflictException;
import uk.police.k9.dogs.exception.ResourceNotFoundException;
import uk.police.k9.dogs.mapper.ReferenceDataMapper;
import uk.police.k9.dogs.repository.ReferenceDataRepository;
import uk.police.k9.dogs.repository.spec.AuditedSpecifications;

import java.time.Clock;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The behaviour every maintainable lookup needs. Dog statuses and leaving reasons are the same
 * thing over different rows, so the rules live here once and the subclasses supply only the
 * repository, an entity factory and the name to use in error messages.
 *
 * <p>Deleting a value retires it: dogs that already hold it keep it - which is what makes the
 * history readable years later - but it can no longer be assigned.
 */
public abstract class ReferenceDataService<E extends ReferenceDataEntity> {

    private static final Sort DEFAULT_SORT =
            Sort.of(Sort.Order.asc("displayOrder"), Sort.Order.asc("label"));

    private final ReferenceDataRepository<E> repository;
    private final ReferenceDataMapper mapper;
    private final Supplier<E> entityFactory;
    private final String resourceName;
    private final Clock clock;

    protected ReferenceDataService(ReferenceDataRepository<E> repository,
                                   ReferenceDataMapper mapper,
                                   Supplier<E> entityFactory,
                                   String resourceName,
                                   Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityFactory = entityFactory;
        this.resourceName = resourceName;
        this.clock = clock;
    }

    @ReadOnly
    public PagedResponse<ReferenceDataResponse> list(boolean includeDeleted, Pageable pageable) {
        PredicateSpecification<E> specification =
                includeDeleted ? null : AuditedSpecifications.notDeleted();
        return PagedResponse.from(
                repository.findAll(specification, Pagination.normalise(pageable, DEFAULT_SORT)),
                mapper::toResponse);
    }

    @ReadOnly
    public ReferenceDataResponse get(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public ReferenceDataResponse create(ReferenceDataRequest request) {
        requireCodeAvailable(request.code(), null);
        E entity = entityFactory.get();
        mapper.applyTo(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public ReferenceDataResponse update(Long id, ReferenceDataRequest request,
                                        @Nullable Set<Long> expectedVersions) {
        E entity = findOrThrow(id);
        Updates.requireEditable(entity, expectedVersions, resourceName);
        requireCodeAvailable(request.code(), id);
        mapper.applyTo(entity, request);
        return mapper.toResponse(repository.update(entity));
    }

    /** Retires the value. Repeating the request changes nothing. */
    @Transactional
    public void delete(Long id) {
        E entity = findOrThrow(id);
        if (entity.isDeleted()) {
            return;
        }
        entity.setDeletedAt(clock.instant());
        repository.update(entity);
    }

    private E findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(resourceName, id));
    }

    private void requireCodeAvailable(String code, Long currentId) {
        repository.findByCodeIgnoreCaseAndDeletedAtIsNull(code)
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw ResourceConflictException.duplicate(resourceName, "code", code);
                });
    }
}
