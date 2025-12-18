package pt.psoft.g1.psoftg1.usermanagement.model.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.psoft.g1.psoftg1.shared.model.mongodb.NameMongoDB;
import pt.psoft.g1.psoftg1.usermanagement.model.Password;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
@EnableMongoAuditing
public class UserMongoDB implements UserDetails {

    private static final long serialVersionUID = 1L;

    @Id
    @Getter
    @Setter
    private String id;

    @Field("version")
    @Version
    private Long version;

    @Field("createdAt")
    @CreatedDate
    @Getter
    @Setter
    private LocalDateTime createdAt;

    @Field("modifiedAt")
    @LastModifiedDate
    @Getter
    @Setter
    private LocalDateTime modifiedAt;

    @Field("createdBy")
    @CreatedBy
    @Getter
    @Setter
    private String createdBy;

    @Field("modifiedBy")
    @LastModifiedBy
    @Getter
    @Setter
    private String modifiedBy;

    @Getter
    @Setter
    private boolean enabled = true;

    @Setter
    @Getter
    @Field("username")
    private String username;

    @Getter
    @Setter
    @Field("password")
    private String password;

    @Getter
    @Field("name")
    private NameMongoDB name;

    @Getter
    @Setter
    @Field("authorities")
    private Set<Role> authorities = new HashSet<>();

    protected UserMongoDB () {
        // for ORM only
    }

    /**
     *
     * @param username
     * @param password
     */
    public UserMongoDB(final String username, final String password) {
        this.username = username;
        setPassword(password);
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
    public static UserMongoDB newUser(final String username, final String password, final String name){
        final var u = new UserMongoDB(username, password);
        u.setName(name);
        return u;
    }

    public void setPassword(final String password){
        Password passwordCheck = new Password(password);
        final PasswordEncoder passwordEnconder = new BCryptPasswordEncoder();
        this.password = passwordEnconder.encode(password);
    }

    public void addAuthority(final Role role){
        this.authorities.add(role);
    }

    public void setName(final String name){
        this.name = new NameMongoDB(name);
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.enabled;
    }
}
