package pt.psoft.g1.psoftg1.shared.model.mongodb;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import pt.psoft.g1.psoftg1.shared.model.StringUtilsCustom;

@Document(collection = "names")  // Optional: Specify the MongoDB collection
public class NameMongoDB {

    @Field("name")  // Optional: Map to a specific field name in MongoDB
    @NotNull
    @Getter
    @NotBlank
    private String name;

    // Constructor, getters, setters as usual
    public NameMongoDB(String name) {
        setName(name);
    }

    public void setName(String name) {
        if (name == null) throw new IllegalArgumentException("Name cannot be null");
        if (name.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
        if (!StringUtilsCustom.isAlphanumeric(name)) throw new IllegalArgumentException("Name must be alphanumeric");
        this.name = name;
    }

    protected NameMongoDB() {
        // for ORM only
    }

    @Override
    public String toString() {
        return this.name;
    }
}
