package pt.psoft.g1.psoftg1.genremanagement.repositories;


import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.List;
import java.util.Optional;

public interface GenreRepository {
    Optional<Genre> findByName(String name);
    Genre save(Genre genre);
    List<String> getMostLentGenres(int maxGenres);
    String getMostLentGenreByReader(String readerNumber);
    void delete(Genre genre);
    List<Genre> findAll();
}
