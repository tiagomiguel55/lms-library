package pt.psoft.g1.psoftg1.shared.services.generator;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Profile("hex")
@Component
public class HexGenerator implements IdGenerator{

    @Override
    public String generateId() {
        // Lógica para gerar ID hexadecimal (24 caracteres)
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12]; // 12 bytes * 2 = 24 caracteres hexadecimais
        random.nextBytes(bytes);

        StringBuilder hexId = new StringBuilder();
        for (byte b : bytes) {
            hexId.append(String.format("%02x", b));
        }
        return hexId.toString();
    }


}
