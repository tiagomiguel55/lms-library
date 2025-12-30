package pt.psoft.g1.psoftg1.genremanagement.repositories.mongodb;

import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.BookMongoDB;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.model.mongodb.GenreMongoDB;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.genremanagement.repositories.mappers.GenreMapperMongoDB;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.LendingMongoDB;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Profile("mongodb")
@Qualifier("mongoDbRepo")
@Component
public class GenreRepositoryMongoDBImpl implements GenreRepository {

    private final GenreRepositoryMongoDB genreRepositoryMongoDB;

    private final GenreMapperMongoDB genreMapperMongoDB;

    private MongoTemplate mongoTemplate;

    private final MongoClient mongoClient;

    private final MongoClient mongo;

    @Autowired
    @Lazy
    public GenreRepositoryMongoDBImpl(GenreRepositoryMongoDB genreRepositoryMongoDB, GenreMapperMongoDB genreMapperMongoDB, MongoClient mongoClient, @Qualifier("mongo") MongoClient mongo,MongoTemplate mongoTemplate) {
        this.genreRepositoryMongoDB = genreRepositoryMongoDB;
        this.genreMapperMongoDB = genreMapperMongoDB;
        this.mongoTemplate = mongoTemplate;
        this.mongoClient = mongoClient;
        this.mongo = mongo;
    }

    @Override
    public Optional<Genre> findByName(String genre) {
        GenreMongoDB genreMongoDB = genreRepositoryMongoDB.findByGenre(genre).get();
        return Optional.ofNullable(genreMapperMongoDB.toDomain(genreMongoDB));
    }



    @Override
    public Genre save(Genre genre) {
        GenreMongoDB mongoGenre = genreMapperMongoDB.toMongoDB(genre);
        System.out.println("MongoGenre: " + mongoGenre.getGenre());
        GenreMongoDB savedGenre = genreRepositoryMongoDB.save(mongoGenre);
        return genreMapperMongoDB.toDomain(savedGenre);
    }


    @Override
    public void delete(Genre genre) {

    }

    @Override
    public List<Genre> findAll() {
        return genreRepositoryMongoDB.findAll().stream()
                .map(genreMapperMongoDB::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getMostLentGenres(int maxGenres) {
        // 1. Buscar todos os empréstimos da coleção 'lendings'
        List<LendingMongoDB> lendings = mongoTemplate.findAll(LendingMongoDB.class, "lendings");

        // 2. Agrupar e contar os empréstimos por gênero
        Map<String, Long> genreCountMap = lendings.stream()
                .map(lending -> {
                    BookMongoDB book = lending.getBook();
                    return (book != null && book.getGenre() != null) ? book.getGenre().getGenre() : null;
                })
                .filter(Objects::nonNull) // Remover valores nulos
                .collect(Collectors.groupingBy(genre -> genre, Collectors.counting()));

        // 3. Ordenar os gêneros por contagem e limitar ao máximo especificado
        List<String> topGenres = genreCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(maxGenres)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return topGenres;
    }

    @Override
    public String getMostLentGenreByReader(String readerNumber) {
        // Step 1: Retrieve all lendings
        List<LendingMongoDB> allLendings = mongoTemplate.findAll(LendingMongoDB.class, "lendings");

        // Step 2: Filter lendings by reader number
        List<LendingMongoDB> filteredLendings = allLendings.stream()
                .filter(lending -> lending.getReaderDetails().getReaderNumber().equals(readerNumber))
                .collect(Collectors.toList());

        // Step 3: Group by genre and count occurrences
        Map<String, Long> genreCount = filteredLendings.stream()
                .map(lending -> lending.getBook().getGenre().getGenre()) // Assuming getGenre() returns GenreMongoDB
                .collect(Collectors.groupingBy(genre -> genre, Collectors.counting()));

        // Step 4: Find the most lent genre
        return genreCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey) // Get the genre name
                .orElse(null); // Return null if no genres found
    }
}
