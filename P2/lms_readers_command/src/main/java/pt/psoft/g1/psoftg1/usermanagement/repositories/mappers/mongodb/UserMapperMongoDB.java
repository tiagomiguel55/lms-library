package pt.psoft.g1.psoftg1.usermanagement.repositories.mappers.mongodb;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.shared.model.mongodb.NameMongoDB;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.model.mongodb.LibrarianMongoDB;
import pt.psoft.g1.psoftg1.usermanagement.model.mongodb.ReaderMongoDB;
import pt.psoft.g1.psoftg1.usermanagement.model.mongodb.UserMongoDB;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapperMongoDB {

    ReaderMongoDB toMongoDB(Reader user);

    Reader toDomain(ReaderMongoDB readerMongoDB);

    @Mapping(target = "id", expression = "java(userMongoDB.getId())")
    @Mapping(target = "createdAt", expression = "java(userMongoDB.getCreatedAt())")
    @Mapping(target = "modifiedAt", expression = "java(userMongoDB.getModifiedAt())")
    @Mapping(target = "createdBy",expression = "java(userMongoDB.getCreatedBy())")
    @Mapping(target = "modifiedBy", expression = "java(userMongoDB.getModifiedBy())")
    @Mapping(target = "enabled", expression = "java(true)")
    ReaderMongoDB toReader (UserMongoDB userMongoDB);

    UserMongoDB toMongoDB(User user);

    User toDomain(UserMongoDB userMongoDB);

    LibrarianMongoDB toMongoDB(Librarian user);

    Librarian toDomain(LibrarianMongoDB librarianMongoDB);

    default String map (Name name) {
        return name.getName();
    }

    default String map (NameMongoDB nameMongoDB) {
        return nameMongoDB.getName();
    }

}
