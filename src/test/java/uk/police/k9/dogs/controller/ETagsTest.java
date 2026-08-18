package uk.police.k9.dogs.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("The If-Match precondition")
class ETagsTest {

    @Test
    @DisplayName("is the version the caller last read")
    void readsTheVersion() {
        assertThat(ETags.expectedVersions("\"3\"")).containsExactly(3L);
        assertThat(ETags.of(3L)).isEqualTo("\"3\"");
    }

    @Test
    @DisplayName("is absent when the caller set none, so the update applies unconditionally")
    void isAbsentWithoutAHeader() {
        assertThat(ETags.expectedVersions(null)).isNull();
        assertThat(ETags.expectedVersions("  ")).isNull();
    }

    @Test
    @DisplayName("asks only that the record exist when it is a wildcard")
    void treatsWildcardAsNoConstraint() {
        assertThat(ETags.expectedVersions("*")).isNull();
    }

    @Test
    @DisplayName("accepts any of the tags when the caller offers a list")
    void readsAList() {
        assertThat(ETags.expectedVersions("\"3\", \"4\"")).containsExactlyInAnyOrder(3L, 4L);
    }

    @Test
    @DisplayName("matches nothing when the tag is weak, because If-Match compares strongly")
    void refusesAWeakTag() {
        assertThat(ETags.expectedVersions("W/\"3\"")).isEmpty();
    }

    @Test
    @DisplayName("matches nothing when the tag is one this API could not have issued")
    void refusesATagItNeverIssued() {
        assertThat(ETags.expectedVersions("\"not-a-version\"")).isEmpty();
        assertThat(ETags.expectedVersions("3")).isEmpty();
    }
}
