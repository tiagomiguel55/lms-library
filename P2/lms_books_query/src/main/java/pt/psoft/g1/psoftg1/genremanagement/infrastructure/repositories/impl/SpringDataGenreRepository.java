package pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.impl;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDTO;

import java.util.*;

public interface SpringDataGenreRepository extends GenreRepository, GenreRepoCustom, MongoRepository<Genre, String> {

    @Query("{}")
    List<Genre> findAllGenres();

    @Override
    @Query("{ 'genre': ?0 }")
    Optional<Genre> findByString(@NotNull String genre);
}

interface GenreRepoCustom {
}

@RequiredArgsConstructor
class GenreRepoCustomImpl implements GenreRepoCustom {

    @NotNull
    private List<GenreLendingsPerMonthDTO> getGenreLendingsPerMonthDtos(
            Map<Integer, Map<Integer, List<GenreLendingsDTO>>> groupedResults) {
        List<GenreLendingsPerMonthDTO> lendingsPerMonth = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, List<GenreLendingsDTO>>> yearEntry : groupedResults.entrySet()) {
            int yearValue = yearEntry.getKey();
            for (Map.Entry<Integer, List<GenreLendingsDTO>> monthEntry : yearEntry.getValue().entrySet()) {
                int monthValue = monthEntry.getKey();
                List<GenreLendingsDTO> values = monthEntry.getValue();
                lendingsPerMonth.add(new GenreLendingsPerMonthDTO(yearValue, monthValue, values));
            }
        }

        return lendingsPerMonth;
    }
}