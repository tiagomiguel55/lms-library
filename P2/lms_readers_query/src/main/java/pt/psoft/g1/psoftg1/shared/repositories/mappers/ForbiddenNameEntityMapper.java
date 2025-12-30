package pt.psoft.g1.psoftg1.shared.repositories.mappers;

import org.mapstruct.Mapper;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.model.relational.ForbiddenNameEntity;

@Mapper(componentModel = "spring")
public interface ForbiddenNameEntityMapper {

    ForbiddenNameEntity toEntity(ForbiddenName forbiddenName);

    ForbiddenName toModel(ForbiddenNameEntity entity);
}
