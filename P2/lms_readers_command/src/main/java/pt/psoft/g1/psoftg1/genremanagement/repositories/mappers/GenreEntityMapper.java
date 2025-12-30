package pt.psoft.g1.psoftg1.genremanagement.repositories.mappers;

import org.mapstruct.Mapper;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.model.relational.GenreEntity;

@Mapper(componentModel = "spring")
public interface GenreEntityMapper {

    Genre toModel(GenreEntity genreEntity);

    GenreEntity toEntity(Genre genre);

}
