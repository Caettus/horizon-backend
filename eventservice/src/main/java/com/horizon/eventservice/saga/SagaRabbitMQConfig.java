package com.horizon.eventservice.saga;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SagaRabbitMQConfig {

    public static final String SAGA_EXCHANGE_NAME = "user.deletion.saga.exchange";
    public static final String QUEUE_NAME = "events.user.deletion.queue";

    @Bean
    public FanoutExchange sagaExchange() {
        return new FanoutExchange(SAGA_EXCHANGE_NAME);
    }

    @Bean
    public Queue userDeletionQueue() {
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public Binding userDeletionBinding(Queue userDeletionQueue, FanoutExchange sagaExchange) {
        return BindingBuilder.bind(userDeletionQueue).to(sagaExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}