package pt.psoft.g1.psoftg1.shared.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.nio.file.Path;

@Document(collection = "photos")
public class Photo {
    @Id
    private String id;

    @NotNull
    @Setter
    @Getter
    private String photoFile;

    public Photo() {
    }

    public Photo(Path photoPath) {
        setPhotoFile(photoPath.toString());
    }

    public Photo(String photoFile) {
        setPhotoFile(photoFile);
    }

    public void setPhotoFile(String photoFile) {
        this.photoFile = photoFile;
    }
}
