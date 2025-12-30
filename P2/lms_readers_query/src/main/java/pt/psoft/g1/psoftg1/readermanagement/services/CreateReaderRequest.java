package pt.psoft.g1.psoftg1.readermanagement.services;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReaderRequest {
    @NotBlank
    @Email
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String fullName;

    @NotBlank
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String birthDate;

    @NotBlank
    private String phoneNumber;

    @Nullable
    private MultipartFile photo;

    private boolean gdpr;

    private boolean marketing;

    private boolean thirdParty;

    @Nullable
    private List<String> interestList;
}
