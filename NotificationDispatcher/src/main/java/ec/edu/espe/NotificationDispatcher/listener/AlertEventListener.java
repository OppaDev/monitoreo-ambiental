package ec.edu.espe.NotificationDispatcher.listener;

import ec.edu.espe.NotificationDispatcher.dto.AlertEvent;
import ec.edu.espe.NotificationDispatcher.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listener que escucha eventos desde la cola RabbitMQ del NotificationDispatcher
 * Procesa eventos de alerta y los envía al servicio de notificaciones
 */
@Component
@Slf4j
public class AlertEventListener {

    @Autowired
    private NotificationService notificationService;

    /**
     * Escucha eventos del bus global desde la cola q.events.notification-dispatcher
     * y procesa los que son eventos de alerta
     */
    @RabbitListener(
        queues = "${app-config.queues.dispatcher}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleGlobalEvent(AlertEvent event) {
        log.info("📨 AlertEvent recibido: tipo={}, sensor={}, valor={}", 
                event.getType(), event.getSensorId(), event.getValue());
        
        try {
            notificationService.processAlert(event);
            log.debug("✅ Notificación procesada exitosamente para alerta: {}", event.getAlertId());
        } catch (Exception e) {
            log.error("❌ Error al procesar evento de alerta en NotificationDispatcher: {}", e.getMessage(), e);
        }
    }
}
