package pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.impl;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataGenreRepository extends GenreRepository, GenreRepoCustom, MongoRepository<Genre, String> {

    default List<Genre> findAllGenres() {
        return findAll();
    }

    default Optional<Genre> findByString(@NotNull String genreName) {
        // Delegate to custom implementation
        return ((GenreRepoCustom) this).findByGenreNameCustom(genreName);
    }
}

interface GenreRepoCustom {
    Optional<Genre> findByGenreNameCustom(String genreName);
}

@RequiredArgsConstructor
class GenreRepoCustomImpl implements GenreRepoCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<Genre> findByGenreNameCustom(String genreName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("genre").is(genreName));
        return Optional.ofNullable(mongoTemplate.findOne(query, Genre.class));
    }
}