package ec.edu.espe.EnvironmentalAnalyzer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // El nombre del exchange global donde se publican TODOS los eventos
    public static final String GLOBAL_EVENTS_EXCHANGE = "environmental.events.exchange";

    // Usar la nomenclatura estándar definida en SystemConstants
    public static final String ANALYZER_QUEUE = SystemConstants.Queues.ENVIRONMENTAL_ANALYZER;

    @Bean
    public FanoutExchange globalEventsExchange() {
        return new FanoutExchange(GLOBAL_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue analyzerQueue() {
        // Una cola duradera para que no se pierdan los mensajes si el servicio se reinicia
        return new Queue(ANALYZER_QUEUE, true);
    }

    @Bean
    public Binding binding(Queue analyzerQueue, FanoutExchange globalEventsExchange) {
        // Enlaza la cola al exchange. Al ser Fanout, recibirá todos los mensajes publicados.
        return BindingBuilder.bind(analyzerQueue).to(globalEventsExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Permite enviar y recibir objetos Java como JSON automáticamente.
        return new Jackson2JsonMessageConverter();
    }
}
