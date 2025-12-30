package pt.psoft.g1.psoftg1.shared.model.mongodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "forbiddenNames")
@NoArgsConstructor
public class ForbiddenNameMongoDB {

    @Id
    private String pk;

    @Getter
    @Setter
    private String forbiddenName;

    public ForbiddenNameMongoDB(String name) {
        this.forbiddenName = name;
    }
}
