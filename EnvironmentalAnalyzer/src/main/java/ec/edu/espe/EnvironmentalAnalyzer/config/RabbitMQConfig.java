package ec.edu.espe.EnvironmentalAnalyzer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app-config.exchanges.global-events}")
    private String globalEventsExchangeName;

    @Value("${app-config.queues.analyzer}")
    private String analyzerQueueName;

    @Bean
    public FanoutExchange globalEventsExchange() {
        return new FanoutExchange(globalEventsExchangeName, true, false);
    }

    @Bean
    public Queue analyzerQueue() {
        // Una cola duradera para que no se pierdan los mensajes si el servicio se reinicia
        return new Queue(analyzerQueueName, true);
    }

    @Bean
    public Binding binding(Queue analyzerQueue, FanoutExchange globalEventsExchange) {
        // Enlaza la cola al exchange. Al ser Fanout, recibirá todos los mensajes publicados.
        return BindingBuilder.bind(analyzerQueue).to(globalEventsExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // Incluir información de tipo para mejorar la deserialización
        converter.setCreateMessageIds(true);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
