package uk.police.k9.dogs.mapper;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.police.k9.dogs.dto.DogRequest;
import uk.police.k9.dogs.dto.DogResponse;
import uk.police.k9.dogs.dto.GenderResponse;
import uk.police.k9.dogs.entity.Dog;
import uk.police.k9.dogs.entity.Gender;

/**
 * Converts between {@link Dog} and its API representations. Each target field names which of the
 * two sources it comes from, so adding a field without wiring it up is a compile error.
 */
@Mapper(config = MappingConfiguration.class,
        uses = {SupplierMapper.class, ReferenceDataMapper.class})
public interface DogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "breed", source = "request.breed")
    @Mapping(target = "badgeId", source = "request.badgeId")
    @Mapping(target = "gender", source = "request.gender")
    @Mapping(target = "birthDate", source = "request.birthDate")
    @Mapping(target = "dateAcquired", source = "request.dateAcquired")
    @Mapping(target = "leavingDate", source = "request.leavingDate")
    @Mapping(target = "kennellingCharacteristic", source = "request.kennellingCharacteristic")
    @Mapping(target = "supplier", source = "references.supplier")
    @Mapping(target = "status", source = "references.status")
    @Mapping(target = "leavingReason", source = "references.leavingReason")
    Dog toEntity(DogRequest request, DogReferences references);

    @InheritConfiguration(name = "toEntity")
    void applyTo(@MappingTarget Dog dog, DogRequest request, DogReferences references);

    DogResponse toResponse(Dog dog);

    default GenderResponse toResponse(Gender gender) {
        return gender == null ? null : GenderResponse.from(gender);
    }
}
