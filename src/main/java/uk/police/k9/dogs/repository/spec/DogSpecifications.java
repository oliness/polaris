package uk.police.k9.dogs.repository.spec;

import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import jakarta.persistence.criteria.JoinType;
import uk.police.k9.dogs.dto.DogFilter;
import uk.police.k9.dogs.entity.Dog;

import java.util.Locale;

/**
 * The criteria behind {@code GET /api/dogs/dogs}. Each term is a separate specification, composed
 * only when supplied, so one query method serves every combination.
 */
public final class DogSpecifications {

    private static final String NAME = "name";
    private static final String BREED = "breed";
    private static final String SUPPLIER = "supplier";

    /** Escape character for LIKE, so a caller searching for "100%" is not handed a wildcard. */
    private static final char LIKE_ESCAPE = '\\';

    private DogSpecifications() {
    }

    public static PredicateSpecification<Dog> matching(DogFilter filter) {
        PredicateSpecification<Dog> specification = null;
        if (filter.hasName()) {
            specification = and(specification, nameContains(filter.name()));
        }
        if (filter.hasBreed()) {
            specification = and(specification, breedContains(filter.breed()));
        }
        if (filter.hasSupplier()) {
            specification = and(specification, supplierNameContains(filter.supplier()));
        }
        return specification;
    }

    public static PredicateSpecification<Dog> nameContains(String value) {
        return (root, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get(NAME)), containsPattern(value), LIKE_ESCAPE);
    }

    public static PredicateSpecification<Dog> breedContains(String value) {
        return (root, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get(BREED)), containsPattern(value), LIKE_ESCAPE);
    }

    public static PredicateSpecification<Dog> supplierNameContains(String value) {
        return (root, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.join(SUPPLIER, JoinType.LEFT).get(NAME)),
                containsPattern(value), LIKE_ESCAPE);
    }

    private static PredicateSpecification<Dog> and(PredicateSpecification<Dog> left,
                                                   PredicateSpecification<Dog> right) {
        return left == null ? right : left.and(right);
    }

    /** Matches the term anywhere in the value, treating any wildcard typed as a literal. */
    private static String containsPattern(String value) {
        String escaped = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
