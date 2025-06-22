package com.horizon.userservice.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserExchangeRabbitMQConfig {

    public static final String USERS_EXCHANGE_NAME = "horizon.users.exchange";
    public static final String SAGA_REPLY_QUEUE = "saga.replies.queue";
    public static final String SAGA_REPLY_EXCHANGE = "saga.replies.exchange";


    @Bean
    TopicExchange usersExchange() {
        // durable = true, autoDelete = false by default
        return new TopicExchange(USERS_EXCHANGE_NAME);
    }

    @Bean
    Queue sagaReplyQueue() {
        return new Queue(SAGA_REPLY_QUEUE);
    }

    @Bean
    DirectExchange sagaReplyExchange() {
        return new DirectExchange(SAGA_REPLY_EXCHANGE);
    }

    @Bean
    Binding sagaReplyBinding(Queue sagaReplyQueue, DirectExchange sagaReplyExchange) {
        return BindingBuilder.bind(sagaReplyQueue).to(sagaReplyExchange).with(SAGA_REPLY_QUEUE);
    }

    // No queues or bindings are defined on the producer (UserService) side for its own consumption related to these events.
    // Queues and bindings will be defined in consumer services (like RsvpService).
    // However, if UserService also needed to consume messages from this exchange via a specific queue,
    // it would be defined here.
} 