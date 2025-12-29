package pt.psoft.g1.psoftg1.genremanagement.repositories.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.model.mongodb.GenreMongoDB;

@Mapper(componentModel = "spring")
public interface GenreMapperMongoDB {

    GenreMongoDB toMongoDB(Genre genre);

    Genre toDomain(GenreMongoDB genreMongoDB);
}
