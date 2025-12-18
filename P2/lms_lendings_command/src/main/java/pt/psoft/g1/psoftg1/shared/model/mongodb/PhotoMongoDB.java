package pt.psoft.g1.psoftg1.shared.model.mongodb;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Document(collection = "photos")  // Optional: Specify collection name
public class PhotoMongoDB {

    @Id
    private String id;  // MongoDB will auto-generate the ID as an ObjectId (String)

    @Field("photoFile")
    @NotNull
    @Setter
    @Getter
    private String photoFile;

    protected PhotoMongoDB() {
        // For ORM or deserialization only
    }

    public PhotoMongoDB(Path photoPath) {
        setPhotoFile(photoPath.toString());
    }

    public String getId() {
        return id;
    }
}


