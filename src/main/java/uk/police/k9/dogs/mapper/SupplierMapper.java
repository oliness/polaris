package uk.police.k9.dogs.mapper;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.police.k9.dogs.dto.SupplierRequest;
import uk.police.k9.dogs.dto.SupplierResponse;
import uk.police.k9.dogs.entity.Supplier;

/**
 * Converts between {@link Supplier} and its API representations.
 */
@Mapper(config = MappingConfiguration.class)
public interface SupplierMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Supplier toEntity(SupplierRequest request);

    @InheritConfiguration(name = "toEntity")
    void applyTo(@MappingTarget Supplier supplier, SupplierRequest request);

    SupplierResponse toResponse(Supplier supplier);
}
