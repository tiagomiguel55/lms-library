package pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.impl;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataAuthorRepository extends AuthorRepository, MongoRepository<Author, String> {
    @Override
    @Query("{ 'authorNumber': ?0 }")
    Optional<Author> findByAuthorNumber(Long authorNumber);

    @Override
    @Query("{ 'name': { $regex: '^?0', $options: 'i' } }")
    List<Author> searchByNameNameStartsWith(String name);

    @Override
    @Query("{ 'name': { $regex: '^?0$', $options: 'i' } }")
    List<Author> searchByNameName(String name);

    // Co-authors query would need to be implemented differently with MongoDB
    // This would typically require application-level logic or aggregation pipeline
    default List<Author> findCoAuthorsByAuthorNumber(Long authorNumber) {
        // This requires fetching books by author, then getting other authors
        // Implementation should be done in service layer with multiple queries
        throw new UnsupportedOperationException("Use service layer for co-author queries");
    }
}
