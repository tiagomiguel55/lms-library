package pt.psoft.g1.psoftg1.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.psoft.g1.psoftg1.shared.services.generator.HashGenerator;
import pt.psoft.g1.psoftg1.shared.services.generator.HexGenerator;
import pt.psoft.g1.psoftg1.shared.services.generator.IdGenerator;

@Configuration
public class IdGeneratorConfig {

    @Bean
    @ConditionalOnProperty(name = "idgenerator.type", havingValue = "hash", matchIfMissing = true)
    public IdGenerator hashIdGenerator() {
        return new HashGenerator();
    }

    @Bean
    @ConditionalOnProperty(name = "idgenerator.type", havingValue = "hex")
    public IdGenerator hexIdGenerator() {
        return new HexGenerator();
    }
}