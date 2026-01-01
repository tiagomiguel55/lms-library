package pt.psoft.g1.psoftg1.usermanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "A User for AMQP communication in readers_command")
public class UserViewAMQP {

    @NotNull
    @Size(max = 100)
    private String username;

    @NotNull
    @Size(max = 100)
    private String fullName;

    private String password;

    private String version;

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();
}
