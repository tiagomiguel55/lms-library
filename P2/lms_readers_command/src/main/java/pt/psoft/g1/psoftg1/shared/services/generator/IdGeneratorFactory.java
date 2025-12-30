package pt.psoft.g1.psoftg1.shared.services.generator;



import org.springframework.stereotype.Component;

@Component
    public class IdGeneratorFactory {

    public IdGenerator getGenerator() {
        return ApplicationContextProvider.getApplicationContext().getBean(IdGenerator.class);

    }
}

