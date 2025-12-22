package pt.psoft.g1.psoftg1.bookmanagement.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchBooksQuery {
    @Getter
    private String title;

    @Getter
    private String genre;

    @Getter
    private String authorName;
}
