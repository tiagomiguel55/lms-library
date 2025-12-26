package pt.psoft.g1.psoftg1.bookmanagement.model;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;
import pt.psoft.g1.psoftg1.shared.model.StringUtilsCustom;

/**
 * Value object for Description.
 * Used as an embedded field in MongoDB documents.
 */
public class Description {
    private static final int DESC_MAX_LENGTH = 4096;

    @Size(max = DESC_MAX_LENGTH)
    private String description;

    public Description(String description) {
        setDescription(description);
    }

    protected Description() {
    }

    public void setDescription(@Nullable String description) {
        if (description == null || description.isBlank()) {
            this.description = null;
        } else if (description.length() > DESC_MAX_LENGTH) {
            throw new IllegalArgumentException("Description has a maximum of 4096 characters");
        } else {
            this.description = StringUtilsCustom.sanitizeHtml(description);
        }
    }

    public String toString() {
        return this.description;
    }
}
