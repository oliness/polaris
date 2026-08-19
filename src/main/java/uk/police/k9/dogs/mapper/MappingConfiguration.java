package uk.police.k9.dogs.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Settings shared by every MapStruct mapper. {@code ReportingPolicy.ERROR} is the important one:
 * an unmapped target field fails the build, so a new column cannot silently miss the API.
 */
@MapperConfig(
        componentModel = MappingConstants.ComponentModel.JSR330,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MappingConfiguration {
}
