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

    // User event queues
    @Bean
    public Queue userCreatedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue userUpdatedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue userDeletedQueue() {
        return new AnonymousQueue();
    }

    // Queue for user requests from other modules
    @Bean
    public Queue userRequestQueue() {
        return QueueBuilder.durable("reader.user.requested.user").build();
    }

    // Bindings for user events
    @Bean
    public Binding userCreatedBinding(DirectExchange direct,
                                     @Qualifier("userCreatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(UserEvents.USER_CREATED);
    }

    @Bean
    public Binding userUpdatedBinding(DirectExchange direct,
                                     @Qualifier("userUpdatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(UserEvents.USER_UPDATED);
    }

    @Bean
    public Binding userDeletedBinding(DirectExchange direct,
                                     @Qualifier("userDeletedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(UserEvents.USER_DELETED);
    }

    // Binding for user requests
    @Bean
    public Binding userRequestBinding(DirectExchange direct,
                                     @Qualifier("userRequestQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(UserEvents.USER_REQUESTED);
    }
}
