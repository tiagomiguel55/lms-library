package pt.psoft.g1.psoftg1.usermanagement.repositories.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.shared.model.relational.NameEntity;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.LibrarianEntity;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.ReaderEntity;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.UserEntity;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {

    ReaderEntity toEntity(Reader user);

    Reader toModel(ReaderEntity entity);


    @Mapping(target = "id", expression = "java(entity.getId())")
    @Mapping(target = "version", expression = "java(entity.getVersion())")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt())")
    @Mapping(target = "modifiedAt", expression = "java(entity.getModifiedAt())")
    @Mapping(target = "createdBy",expression = "java(entity.getCreatedBy())")
    @Mapping(target = "modifiedBy", expression = "java(entity.getModifiedBy())")
    @Mapping(target = "enabled", expression = "java(true)")
    ReaderEntity toReader (UserEntity entity);

    @Mapping(target = "id", expression = "java(entity.getId())")
    @Mapping(target = "version", expression = "java(entity.getVersion())")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt())")
    @Mapping(target = "modifiedAt", expression = "java(entity.getModifiedAt())")
    @Mapping(target = "createdBy",expression = "java(entity.getCreatedBy())")
    @Mapping(target = "modifiedBy", expression = "java(entity.getModifiedBy())")
    @Mapping(target = "enabled", expression = "java(true)")
    LibrarianEntity toLibrarian (UserEntity entity);


    UserEntity toEntity(User user);

    User toModel(UserEntity entity);

    LibrarianEntity toEntity(Librarian user);

    Librarian toModel(LibrarianEntity entity);

    default String map (Name name) {
        return name.getName();
    }

    default String map (NameEntity nameEntity) {
        return nameEntity.getName();
    }



}
