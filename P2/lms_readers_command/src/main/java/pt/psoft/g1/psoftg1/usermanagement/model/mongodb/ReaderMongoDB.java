package pt.psoft.g1.psoftg1.usermanagement.model.mongodb;

import org.springframework.data.mongodb.core.mapping.Document;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

@Document(collection = "readers")
public class ReaderMongoDB extends UserMongoDB {

    protected ReaderMongoDB() {
        // for ORM only
    }

    public ReaderMongoDB(String username, String password){
        super(username, password);
        this.addAuthority(new Role(Role.READER));
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

    public static ReaderMongoDB newReader(final String username, final String password, final String name){
        final var u = new ReaderMongoDB(username, password);
        u.setName(name);
        return u;
    }
}
