package pt.psoft.g1.psoftg1.readermanagement.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ReaderSagaViewAMQP {

    private String lendingNumber;
    @NotNull
    @Size(max = 100)
    private String username;

    @NotNull
    @Size(max = 100)
    private String fullName;

    private String readerNumber;

    private String password;

    private String birthDate;

    private String phoneNumber;

    private String photoUrl;

    private boolean gdpr;

    private boolean marketing;

    private boolean thirdParty;

    private String version;

    private List<String> interestList;

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();
}
