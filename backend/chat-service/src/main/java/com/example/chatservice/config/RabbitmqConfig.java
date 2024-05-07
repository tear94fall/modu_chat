package com.example.chatservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${rabbitmq.queue.queue1.name}")
    private String queueName;

    @Value("${rabbitmq.queue.queue2.name}")
    private String wsQueueName;

    @Value("${rabbitmq.queue.queue3.name}")
    private String stQueueName;

    @Value("${rabbitmq.queue.queue1.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.queue.queue2.exchange}")
    private String wsExchangeName;

    @Value("${rabbitmq.queue.queue3.exchange}")
    private String stExchangeName;

    @Value("${rabbitmq.queue.routing.key.queue1}")
    private String routingKey;

    @Value("${rabbitmq.queue.routing.key.queue2}")
    private String wsRoutingKey;

    @Value("${rabbitmq.queue.routing.key.queue3}")
    private String stRoutingKey;

    @Bean
    Queue queue() {
        return new Queue(queueName);
    }

    @Bean
    Queue wsQueue() {
        return new Queue(wsQueueName);
    }

    @Bean
    Queue stQueue() {
        return new Queue(stQueueName);
    }

    @Bean
    DirectExchange directExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    DirectExchange wsDirectExchange() {
        return new DirectExchange(wsExchangeName);
    }

    @Bean
    DirectExchange stDirectExchange() {
        return new DirectExchange(stExchangeName);
    }

    @Bean
    Binding binding(DirectExchange directExchange, Queue queue) {
        return BindingBuilder.bind(queue).to(directExchange).with(routingKey);
    }

    @Bean
    Binding wsBinding(DirectExchange wsDirectExchange, Queue wsQueue) {
        return BindingBuilder.bind(wsQueue).to(wsDirectExchange).with(wsRoutingKey);
    }

    @Bean
    Binding stBinding(DirectExchange stDirectExchange, Queue stQueue) {
        return BindingBuilder.bind(stQueue).to(stDirectExchange).with(stRoutingKey);
    }

    @Bean
    ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
