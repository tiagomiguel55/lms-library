package pt.psoft.g1.psoftg1.shared.services.generator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Profile("hash")
@Component
public class HashGenerator implements IdGenerator{

    @Override
    public String generateId() {
        try {
            // Gerar um UUID aleatório
            String randomUUID = UUID.randomUUID().toString();

            // Criar um hash SHA-256 do UUID
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(randomUUID.getBytes(StandardCharsets.UTF_8));

            // Converter o hash para uma string hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            // Retornar os primeiros 20 caracteres do hash
            return hexString.toString().substring(0, 20);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating ID", e);
        }
    }


}
