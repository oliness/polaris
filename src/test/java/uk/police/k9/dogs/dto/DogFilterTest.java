package uk.police.k9.dogs.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A dog filter")
class DogFilterTest {

    @Test
    @DisplayName("matches everything when it is empty")
    void emptyFilterHasNoTerms() {
        DogFilter filter = DogFilter.empty();

        assertThat(filter.hasName()).isFalse();
        assertThat(filter.hasBreed()).isFalse();
        assertThat(filter.hasSupplier()).isFalse();
    }

    @Test
    @DisplayName("reports the terms that were supplied")
    void reportsSuppliedTerms() {
        DogFilter filter = new DogFilter("bax", null, "ashcombe");

        assertThat(filter.hasName()).isTrue();
        assertThat(filter.hasBreed()).isFalse();
        assertThat(filter.hasSupplier()).isTrue();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "   "})
    @DisplayName("treats a blank term as no term at all, so it does not search for nothing")
    void blankTermsAreIgnored(String blank) {
        DogFilter filter = new DogFilter(blank, blank, blank);

        assertThat(filter.hasName()).isFalse();
        assertThat(filter.hasBreed()).isFalse();
        assertThat(filter.hasSupplier()).isFalse();
    }
}
