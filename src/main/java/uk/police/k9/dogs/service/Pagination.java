package uk.police.k9.dogs.service;

import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;

/** Applied to every paged query. */
final class Pagination {

    private Pagination() {
    }

    /**
     * Without a default sort the database may return rows in any order, so page 2 can repeat or
     * skip what was on page 1. {@code withTotal()} is needed because a {@code Pageable} that was
     * not asked for a total will refuse to report one.
     */
    static Pageable normalise(Pageable pageable, Sort defaultSort) {
        Pageable sorted = pageable.isSorted() ? pageable : pageable.withSort(defaultSort);
        return sorted.withTotal();
    }
}
