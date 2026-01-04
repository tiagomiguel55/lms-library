package pt.psoft.g1.psoftg1.smokeTests;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that provides a mocked RabbitTemplate bean.
 * This allows smoke tests to run without a real RabbitMQ connection.
 */
@TestConfiguration
public class TestRabbitMockConfig {

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }
}
