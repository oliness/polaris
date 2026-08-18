package uk.police.k9.dogs.config;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.police.k9.dogs.dto.DogFilter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("The filter query parameter")
class DogFilterConverterTest {

    private final DogFilterConverter converter = new DogFilterConverter(ObjectMapper.getDefault());

    @Test
    @DisplayName("is read as JSON into the search terms")
    void parsesJson() {
        ArgumentConversionContext<DogFilter> context = ConversionContext.of(DogFilter.class);

        Optional<DogFilter> filter = converter.convert("{\"name\":\"bax\",\"breed\":\"malinois\"}",
                DogFilter.class, context);

        assertThat(filter).contains(new DogFilter("bax", "malinois", null));
        assertThat(context.getLastError()).isEmpty();
    }

    @Test
    @DisplayName("matches everything when it is left out")
    void treatsBlankAsNoFilter() {
        ArgumentConversionContext<DogFilter> context = ConversionContext.of(DogFilter.class);

        assertThat(converter.convert("", DogFilter.class, context)).contains(DogFilter.empty());
        assertThat(context.getLastError()).isEmpty();
    }

    @Test
    @DisplayName("is rejected when a search term is misspelt, rather than silently widening the search")
    void rejectsUnknownTerms() {
        ArgumentConversionContext<DogFilter> context = ConversionContext.of(DogFilter.class);

        Optional<DogFilter> filter = converter.convert("{\"colour\":\"black\"}", DogFilter.class, context);

        assertThat(filter).isEmpty();
        assertThat(context.getLastError()).isPresent();
    }

    @Test
    @DisplayName("is rejected when it is not JSON at all")
    void rejectsMalformedJson() {
        ArgumentConversionContext<DogFilter> context = ConversionContext.of(DogFilter.class);

        Optional<DogFilter> filter = converter.convert("{oops", DogFilter.class, context);

        assertThat(filter).isEmpty();
        assertThat(context.getLastError())
                .hasValueSatisfying(error -> assertThat(error.getCause())
                        .hasMessageContaining("filter must be a JSON object"));
    }
}
