package pt.psoft.g1.psoftg1.shared.model;

import org.springframework.context.annotation.PropertySource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * Value object for Name.
 * Used as an embedded field in MongoDB documents.
 */
@Getter
@PropertySource({ "classpath:config/library.properties" })
public class Name {
    @NotNull
    @NotBlank
    private String name;

    public Name(String name) {
        setName(name);
    }

    public void setName(String name) {
        if (name == null)
            throw new IllegalArgumentException("Name cannot be null");
        if (name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank, nor only white spaces");
        if (!StringUtilsCustom.isAlphanumeric(name))
            throw new IllegalArgumentException("Name can only contain alphanumeric characters");

        /*
         * // Logic moved to UserService.java, ReaderService.java for(String forbidden : forbiddenNames){
         * if(name.contains(forbidden)) throw new IllegalArgumentException("Name contains forbidden word"); }
         */
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    protected Name() {
        // for ORM only
    }
}
