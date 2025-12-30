package pt.psoft.g1.psoftg1.shared.services.generator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IdGeneratorFactory {

    private final IdGenerator idGenerator;

    /**
     * Spring-managed constructor. If a bean of type IdGenerator is available (from configuration), use it.
     * Otherwise, fall back to a safe default implementation.
     */
    @Autowired
    public IdGeneratorFactory(Optional<IdGenerator> idGeneratorOptional) {
        this.idGenerator = idGeneratorOptional.orElseGet(HashGenerator::new);
    }

    /**
     * No-args constructor for manual instantiation (e.g., domain code using `new IdGeneratorFactory()`).
     * Provides a safe default generator to avoid NullPointerExceptions when not in a Spring context.
     */
    public IdGeneratorFactory() {
        this.idGenerator = new HashGenerator();
    }

    public IdGenerator getGenerator() {
        return idGenerator;
    }
}

