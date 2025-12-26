package pt.psoft.g1.psoftg1.bookmanagement.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Value object for Title.
 * Used as an embedded field in MongoDB documents.
 */
public class Title {
    private static final int TITLE_MAX_LENGTH = 128;
    
    @NotBlank(message = "Title cannot be blank")
    @Size(min = 1, max = TITLE_MAX_LENGTH)
    @Getter
    private String title;

    protected Title() {
    }

    public Title(String title) {
        setTitle(title);
    }

    public void setTitle(String title) {

        /*
         * if (!StringUtilsCustom.startsOrEndsInWhiteSpace(title)) { throw new
         * IllegalArgumentException("Invalid title: " + title); }
         */
        if (title == null)
            throw new IllegalArgumentException("Title cannot be null");
        if (title.isBlank())
            throw new IllegalArgumentException("Title cannot be blank");
        if (title.length() > TITLE_MAX_LENGTH)
            throw new IllegalArgumentException("Title has a maximum of " + TITLE_MAX_LENGTH + " characters");
        this.title = title.strip();
    }

    public String toString() {
        return this.title;
    }
}
