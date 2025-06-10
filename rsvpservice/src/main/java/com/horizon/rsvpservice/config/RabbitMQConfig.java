package com.horizon.rsvpservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange to listen to
    public static final String USERS_EXCHANGE_NAME = "horizon.users.exchange"; // Must match UserService exchange

    // Queue for RsvpService to consume user events
    public static final String RSVP_USER_EVENTS_QUEUE_NAME = "rsvp.service.user.updates.queue"; // From UserEventListener

    // Routing keys to bind for
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_PROFILE_UPDATED_ROUTING_KEY = "user.profile.updated";
    public static final String USER_FORGOTTEN_ROUTING_KEY = "user.forgotten";

    // Queue for user forgotten events
    public static final String RSVP_USER_FORGOTTEN_QUEUE_NAME = "horizon.users.forgotten.rsvps";

    @Bean
    TopicExchange usersExchange() {
        // This declares the exchange. If already declared by UserService with same properties, it's idempotent.
        // It's good practice for consumers to also declare to ensure robustness, 
        // or have a central way of defining shared infrastructure.
        return new TopicExchange(USERS_EXCHANGE_NAME);
    }

    @Bean
    Queue rsvpUserEventsQueue() {
        // Durable queue (true by default)
        return new Queue(RSVP_USER_EVENTS_QUEUE_NAME);
    }

    @Bean
    Binding userRegisteredBinding(Queue rsvpUserEventsQueue, TopicExchange usersExchange) {
        return BindingBuilder.bind(rsvpUserEventsQueue).to(usersExchange).with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    Binding userProfileUpdatedBinding(Queue rsvpUserEventsQueue, TopicExchange usersExchange) {
        return BindingBuilder.bind(rsvpUserEventsQueue).to(usersExchange).with(USER_PROFILE_UPDATED_ROUTING_KEY);
    }

    @Bean
    Queue rsvpUserForgottenQueue() {
        return new Queue(RSVP_USER_FORGOTTEN_QUEUE_NAME);
    }

    @Bean
    Binding userForgottenBinding(Queue rsvpUserForgottenQueue, TopicExchange usersExchange) {
        return BindingBuilder.bind(rsvpUserForgottenQueue).to(usersExchange).with(USER_FORGOTTEN_ROUTING_KEY);
    }

    // Future: If NotificationService is added, it would have a similar configuration for its own queue
    // public static final String NOTIFICATION_USER_EVENTS_QUEUE_NAME = "notification.service.user.updates.queue";
    // @Bean Queue notificationUserEventsQueue() { return new Queue(NOTIFICATION_USER_EVENTS_QUEUE_NAME); }
    // @Bean Binding notificationUserRegisteredBinding(...) { ... }
    // @Bean Binding notificationUserProfileUpdatedBinding(...) { ... }
} 