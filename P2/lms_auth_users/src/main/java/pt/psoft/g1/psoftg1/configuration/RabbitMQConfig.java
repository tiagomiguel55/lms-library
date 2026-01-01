package pt.psoft.g1.psoftg1.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.psoft.g1.psoftg1.shared.model.UserEvents;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Direct exchange for user events
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("LMS.direct");
    }

    // Queue for SAGA: Reader + User creation requests
    @Bean
    public Queue userRequestQueue() {
        return QueueBuilder.durable("reader.user.requested.user").build();
    }

    // Binding for SAGA requests
    @Bean
    public Binding userRequestBinding(DirectExchange direct,
                                     @Qualifier("userRequestQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(UserEvents.USER_REQUESTED);
    }

    // NOTE: Sync queues (user.created, user.updated, user.deleted) are NOT needed
    // because all replicas share the same database.
}
