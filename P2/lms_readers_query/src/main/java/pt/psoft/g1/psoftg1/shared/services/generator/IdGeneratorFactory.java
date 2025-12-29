package pt.psoft.g1.psoftg1.shared.services.generator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdGeneratorFactory {

    @Autowired
    private IdGenerator idGenerator;

    public IdGenerator getGenerator() {
        return idGenerator;
    }
}

