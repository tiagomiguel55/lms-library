package pt.psoft.g1.psoftg1.genremanagement.services;

import pt.psoft.g1.psoftg1.genremanagement.api.GenreViewAMQP;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.List;

public interface GenreService {

    Genre create(GenreViewAMQP genreViewAMQP);

    Genre update(GenreViewAMQP genreViewAMQP);

    void delete(GenreViewAMQP genreViewAMQP);

    List<GenreViewAMQP> getAllGenres();
}
