package pt.psoft.g1.psoftg1.usermanagement.model.mongodb;

import org.springframework.data.mongodb.core.mapping.Document;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

@Document(collection = "librarians")
public class LibrarianMongoDB extends UserMongoDB {

    protected LibrarianMongoDB() {
        // for ORM only
    }

    public LibrarianMongoDB(String username, String password) {
        super(username, password);
    }

    /**
     * factory method. since mapstruct does not handle protected/private setters
     * neither more than one public constructor, we use these factory methods for
     * helper creation scenarios
     *
     * @param username
     * @param password
     * @param name
     * @return
     */

    public static LibrarianMongoDB newLibrarian(final String username, final String password, final String name) {
        final var u = new LibrarianMongoDB(username, password);
        u.setName(name);
        u.addAuthority(new Role(Role.LIBRARIAN));
        return u;
    }
}
