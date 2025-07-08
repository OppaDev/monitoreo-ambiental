package ec.edu.espe.NotificationDispatcher.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el NotificationDispatcher
 * Se conecta al exchange global para recibir eventos de alerta
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app-config.exchanges.global-events}")
    private String globalEventsExchangeName;

    @Value("${app-config.queues.dispatcher}")
    private String dispatcherQueueName;

    @Bean
    public FanoutExchange globalEventsExchange() {
        return new FanoutExchange(globalEventsExchangeName, true, false);
    }

    @Bean
    public Queue dispatcherQueue() {
        // Cola duradera para que no se pierdan las notificaciones si el servicio se reinicia
        return new Queue(dispatcherQueueName, true);
    }

    @Bean
    public Binding binding(Queue dispatcherQueue, FanoutExchange globalEventsExchange) {
        // Enlaza la cola al exchange. Al ser Fanout, recibirá todos los eventos publicados.
        return BindingBuilder.bind(dispatcherQueue).to(globalEventsExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Permite enviar y recibir objetos Java como JSON automáticamente.
        return new Jackson2JsonMessageConverter();
    }
}
