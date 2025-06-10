package com.horizon.userservice.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDeletionRabbitMQConfig {

    public static final String EXCHANGE_NAME = "user.deletion.exchange";
    public static final String USER_DELETION_REQUESTED_QUEUE = "user.deletion.requested.queue";
    public static final String USER_DELETION_CONFIRMED_QUEUE = "user.deletion.confirmed.queue";
    public static final String USER_DELETION_EVENTS_CONFIRMED_QUEUE = "user.deletion.events.confirmed";
    public static final String USER_DELETION_RSVPS_CONFIRMED_QUEUE = "user.deletion.rsvps.confirmed";

    // Added for failure cases
    public static final String USER_DELETION_EVENTS_FAILED_QUEUE = "user.deletion.events.failed";
    public static final String USER_DELETION_RSVPS_FAILED_QUEUE = "user.deletion.rsvps.failed";

    public static final String USER_DELETION_ROUTING_KEY = "user.deletion.#";
    public static final String USER_DELETION_EVENTS_CONFIRMED_ROUTING_KEY = "user.deletion.events.confirmed";
    public static final String USER_DELETION_RSVPS_CONFIRMED_ROUTING_KEY = "user.deletion.rsvps.confirmed";

    // Added for failure cases
    public static final String USER_DELETION_EVENTS_FAILED_ROUTING_KEY = "user.deletion.events.failed";
    public static final String USER_DELETION_RSVPS_FAILED_ROUTING_KEY = "user.deletion.rsvps.failed";

    @Bean
    public TopicExchange userDeletionExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue userDeletionRequestedQueue() {
        return new Queue(USER_DELETION_REQUESTED_QUEUE);
    }

    @Bean
    public Queue userDeletionConfirmedQueue() {
        return new Queue(USER_DELETION_CONFIRMED_QUEUE);
    }

    @Bean
    public Queue userDeletionEventsConfirmedQueue() {
        return new Queue(USER_DELETION_EVENTS_CONFIRMED_QUEUE);
    }

    @Bean
    public Queue userDeletionRsvpsConfirmedQueue() {
        return new Queue(USER_DELETION_RSVPS_CONFIRMED_QUEUE);
    }

    // Added for failure cases
    @Bean
    public Queue userDeletionEventsFailedQueue() {
        return new Queue(USER_DELETION_EVENTS_FAILED_QUEUE);
    }

    @Bean
    public Queue userDeletionRsvpsFailedQueue() {
        return new Queue(USER_DELETION_RSVPS_FAILED_QUEUE);
    }

    @Bean
    public Binding userDeletionBinding(TopicExchange userDeletionExchange, Queue userDeletionRequestedQueue) {
        return BindingBuilder.bind(userDeletionRequestedQueue).to(userDeletionExchange).with(USER_DELETION_ROUTING_KEY);
    }

    @Bean
    public Binding userDeletionEventsConfirmedBinding(TopicExchange userDeletionExchange, Queue userDeletionEventsConfirmedQueue) {
        return BindingBuilder.bind(userDeletionEventsConfirmedQueue).to(userDeletionExchange).with(USER_DELETION_EVENTS_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding userDeletionRsvpsConfirmedBinding(TopicExchange userDeletionExchange, Queue userDeletionRsvpsConfirmedQueue) {
        return BindingBuilder.bind(userDeletionRsvpsConfirmedQueue).to(userDeletionExchange).with(USER_DELETION_RSVPS_CONFIRMED_ROUTING_KEY);
    }

    // Added for failure cases
    @Bean
    public Binding userDeletionEventsFailedBinding(TopicExchange userDeletionExchange, Queue userDeletionEventsFailedQueue) {
        return BindingBuilder.bind(userDeletionEventsFailedQueue).to(userDeletionExchange).with(USER_DELETION_EVENTS_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding userDeletionRsvpsFailedBinding(TopicExchange userDeletionExchange, Queue userDeletionRsvpsFailedQueue) {
        return BindingBuilder.bind(userDeletionRsvpsFailedQueue).to(userDeletionExchange).with(USER_DELETION_RSVPS_FAILED_ROUTING_KEY);
    }
}