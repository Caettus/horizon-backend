package com.horizon.userservice.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserExchangeRabbitMQConfig {

    public static final String USERS_EXCHANGE_NAME = "horizon.users.exchange";

    @Bean
    TopicExchange usersExchange() {
        // durable = true, autoDelete = false by default
        return new TopicExchange(USERS_EXCHANGE_NAME);
    }

    // No queues or bindings are defined on the producer (UserService) side for its own consumption related to these events.
    // Queues and bindings will be defined in consumer services (like RsvpService).
    // However, if UserService also needed to consume messages from this exchange via a specific queue,
    // it would be defined here.
} 