package pt.psoft.g1.psoftg1.shared.model;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "forbidden_names")
@NoArgsConstructor
public class ForbiddenName {

    @Id
    private String id;

    @Getter
    @Setter
    @Size(min = 1)
    @Indexed(unique = true)
    private String forbiddenName;

    public ForbiddenName(String name) {
        this.forbiddenName = name;
    }
}
