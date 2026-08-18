package uk.police.k9.dogs.service;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.transaction.annotation.ReadOnly;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import uk.police.k9.dogs.dto.DogFilter;
import uk.police.k9.dogs.dto.DogRequest;
import uk.police.k9.dogs.dto.DogResponse;
import uk.police.k9.dogs.dto.PagedResponse;
import uk.police.k9.dogs.entity.Dog;
import uk.police.k9.dogs.entity.DogStatus;
import uk.police.k9.dogs.entity.LeavingReason;
import uk.police.k9.dogs.entity.Supplier;
import uk.police.k9.dogs.exception.ResourceConflictException;
import uk.police.k9.dogs.exception.ResourceNotFoundException;
import uk.police.k9.dogs.mapper.DogMapper;
import uk.police.k9.dogs.mapper.DogReferences;
import uk.police.k9.dogs.repository.DogRepository;
import uk.police.k9.dogs.repository.DogStatusRepository;
import uk.police.k9.dogs.repository.LeavingReasonRepository;
import uk.police.k9.dogs.repository.SupplierRepository;
import uk.police.k9.dogs.repository.spec.AuditedSpecifications;
import uk.police.k9.dogs.repository.spec.DogSpecifications;

import java.time.Clock;
import java.util.Objects;
import java.util.Set;

/**
 * The register of dogs, and the rules that go with it. Deleting stamps {@code deletedAt} rather
 * than issuing a {@code DELETE}, so the record survives for audit.
 */
@Singleton
public class DogService {

    private static final String RESOURCE = "Dog";
    private static final Sort DEFAULT_SORT = Sort.of(Sort.Order.asc("name"), Sort.Order.asc("id"));

    private final DogRepository dogRepository;
    private final SupplierRepository supplierRepository;
    private final DogStatusRepository dogStatusRepository;
    private final LeavingReasonRepository leavingReasonRepository;
    private final DogMapper dogMapper;
    private final Clock clock;

    public DogService(DogRepository dogRepository,
                      SupplierRepository supplierRepository,
                      DogStatusRepository dogStatusRepository,
                      LeavingReasonRepository leavingReasonRepository,
                      DogMapper dogMapper,
                      Clock clock) {
        this.dogRepository = dogRepository;
        this.supplierRepository = supplierRepository;
        this.dogStatusRepository = dogStatusRepository;
        this.leavingReasonRepository = leavingReasonRepository;
        this.dogMapper = dogMapper;
        this.clock = clock;
    }

    @ReadOnly
    public PagedResponse<DogResponse> list(DogFilter filter, boolean includeDeleted, Pageable pageable) {
        PredicateSpecification<Dog> specification = DogSpecifications.matching(filter);
        if (!includeDeleted) {
            PredicateSpecification<Dog> notDeleted = AuditedSpecifications.notDeleted();
            specification = specification == null ? notDeleted : specification.and(notDeleted);
        }
        return PagedResponse.from(
                dogRepository.findAll(specification, Pagination.normalise(pageable, DEFAULT_SORT)),
                dogMapper::toResponse);
    }

    /**
     * A deleted dog is still returned here, flagged {@code deleted}: an auditor holding an
     * identifier has to be able to read the record being kept for them.
     */
    @ReadOnly
    public DogResponse get(Long id) {
        return dogMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public DogResponse create(DogRequest request) {
        DogReferences references = resolveReferences(request);
        requireBadgeAvailable(request.badgeId(), null);
        Dog dog = dogMapper.toEntity(request, references);
        return dogMapper.toResponse(dogRepository.save(dog));
    }

    @Transactional
    public DogResponse update(Long id, DogRequest request, @Nullable Set<Long> expectedVersions) {
        Dog dog = findOrThrow(id);
        Updates.requireEditable(dog, expectedVersions, RESOURCE);
        DogReferences references = resolveReferences(request);
        requireBadgeAvailable(request.badgeId(), id);
        dogMapper.applyTo(dog, request, references);
        return dogMapper.toResponse(dogRepository.update(dog));
    }

    /** Marks the dog as deleted. Repeating the request changes nothing. */
    @Transactional
    public void delete(Long id) {
        Dog dog = findOrThrow(id);
        if (dog.isDeleted()) {
            return;
        }
        dog.setDeletedAt(clock.instant());
        dogRepository.update(dog);
    }

    private Dog findOrThrow(Long id) {
        return dogRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }

    /**
     * Loads the records the request points at, refusing any the force has retired - a dog cannot
     * be sourced from a deleted supplier or given a deleted status.
     */
    private DogReferences resolveReferences(DogRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", request.supplierId()));
        requireActive(supplier.isDeleted(), "Supplier", request.supplierId());

        DogStatus status = dogStatusRepository.findById(request.statusId())
                .orElseThrow(() -> ResourceNotFoundException.of("Dog status", request.statusId()));
        requireActive(status.isDeleted(), "Dog status", request.statusId());

        LeavingReason leavingReason = null;
        if (request.leavingReasonId() != null) {
            leavingReason = leavingReasonRepository.findById(request.leavingReasonId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Leaving reason", request.leavingReasonId()));
            requireActive(leavingReason.isDeleted(), "Leaving reason", request.leavingReasonId());
        }

        return new DogReferences(supplier, status, leavingReason);
    }

    private static void requireActive(boolean deleted, String resource, Object identifier) {
        if (deleted) {
            throw ResourceConflictException.deleted(resource, identifier);
        }
    }

    /**
     * A badge may only be held by one active dog at a time. Deleted dogs keep theirs, so the audit
     * trail still shows who carried it.
     */
    private void requireBadgeAvailable(String badgeId, Long currentDogId) {
        if (badgeId == null || badgeId.isBlank()) {
            return;
        }
        dogRepository.findByBadgeIdIgnoreCaseAndDeletedAtIsNull(badgeId)
                .filter(existing -> !Objects.equals(existing.getId(), currentDogId))
                .ifPresent(existing -> {
                    throw ResourceConflictException.duplicate(RESOURCE, "badgeId", badgeId);
                });
    }
}
