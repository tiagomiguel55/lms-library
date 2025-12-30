package pt.psoft.g1.psoftg1.shared.services;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
@RequiredArgsConstructor
public class ForbiddenNameServiceImpl implements ForbiddenNameService {
    private final ForbiddenNameRepository repo;

    public void loadDataFromFile(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        final var fn = repo.findByForbiddenName(line);
                        if (fn.isEmpty()) {
                            ForbiddenName entity = new ForbiddenName(line);
                            repo.save(entity);
                        }
                    } catch (DuplicateKeyException e) {
                        // Ignore duplicate key errors when multiple replicas try to insert the same data
                        // This is expected behavior when running multiple instances
                    } catch (Exception e) {
                        // Log and continue with other names if one fails
                        System.err.println("Warning: Could not process forbidden name '" + line + "': " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
