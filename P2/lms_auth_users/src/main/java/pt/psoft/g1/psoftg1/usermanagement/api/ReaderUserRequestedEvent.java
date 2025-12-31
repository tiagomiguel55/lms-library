package pt.psoft.g1.psoftg1.usermanagement.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReaderUserRequestedEvent {
    private String readerNumber;
    private String username;
    private String password;
    private String fullName;
    private String birthDate;
    private String phoneNumber;
    private String photoURI;
    private boolean gdpr;
    private boolean marketing;
    private boolean thirdParty;
}