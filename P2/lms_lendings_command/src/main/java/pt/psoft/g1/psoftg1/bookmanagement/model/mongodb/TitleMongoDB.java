package pt.psoft.g1.psoftg1.bookmanagement.model.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

public class TitleMongoDB {

    private static final int TITLE_MAX_LENGTH = 128;

    @Field
    @Getter
    String title;

    protected TitleMongoDB() {}

    public TitleMongoDB(String title) {
        this.title = title;
    }

    public void setTitle(String title){
        if(title == null)
            throw new IllegalArgumentException("Title cannot be null");
        if(title.isBlank())
            throw new IllegalArgumentException("Title cannot be blank");
        if(title.length() > TITLE_MAX_LENGTH)
            throw new IllegalArgumentException("Title has a maximum of " + TITLE_MAX_LENGTH + " characters");
        this.title = title.strip();

    }

    public String toString() {
        return this.title;
    }

}
