package com.horizon.userservice.saga;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SagaRabbitMQConfig {

    public static final String SAGA_EXCHANGE_NAME = "user.deletion.saga.exchange";
    public static final String REPLY_QUEUE_NAME = "user.deletion.reply.queue";

    @Bean
    public FanoutExchange sagaExchange() {
        return new FanoutExchange(SAGA_EXCHANGE_NAME);
    }

    @Bean
    public Queue sagaReplyQueue() {
        return new Queue(REPLY_QUEUE_NAME);
    }

    //This is not needed as the reply queue is configured above.
    /*@Bean
    public Binding sagaReplyBinding(Queue sagaReplyQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(sagaReplyQueue).to(sagaExchange).with(REPLY_QUEUE_NAME);
    }*/


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
