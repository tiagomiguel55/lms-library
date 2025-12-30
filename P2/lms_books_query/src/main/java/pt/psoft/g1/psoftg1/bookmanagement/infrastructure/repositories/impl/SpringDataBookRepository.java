package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataBookRepository extends BookRepository, BookRepoCustom, MongoRepository<Book, String> {

    default Optional<Book> findByIsbn(String isbn) {
        return ((BookRepoCustom) this).findByIsbnCustom(isbn);
    }

    default List<Book> findByGenre(String genre) {
        return ((BookRepoCustom) this).findByGenreCustom(genre);
    }

    default List<Book> findByTitle(String title) {
        return ((BookRepoCustom) this).findByTitleCustom(title);
    }

    default List<Book> findByAuthorName(String authorName) {
        return ((BookRepoCustom) this).findByAuthorNameCustom(authorName);
    }

    default List<Book> findBooksByAuthorNumber(Long authorNumber) {
        return ((BookRepoCustom) this).findBooksByAuthorNumberCustom(authorNumber);
    }
}

interface BookRepoCustom {
    Optional<Book> findByIsbnCustom(String isbn);
    List<Book> searchBooks(pt.psoft.g1.psoftg1.shared.services.Page page, SearchBooksQuery query);
    List<Book> findByGenreCustom(String genre);
    List<Book> findByTitleCustom(String title);
    List<Book> findByAuthorNameCustom(String authorName);
    List<Book> findBooksByAuthorNumberCustom(Long authorNumber);
}

@RequiredArgsConstructor
class BookRepoCustomImpl implements BookRepoCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<Book> findByIsbnCustom(String isbn) {
        Query query = new Query();
        query.addCriteria(Criteria.where("isbn.isbn").is(isbn));
        return Optional.ofNullable(mongoTemplate.findOne(query, Book.class));
    }

    @Override
    public List<Book> searchBooks(pt.psoft.g1.psoftg1.shared.services.Page page, SearchBooksQuery searchQuery) {
        Query query = new Query();

        if (StringUtils.hasText(searchQuery.getTitle())) {
            query.addCriteria(Criteria.where("title.title").regex("^" + searchQuery.getTitle(), "i"));
        }

        if (StringUtils.hasText(searchQuery.getGenre())) {
            query.addCriteria(Criteria.where("genre.genre").regex("^" + searchQuery.getGenre(), "i"));
        }

        if (StringUtils.hasText(searchQuery.getAuthorName())) {
            query.addCriteria(Criteria.where("authors.name.name").regex("^" + searchQuery.getAuthorName(), "i"));
        }

        query.skip((long) (page.getNumber() - 1) * page.getLimit());
        query.limit(page.getLimit());

        return mongoTemplate.find(query, Book.class);
    }

    @Override
    public List<Book> findByGenreCustom(String genre) {
        Query query = new Query();
        query.addCriteria(Criteria.where("genre.genre").regex(genre, "i"));
        return mongoTemplate.find(query, Book.class);
    }

    @Override
    public List<Book> findByTitleCustom(String title) {
        Query query = new Query();
        query.addCriteria(Criteria.where("title.title").regex(title, "i"));
        return mongoTemplate.find(query, Book.class);
    }

    @Override
    public List<Book> findByAuthorNameCustom(String authorName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("authors.name.name").regex(authorName, "i"));
        return mongoTemplate.find(query, Book.class);
    }

    @Override
    public List<Book> findBooksByAuthorNumberCustom(Long authorNumber) {
        Query query = new Query();
        query.addCriteria(Criteria.where("authors.authorNumber").is(authorNumber));
        return mongoTemplate.find(query, Book.class);
    }
}
