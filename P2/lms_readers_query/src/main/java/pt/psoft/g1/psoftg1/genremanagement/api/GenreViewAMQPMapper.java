package pt.psoft.g1.psoftg1.genremanagement.api;

import org.mapstruct.Mapper;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

@Mapper(componentModel = "spring")
public interface GenreViewAMQPMapper {

    GenreViewAMQP toGenreViewAMQP(Genre genre);


}
