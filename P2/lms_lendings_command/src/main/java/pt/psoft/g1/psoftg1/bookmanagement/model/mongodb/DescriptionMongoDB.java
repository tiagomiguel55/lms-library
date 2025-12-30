package pt.psoft.g1.psoftg1.bookmanagement.model.mongodb;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import org.springframework.data.mongodb.core.mapping.Field;
import pt.psoft.g1.psoftg1.shared.model.StringUtilsCustom;


public class DescriptionMongoDB {

    private final static int DESC_MAX_LENGTH = 4096;

    @Field
    String description;

    public DescriptionMongoDB(String description) {
        setDescription(description);
    }

    protected DescriptionMongoDB() {}

    public void setDescription(@Nullable String description) {
        if(description == null || description.isBlank()) {
            this.description = null;
        }else if(description.length() > DESC_MAX_LENGTH) {
            throw new IllegalArgumentException("Description has a maximum of 4096 characters");
        }else{
            this.description = StringUtilsCustom.sanitizeHtml(description);
        }
    }

    public String toString() {
        return this.description;
    }
}
