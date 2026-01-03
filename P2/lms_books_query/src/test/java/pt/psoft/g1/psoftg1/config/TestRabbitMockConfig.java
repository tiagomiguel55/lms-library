package pt.psoft.g1.psoftg1.config;

import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that provides mocked RabbitMQ beans.
 * This allows smoke tests to run without a real RabbitMQ connection.
 */
@TestConfiguration
public class TestRabbitMockConfig {

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    @Bean("direct")
    public DirectExchange direct() {
        return Mockito.mock(DirectExchange.class);
    }

    @Bean("directAuthors")
    public DirectExchange directAuthors() {
        return Mockito.mock(DirectExchange.class);
    }

    @Bean("directGenres")
    public DirectExchange directGenres() {
        return Mockito.mock(DirectExchange.class);
    }

    @Bean
    public Queue queue1() {
        return Mockito.mock(Queue.class);
    }

    @Bean
    public Queue queue2() {
        return Mockito.mock(Queue.class);
    }

    @Bean
    public Queue queue3() {
        return Mockito.mock(Queue.class);
    }

    @Bean
    public Queue queue4() {
        return Mockito.mock(Queue.class);
    }

    @Bean
    public Queue queueAuthors1() {
        return Mockito.mock(Queue.class);
    }

    @Bean
    public Queue queueAuthors2() {
        return Mockito.mock(Queue.class);
    }

    @Bean
    public Queue queueAuthors3() {
        return Mockito.mock(Queue.class);
    }

    @Bean
    public Queue queueGenres1() {
        return Mockito.mock(Queue.class);
    }

    @Bean
    public Queue queueGenres2() {
        return Mockito.mock(Queue.class);
    }

    @Bean
    public Queue queueGenres3() {
        return Mockito.mock(Queue.class);
    }
}
