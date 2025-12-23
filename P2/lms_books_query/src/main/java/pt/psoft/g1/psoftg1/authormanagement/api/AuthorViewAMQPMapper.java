package pt.psoft.g1.psoftg1.authormanagement.api;

import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import org.springframework.stereotype.Component;

@Component
public class AuthorViewAMQPMapper {

    public AuthorViewAMQP authorToAuthorViewAMQP(Author author) {
        return new AuthorViewAMQP(
            author.getAuthorNumber(),
            author.getName(),
            author.getBio(),
            author.getPhoto() != null ? author.getPhoto().getPhotoFile() : null
        );
    }

    public Author authorViewAMQPtoAuthor(AuthorViewAMQP authorViewAMQP) {
        return new Author(
            authorViewAMQP.getAuthorNumber(),
            authorViewAMQP.getName(),
            authorViewAMQP.getBio(),
            null
        );
    }
}
