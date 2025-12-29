package pt.psoft.g1.psoftg1.genremanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreViewAMQP;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreViewAMQPMapper;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private  final GenreViewAMQPMapper genreViewAMQPMapper;


    @Override
    public Genre create(GenreViewAMQP genreViewAMQP) {
        genreRepository.findByName(genreViewAMQP.getGenre()).ifPresent(genre -> {
            throw new RuntimeException("Genre already exists");
        });
        Genre genre = new Genre(genreViewAMQP.getGenre());
        Genre SavedGenre = genreRepository.save(genre);
        return SavedGenre;
    }

    @Override
    public Genre update(GenreViewAMQP genreViewAMQP) {
        Genre genre = genreRepository.findByName(genreViewAMQP.getGenre()).orElseThrow();

        genre.setGenre(genreViewAMQP.getGenre());

        Genre SavedGenre = genreRepository.save(genre);

        return SavedGenre;
    }

    @Override
    public void delete(GenreViewAMQP genreViewAMQP) {

    }

    @Override
    public List<GenreViewAMQP> getAllGenres() {
        List <Genre> genres = genreRepository.findAll();

        List<GenreViewAMQP> genreViewAMQPS = new ArrayList<>();

        for (Genre genre : genres) {
            genreViewAMQPS.add(genreViewAMQPMapper.toGenreViewAMQP(genre));
        }

        return genreViewAMQPS;
    }


}
