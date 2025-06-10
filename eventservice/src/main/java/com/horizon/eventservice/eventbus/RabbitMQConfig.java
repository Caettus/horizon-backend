package com.horizon.eventservice.eventbus;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "horizon.exchange.events";
    public static final String QUEUE_EVENT_CREATED = "horizon.queue.event.created";
    public static final String ROUTING_KEY_EVENT_CREATED = "event.created";

    // User forgotten event configuration
    public static final String USERS_EXCHANGE = "horizon.users.exchange";
    public static final String QUEUE_USER_FORGOTTEN_EVENTS = "horizon.users.forgotten.events";
    public static final String ROUTING_KEY_USER_FORGOTTEN = "user.forgotten";

    @Bean
    TopicExchange exchange() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    Queue queueEventCreated() {
        return QueueBuilder
                .durable(QUEUE_EVENT_CREATED)
                .build();
    }

    @Bean
    Binding bindingEventCreated(Queue queueEventCreated, TopicExchange exchange) {
        return BindingBuilder
                .bind(queueEventCreated)
                .to(exchange)
                .with(ROUTING_KEY_EVENT_CREATED);
    }

    @Bean
    public Queue userForgottenQueue() {
        return new Queue(QUEUE_USER_FORGOTTEN_EVENTS, true);
    }

    @Bean
    public TopicExchange usersExchange() {
        return new TopicExchange(USERS_EXCHANGE, true, false);
    }

    @Bean
    public Binding userForgottenBinding(Queue userForgottenQueue, TopicExchange usersExchange) {
        return BindingBuilder.bind(userForgottenQueue).to(usersExchange).with(ROUTING_KEY_USER_FORGOTTEN);
    }

    @Bean
    MessageConverter jsonConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
