package com.horizon.userservice.eventbus;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "horizon.exchange.events";
    public static final String QUEUE_EVENT_CREATED = "horizon.queue.event.created";

    @Bean
    TopicExchange exchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    Queue queueEventCreated() {
        return QueueBuilder.durable(QUEUE_EVENT_CREATED).build();
    }

    @Bean
    Binding binding(Queue queueEventCreated, TopicExchange exchange) {
        return BindingBuilder.bind(queueEventCreated).to(exchange).with("event.created");
    }

    @Bean
    MessageConverter jsonConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
