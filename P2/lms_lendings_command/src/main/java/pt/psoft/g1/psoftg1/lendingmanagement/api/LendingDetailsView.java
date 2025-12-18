package pt.psoft.g1.psoftg1.lendingmanagement.api;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class LendingDetailsView {

    // Informações do Livro
    @NotNull
    private String bookTitle;

    @NotNull
    private List<String> bookAuthorIds;

    @NotNull
    private String bookGenre;

    private String bookDescription;

    @NotNull
    private String bookIsbn;


    // Informações do Leitor
    @NotNull
    private String readerUsername;

    @NotNull
    private String readerFullName;

    private String readerNumber;

    private String readerPassword;

    private String readerBirthDate;

    private String readerPhoneNumber;

    private String readerPhotoUrl;

    private boolean readerGdpr;

    private boolean readerMarketing;

    private boolean readerThirdParty;


    private List<String> readerInterestList;


}
