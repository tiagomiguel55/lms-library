package pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface SpringDataAuthorRepository extends AuthorRepository, AuthorRepoCustom, MongoRepository<Author, String> {

    default Optional<Author> findByAuthorNumber(Long authorNumber) {
        return ((AuthorRepoCustom) this).findByAuthorNumberCustom(authorNumber);
    }

    default List<Author> findCoAuthorsByAuthorNumber(Long authorNumber) {
        return ((AuthorRepoCustom) this).findCoAuthorsByAuthorNumberCustom(authorNumber);
    }
}

interface AuthorRepoCustom {
    Optional<Author> findByAuthorNumberCustom(Long authorNumber);
    List<Author> findCoAuthorsByAuthorNumberCustom(Long authorNumber);
}

@RequiredArgsConstructor
class AuthorRepoCustomImpl implements AuthorRepoCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<Author> findByAuthorNumberCustom(Long authorNumber) {
        Query query = new Query();
        query.addCriteria(Criteria.where("authorNumber").is(authorNumber));
        return Optional.ofNullable(mongoTemplate.findOne(query, Author.class));
    }

    @Override
    public List<Author> findCoAuthorsByAuthorNumberCustom(Long authorNumber) {
        // First find all books by this author
        Query bookQuery = new Query();
        bookQuery.addCriteria(Criteria.where("authors.$id").exists(true));
        List<Book> books = mongoTemplate.find(bookQuery, Book.class);

        // Extract all co-authors from these books (excluding the original author)
        return books.stream()
            .filter(book -> book.getAuthors() != null)
            .flatMap(book -> book.getAuthors().stream())
            .filter(author -> author.getAuthorNumber() != authorNumber)
            .distinct()
            .collect(Collectors.toList());
    }
}
