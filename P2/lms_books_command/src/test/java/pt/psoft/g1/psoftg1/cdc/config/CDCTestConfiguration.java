package pt.psoft.g1.psoftg1.cdc.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.mockito.Mockito;

/**
 * Configuração de teste para CDC
 */
@TestConfiguration
@Profile("cdc-test")
public class CDCTestConfiguration {

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    public DirectExchange directExchange() {
        return Mockito.mock(DirectExchange.class);
    }
}
