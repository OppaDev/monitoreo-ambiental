package ec.edu.espe.SensorDataCollector.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange-name}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue-name}")
    private String queueName;

    // 1. Define el Exchange Fanout global
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(exchangeName, true, false); // durable=true, autoDelete=false
    }

    // 2. Define la Cola específica para este microservicio
    @Bean
    public Queue sensorDataCollectorQueue() {
        return new Queue(queueName, true); // durable=true
    }

    // 3. Vincula la Cola al Exchange (Binding)
    // Esto hace que la cola 'q.events.sensor-data-collector' reciba todos los mensajes del exchange 'environmental.events.exchange'.
    @Bean
    public Binding binding(Queue sensorDataCollectorQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(sensorDataCollectorQueue).to(fanoutExchange);
    }

    // 4. Configura el MessageConverter para que los objetos se envíen como JSON
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 5. Configura el RabbitTemplate para que use el convertidor a JSON por defecto
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}