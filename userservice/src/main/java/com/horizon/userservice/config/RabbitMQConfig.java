package com.horizon.userservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "user.deletion.exchange";
    public static final String REQUESTED_QUEUE = "user.deletion.requested";
    public static final String EVENTS_CONFIRMED_QUEUE = "user.deletion.events.confirmed";
    public static final String RSVPS_CONFIRMED_QUEUE = "user.deletion.rsvps.confirmed";
    public static final String CONFIRMED_QUEUE = "user.deletion.confirmed";

    @Bean
    public DirectExchange userDeletionExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue userDeletionRequestedQueue() {
        return new Queue(REQUESTED_QUEUE, true);
    }

    @Bean
    public Queue userDeletionEventsConfirmedQueue() {
        return new Queue(EVENTS_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Queue userDeletionRsvpsConfirmedQueue() {
        return new Queue(RSVPS_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Queue userDeletionConfirmedQueue() {
        return new Queue(CONFIRMED_QUEUE, true);
    }

    @Bean
    public Binding userDeletionRequestedBinding() {
        return BindingBuilder.bind(userDeletionRequestedQueue())
                .to(userDeletionExchange())
                .with("user.deletion.requested");
    }

    @Bean
    public Binding userDeletionEventsConfirmedBinding() {
        return BindingBuilder.bind(userDeletionEventsConfirmedQueue())
                .to(userDeletionExchange())
                .with("user.deletion.events.confirmed");
    }

    @Bean
    public Binding userDeletionRsvpsConfirmedBinding() {
        return BindingBuilder.bind(userDeletionRsvpsConfirmedQueue())
                .to(userDeletionExchange())
                .with("user.deletion.rsvps.confirmed");
    }

    @Bean
    public Binding userDeletionConfirmedBinding() {
        return BindingBuilder.bind(userDeletionConfirmedQueue())
                .to(userDeletionExchange())
                .with("user.deletion.confirmed");
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
} 