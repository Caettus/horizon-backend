package com.horizon.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String USERS_EXCHANGE_NAME = "horizon.users.exchange";

    // Queues
    public static final String RSVP_USER_EVENTS_QUEUE_NAME = "rsvp.service.user.updates.queue";
    public static final String RSVP_USER_FORGOTTEN_QUEUE_NAME = "horizon.users.forgotten.rsvps";

    // Routing keys
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_PROFILE_UPDATED_ROUTING_KEY = "user.profile.updated";
    public static final String USER_FORGOTTEN_ROUTING_KEY = "user.forgotten";

    @Bean
    public TopicExchange usersExchange() {
        return new TopicExchange(USERS_EXCHANGE_NAME);
    }

    // RSVP Service specific queues and bindings
    @Bean
    public Queue rsvpUserEventsQueue() {
        return new Queue(RSVP_USER_EVENTS_QUEUE_NAME);
    }

    @Bean
    public Binding userRegisteredBinding(Queue rsvpUserEventsQueue, TopicExchange usersExchange) {
        return BindingBuilder.bind(rsvpUserEventsQueue).to(usersExchange).with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Binding userProfileUpdatedBinding(Queue rsvpUserEventsQueue, TopicExchange usersExchange) {
        return BindingBuilder.bind(rsvpUserEventsQueue).to(usersExchange).with(USER_PROFILE_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Queue rsvpUserForgottenQueue() {
        return new Queue(RSVP_USER_FORGOTTEN_QUEUE_NAME);
    }

    @Bean
    public Binding userForgottenBinding(Queue rsvpUserForgottenQueue, TopicExchange usersExchange) {
        return BindingBuilder.bind(rsvpUserForgottenQueue).to(usersExchange).with(USER_FORGOTTEN_ROUTING_KEY);
    }
} 