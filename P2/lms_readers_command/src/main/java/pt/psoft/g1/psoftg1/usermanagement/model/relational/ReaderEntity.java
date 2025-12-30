package pt.psoft.g1.psoftg1.usermanagement.model.relational;

import jakarta.persistence.Entity;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

@Entity
public class ReaderEntity extends UserEntity {
    protected ReaderEntity() {
        // for ORM only
    }
    public ReaderEntity(String username, String password) {
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

    public static ReaderEntity newReader(final String username, final String password, final String name) {
        final var u = new ReaderEntity(username, password);
        u.setName(name);
        return u;
    }
}
