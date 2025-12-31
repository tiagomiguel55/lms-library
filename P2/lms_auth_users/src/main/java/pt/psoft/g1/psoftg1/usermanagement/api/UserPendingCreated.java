package pt.psoft.g1.psoftg1.usermanagement.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPendingCreated {
    private String readerNumber;
    private String userId;
    private String username;
    private boolean finalized;
}

