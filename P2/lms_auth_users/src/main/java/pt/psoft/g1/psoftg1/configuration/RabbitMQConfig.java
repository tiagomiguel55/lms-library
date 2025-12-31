package pt.psoft.g1.psoftg1.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for lms_auth_users service
 *
 * Configures queues and bindings for participating in the Reader+User creation SAGA
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("LMS.direct");
    }

    // ========================================
    // SAGA: Reader + User Creation Queues
    // ========================================

    /**
     * Queue to receive requests to create a User as part of Reader+User SAGA
     * This is a DURABLE NAMED queue so the service can restart without losing messages
     */
    @Bean
    public Queue readerUserRequestedUserQueue() {
        return new Queue("reader.user.requested.user", true);
    }

    /**
     * Queue to send confirmation that User was created (pending finalization)
     * This is a DURABLE NAMED queue for the SAGA coordinator to receive
     */
    @Bean
    public Queue userPendingCreatedQueue() {
        return new Queue("user.pending.created", true);
    }

    // ========================================
    // Bindings
    // ========================================

    @Bean
    public Binding readerUserRequestedUserBinding(
            @Qualifier("directExchange") DirectExchange direct,
            @Qualifier("readerUserRequestedUserQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("reader.user.requested.user");
    }

    @Bean
    public Binding userPendingCreatedBinding(
            @Qualifier("directExchange") DirectExchange direct,
            @Qualifier("userPendingCreatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("user.pending.created");
    }
}

