package com.horizon.eventservice.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDeletionRabbitMQConfig {

    public static final String EXCHANGE_NAME = "user.deletion.exchange";
    public static final String USER_DELETION_REQUESTED_QUEUE = "user.deletion.requested.eventservice.queue";
    public static final String USER_DELETION_REQUESTED_ROUTING_KEY = "user.deletion.requested";

    @Bean
    public TopicExchange userDeletionExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue userDeletionRequestedQueue() {
        return new Queue(USER_DELETION_REQUESTED_QUEUE);
    }

    @Bean
    public Binding userDeletionBinding(TopicExchange userDeletionExchange, Queue userDeletionRequestedQueue) {
        return BindingBuilder.bind(userDeletionRequestedQueue).to(userDeletionExchange).with(USER_DELETION_REQUESTED_ROUTING_KEY);
    }
} 