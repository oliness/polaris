package uk.police.k9.dogs.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.police.k9.dogs.dto.ReferenceDataRequest;
import uk.police.k9.dogs.dto.ReferenceDataResponse;
import uk.police.k9.dogs.entity.ReferenceDataEntity;

/**
 * Converts between the reference-data tables and their API representations, written against
 * {@link ReferenceDataEntity} so both lookups share one mapper.
 */
@Mapper(config = MappingConfiguration.class)
public interface ReferenceDataMapper {

    ReferenceDataResponse toResponse(ReferenceDataEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "displayOrder", source = "displayOrder", defaultValue = "0")
    void applyTo(@MappingTarget ReferenceDataEntity entity, ReferenceDataRequest request);
}
