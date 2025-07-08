package ec.edu.espe.NotificationDispatcher.listener;

import ec.edu.espe.NotificationDispatcher.dto.AlertEvent;
import ec.edu.espe.NotificationDispatcher.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Listener que escucha eventos desde la cola RabbitMQ del NotificationDispatcher
 * Procesa eventos de alerta y los envía al servicio de notificaciones
 */
@Component
@Slf4j
public class AlertEventListener {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Escucha eventos del bus global desde la cola q.events.notification-dispatcher
     * y procesa los que son eventos de alerta
     */
    @RabbitListener(queues = "${app-config.queues.dispatcher}")
    public void handleGlobalEvent(Object event) {
        log.info("📨 Evento recibido en NotificationDispatcher: {}", event.getClass().getSimpleName());
        
        try {
            // Si el evento es directamente un AlertEvent
            if (event instanceof AlertEvent) {
                log.info("✅ Procesando AlertEvent directo");
                notificationService.processAlert((AlertEvent) event);
                return;
            }
            
            // Si el evento viene como Map (JSON deserializado)
            if (event instanceof Map) {
                Map<String, Object> eventMap = (Map<String, Object>) event;
                
                // Verificar si tiene estructura de alerta
                if (isAlertEvent(eventMap)) {
                    AlertEvent alertEvent = convertToAlertEvent(eventMap);
                    if (alertEvent != null) {
                        log.info("✅ Procesando evento de alerta convertido: {}", alertEvent.getType());
                        notificationService.processAlert(alertEvent);
                    }
                } else {
                    log.debug("ℹ️ Evento recibido no es una alerta, ignorando: {}", eventMap.get("eventType"));
                }
                return;
            }
            
            // Si viene como String (JSON)
            if (event instanceof String) {
                try {
                    Map<String, Object> eventMap = objectMapper.readValue((String) event, Map.class);
                    if (isAlertEvent(eventMap)) {
                        AlertEvent alertEvent = convertToAlertEvent(eventMap);
                        if (alertEvent != null) {
                            log.info("✅ Procesando evento JSON convertido: {}", alertEvent.getType());
                            notificationService.processAlert(alertEvent);
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Error al parsear evento JSON: {}", e.getMessage());
                }
                return;
            }
            
            log.warn("⚠️ Tipo de evento no soportado: {}", event.getClass().getName());
            
        } catch (Exception e) {
            log.error("❌ Error al procesar evento en NotificationDispatcher: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica si un Map contiene datos de un evento de alerta
     */
    private boolean isAlertEvent(Map<String, Object> eventMap) {
        String eventType = (String) eventMap.get("eventType");
        String type = (String) eventMap.get("type");
        
        // Verificar que tenga tipo de evento y que sea una alerta
        if (eventType != null) {
            return eventType.toLowerCase().contains("alert") || 
                   eventType.toLowerCase().contains("warning") ||
                   eventType.toLowerCase().contains("detected");
        }
        
        if (type != null) {
            return type.toLowerCase().contains("alert") || 
                   type.toLowerCase().contains("warning") ||
                   type.toLowerCase().contains("detected");
        }
        
        // También verificar si tiene alertId
        return eventMap.containsKey("alertId");
    }

    /**
     * Convierte un Map a AlertEvent
     */
    private AlertEvent convertToAlertEvent(Map<String, Object> eventMap) {
        try {
            AlertEvent alertEvent = new AlertEvent();
            
            // Mapear campos básicos
            alertEvent.setAlertId((String) eventMap.get("alertId"));
            
            // El tipo puede venir en "type" o "eventType"
            String type = (String) eventMap.get("type");
            if (type == null) {
                type = (String) eventMap.get("eventType");
            }
            alertEvent.setType(type);
            
            alertEvent.setSensorId((String) eventMap.get("sensorId"));
            alertEvent.setMessage((String) eventMap.get("message"));
            
            // Mapear valores numéricos con manejo de diferentes tipos
            Object valueObj = eventMap.get("value");
            if (valueObj instanceof Number) {
                alertEvent.setValue(((Number) valueObj).doubleValue());
            } else if (valueObj instanceof String) {
                try {
                    alertEvent.setValue(Double.parseDouble((String) valueObj));
                } catch (NumberFormatException e) {
                    log.warn("⚠️ No se pudo convertir valor a número: {}", valueObj);
                }
            }
            
            Object thresholdObj = eventMap.get("threshold");
            if (thresholdObj instanceof Number) {
                alertEvent.setThreshold(((Number) thresholdObj).doubleValue());
            } else if (thresholdObj instanceof String) {
                try {
                    alertEvent.setThreshold(Double.parseDouble((String) thresholdObj));
                } catch (NumberFormatException e) {
                    log.warn("⚠️ No se pudo convertir threshold a número: {}", thresholdObj);
                }
            }
            
            // Mapear timestamp
            Object timestampObj = eventMap.get("timestamp");
            if (timestampObj instanceof String) {
                try {
                    alertEvent.setTimestamp(java.time.ZonedDateTime.parse((String) timestampObj));
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo parsear timestamp: {}", timestampObj);
                    alertEvent.setTimestamp(java.time.ZonedDateTime.now());
                }
            } else {
                alertEvent.setTimestamp(java.time.ZonedDateTime.now());
            }
            
            // Validar que al menos tenga tipo
            if (alertEvent.getType() == null || alertEvent.getType().isEmpty()) {
                log.warn("⚠️ Evento sin tipo válido, no se puede procesar");
                return null;
            }
            
            return alertEvent;
            
        } catch (Exception e) {
            log.error("❌ Error al convertir Map a AlertEvent: {}", e.getMessage(), e);
            return null;
        }
    }
}
