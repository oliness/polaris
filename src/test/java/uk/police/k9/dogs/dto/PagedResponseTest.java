package uk.police.k9.dogs.dto;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A page of results")
class PagedResponseTest {

    @Test
    @DisplayName("carries the paging details a client needs to ask for the next page")
    void copiesPagingDetails() {
        Page<String> page = Page.of(List.of("Baxter", "Nala"), Pageable.from(1, 2), 5L);

        PagedResponse<String> response = PagedResponse.from(page, name -> name);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("maps every entity on the page into its response form")
    void mapsContent() {
        Page<String> page = Page.of(List.of("Baxter", "Nala"), Pageable.from(0, 2), 2L);

        PagedResponse<Integer> response = PagedResponse.from(page, String::length);

        assertThat(response.content()).containsExactly(6, 4);
    }

    @Test
    @DisplayName("is empty rather than absent when nothing matched")
    void emptyPageHasEmptyContent() {
        Page<String> page = Page.of(List.of(), Pageable.from(0, 20), 0L);

        PagedResponse<String> response = PagedResponse.from(page, name -> name);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }
}
