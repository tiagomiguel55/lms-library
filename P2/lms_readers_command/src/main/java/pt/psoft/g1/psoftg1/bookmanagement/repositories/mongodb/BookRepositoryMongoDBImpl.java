package pt.psoft.g1.psoftg1.bookmanagement.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.mongodb.BookMongoDB;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.mappers.BookMapperMongoDB;
import pt.psoft.g1.psoftg1.genremanagement.model.mongodb.GenreMongoDB;
import pt.psoft.g1.psoftg1.genremanagement.repositories.mongodb.GenreRepositoryMongoDB;
import pt.psoft.g1.psoftg1.lendingmanagement.model.mongodb.LendingMongoDB;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Profile("mongodb")
@Qualifier("mongoDbRepo")
@Component
public class BookRepositoryMongoDBImpl implements BookRepository {

    private final BookRepositoryMongoDB bookRepositoryMongoDB;
    private final GenreRepositoryMongoDB genreRepositoryMongoDB;
    private final BookMapperMongoDB bookMapperMongoDB;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    @Lazy
    public BookRepositoryMongoDBImpl(BookRepositoryMongoDB bookRepositoryMongoDB,
                                     BookMapperMongoDB bookMapperMongoDB, GenreRepositoryMongoDB genreRepositoryMongoDB, MongoTemplate mongoTemplate) {
        this.bookRepositoryMongoDB = bookRepositoryMongoDB;
        this.bookMapperMongoDB = bookMapperMongoDB;
        this.genreRepositoryMongoDB = genreRepositoryMongoDB;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        BookMongoDB bookMongoDB = bookRepositoryMongoDB.findByIsbn(isbn).get();
        return Optional.ofNullable(bookMapperMongoDB.toDomain(bookMongoDB));
    }


    @Override
    public Book save(Book book) {
        BookMongoDB bookMongoDB = bookMapperMongoDB.toMongoDB(book);
        // Check if the genre exists to assign it to the book
        if (bookMongoDB.getGenre() != null) {
            // Verifica se o gênero já existe no banco de dados pelo nome
            GenreMongoDB existingGenre = genreRepositoryMongoDB.findByGenre(bookMongoDB.getGenre().getGenre()).get();
            if (existingGenre == null) {
                // Se o gênero não existe, salva o novo gênero
                existingGenre = genreRepositoryMongoDB.save(bookMongoDB.getGenre());
                bookMongoDB.setGenre(existingGenre);
            }
            bookMongoDB.setGenre(existingGenre); // Atualiza o gênero da BookEntity com o gênero persistido
        }


        BookMongoDB savedEntity = bookRepositoryMongoDB.save(bookMongoDB);

        return bookMapperMongoDB.toDomain(savedEntity);
    }

    @Override
    public void delete(Book book) {
        // Assuming you have a method to delete by ISBN or ID
        //bookRepositoryMongoDB.deleteByIsbn(book.getIsbn());
    }

    @Override
    public List<Book> findAll() {
        return bookRepositoryMongoDB.findAll().stream()
                .map(bookMapperMongoDB::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Book> findMostLentBooksByGenre(int maxBooks, String genre) {
        // 1. Buscar todos os empréstimos da coleção 'lendings'
        List<LendingMongoDB> lendings = mongoTemplate.findAll(LendingMongoDB.class, "lendings");
       // 2. Filtrar os empréstimos e contar os livros por gênero
        Map<String, Long> lendingCountMap = lendings.stream()
                .filter(lending -> {
                    // Obter o livro correspondente ao empréstimo através do método getBook
                    BookMongoDB book = lending.getBook(); // Método que retorna o livro associado
                    return book != null && genre.equals(book.getGenre().getGenre());
                })
                .collect(Collectors.groupingBy(lending -> lending.getBook().getBookId(), Collectors.counting())); // Contar por bookId
        // 3. Obter os IDs dos livros mais emprestados e limitar ao maxBooks
        List<String> topBookIds = lendingCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(maxBooks)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 4. Recuperar os detalhes dos livros mais emprestados
        List<BookMongoDB> mostLentBooks = mongoTemplate.find(Query.query(Criteria.where("_id").in(topBookIds)), BookMongoDB.class, "books");

        // 5. Mapear para objetos de domínio
        return mostLentBooks.stream()
                .map(bookMapperMongoDB::toDomain)
                .collect(Collectors.toList());
    }
}
