package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.util.StringUtils;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;

import java.util.List;
import java.util.Optional;

public interface SpringDataBookRepository extends BookRepository, BookRepoCustom, MongoRepository<Book, String> {

    Optional<Book> findByIsbn(String isbn);

    @org.springframework.data.mongodb.repository.Query("{ 'genre.$id': ?0 }")
    List<Book> findByGenreId(String genreId);

    default List<Book> findByGenre(String genre) {
        // This needs custom implementation - will search by genre name via lookup
        throw new UnsupportedOperationException("Use findByGenreId or custom implementation");
    }

    @org.springframework.data.mongodb.repository.Query("{ 'title': { $regex: ?0, $options: 'i' } }")
    List<Book> findByTitle(String title);

    @org.springframework.data.mongodb.repository.Query("{ 'authors.name': { $regex: ?0, $options: 'i' } }")
    List<Book> findByAuthorName(String authorName);

    @org.springframework.data.mongodb.repository.Query("{ 'authors.$id': ?0 }")
    List<Book> findBooksByAuthorNumber(Long authorNumber);
}

interface BookRepoCustom {
    List<Book> searchBooks(pt.psoft.g1.psoftg1.shared.services.Page page, SearchBooksQuery query);
    List<Book> findByGenreName(String genreName);
}

@RequiredArgsConstructor
class BookRepoCustomImpl implements BookRepoCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Book> searchBooks(pt.psoft.g1.psoftg1.shared.services.Page page, SearchBooksQuery query) {
        String title = query.getTitle();
        String genre = query.getGenre();
        String authorName = query.getAuthorName();

        Query mongoQuery = new Query();
        
        if (StringUtils.hasText(title)) {
            mongoQuery.addCriteria(Criteria.where("title").regex("^" + title, "i"));
        }

        if (StringUtils.hasText(genre)) {
            mongoQuery.addCriteria(Criteria.where("genre.genre").regex("^" + genre, "i"));
        }

        if (StringUtils.hasText(authorName)) {
            mongoQuery.addCriteria(Criteria.where("authors.name").regex("^" + authorName, "i"));
        }

        // Apply pagination
        int skip = (page.getNumber() - 1) * page.getLimit();
        mongoQuery.skip(skip).limit(page.getLimit());
        
        // Sort by title
        mongoQuery.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "title"));

        return mongoTemplate.find(mongoQuery, Book.class);
    }

    @Override
    public List<Book> findByGenreName(String genreName) {
        Query mongoQuery = new Query();
        mongoQuery.addCriteria(Criteria.where("genre.genre").regex(genreName, "i"));
        return mongoTemplate.find(mongoQuery, Book.class);
    }
}
