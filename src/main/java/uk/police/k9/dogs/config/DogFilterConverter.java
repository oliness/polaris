package uk.police.k9.dogs.config;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Singleton;
import uk.police.k9.dogs.dto.DogFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Binds the {@code filter} query parameter, which the task specifies as JSON, to a
 * {@link DogFilter}. Converting here rejects malformed JSON during binding, before the controller.
 */
@Singleton
public class DogFilterConverter implements TypeConverter<String, DogFilter> {

    private static final Argument<DogFilter> DOG_FILTER = Argument.of(DogFilter.class);

    private final JsonMapper jsonMapper;

    public DogFilterConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<DogFilter> convert(String value, Class<DogFilter> targetType, ConversionContext context) {
        if (value == null || value.isBlank()) {
            return Optional.of(DogFilter.empty());
        }
        try {
            return Optional.ofNullable(jsonMapper.readValue(value.getBytes(StandardCharsets.UTF_8), DOG_FILTER));
        } catch (IOException e) {
            context.reject(value, new IllegalArgumentException(
                    "filter must be a JSON object with any of the keys name, breed, supplier - " + e.getMessage(), e));
            return Optional.empty();
        }
    }
}
