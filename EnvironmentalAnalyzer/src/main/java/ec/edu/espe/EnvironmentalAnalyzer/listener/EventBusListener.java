package ec.edu.espe.EnvironmentalAnalyzer.listener;

import ec.edu.espe.EnvironmentalAnalyzer.dto.NewSensorReadingEvent;
import ec.edu.espe.EnvironmentalAnalyzer.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@Slf4j
public class EventBusListener {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Escucha eventos del bus global desde la cola q.events.environmental-analyzer
     * y procesa los que son relevantes para el análisis
     */
    @RabbitListener(queues = "${app-config.queues.analyzer}")
    public void handleGlobalEvent(Object event) {
        log.info("Evento recibido en cola environmental-analyzer: {}", event.getClass().getSimpleName());
        
        try {
            // Si el evento es directamente un NewSensorReadingEvent
            if (event instanceof NewSensorReadingEvent) {
                log.info("Procesando NewSensorReadingEvent directo");
                analysisService.analyzeSensorReading((NewSensorReadingEvent) event);
                return;
            }
            
            // Si el evento viene como Map (JSON deserializado)
            if (event instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> eventMap = (Map<String, Object>) event;
                
                // Verificar si es un evento de lectura de sensor
                if (isNewSensorReadingEvent(eventMap)) {
                    NewSensorReadingEvent sensorEvent = convertToSensorReadingEvent(eventMap);
                    if (sensorEvent != null) {
                        log.info("Procesando NewSensorReadingEvent desde Map: sensor={}, tipo={}, valor={}", 
                                sensorEvent.getSensorId(), sensorEvent.getType(), sensorEvent.getValue());
                        analysisService.analyzeSensorReading(sensorEvent);
                    }
                    return;
                }
                
                // Log de otros tipos de eventos para debug
                String eventType = (String) eventMap.get("eventType");
                if (eventType != null) {
                    log.debug("Evento de sistema recibido: {}", eventType);
                } else {
                    log.debug("Evento Map sin tipo específico recibido");
                }
                return;
            }
            
            // Si el evento viene como String (JSON)
            if (event instanceof String) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> eventMap = objectMapper.readValue((String) event, Map.class);
                    
                    if (isNewSensorReadingEvent(eventMap)) {
                        NewSensorReadingEvent sensorEvent = convertToSensorReadingEvent(eventMap);
                        if (sensorEvent != null) {
                            log.info("Procesando NewSensorReadingEvent desde JSON String");
                            analysisService.analyzeSensorReading(sensorEvent);
                        }
                        return;
                    }
                    
                    String eventType = (String) eventMap.get("eventType");
                    log.debug("Evento JSON recibido: {}", eventType);
                    
                } catch (Exception e) {
                    log.warn("Error al parsear evento JSON: {}", e.getMessage());
                    log.debug("Evento String recibido (no JSON): {}", event);
                }
                return;
            }
            
            // Evento de tipo desconocido
            log.warn("Tipo de evento no reconocido: {}", event.getClass().getName());
            
        } catch (Exception e) {
            log.error("Error al procesar evento: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica si el evento Map corresponde a un NewSensorReadingEvent
     */
    private boolean isNewSensorReadingEvent(Map<String, Object> eventMap) {
        return eventMap.containsKey("sensorId") && 
               eventMap.containsKey("type") && 
               eventMap.containsKey("value") &&
               (eventMap.containsKey("timestamp") || eventMap.containsKey("readingTime"));
    }

    /**
     * Convierte un Map a NewSensorReadingEvent
     */
    private NewSensorReadingEvent convertToSensorReadingEvent(Map<String, Object> eventMap) {
        try {
            NewSensorReadingEvent event = new NewSensorReadingEvent();
            
            event.setEventId((String) eventMap.get("eventId"));
            event.setSensorId((String) eventMap.get("sensorId"));
            event.setType((String) eventMap.get("type"));
            
            // Manejar el valor que puede venir como diferentes tipos numéricos
            Object valueObj = eventMap.get("value");
            if (valueObj instanceof Number) {
                event.setValue(((Number) valueObj).doubleValue());
            } else if (valueObj instanceof String) {
                try {
                    event.setValue(Double.parseDouble((String) valueObj));
                } catch (NumberFormatException e) {
                    log.warn("No se pudo convertir valor a Double: {}", valueObj);
                    return null;
                }
            }
            
            // Manejar timestamp - puede venir con diferentes nombres
            Object timestampObj = eventMap.get("timestamp");
            if (timestampObj == null) {
                timestampObj = eventMap.get("readingTime");
            }
            
            if (timestampObj instanceof String) {
                try {
                    event.setTimestamp(java.time.ZonedDateTime.parse((String) timestampObj));
                } catch (Exception e) {
                    log.warn("Error al parsear timestamp, usando tiempo actual: {}", e.getMessage());
                    event.setTimestamp(java.time.ZonedDateTime.now());
                }
            } else {
                event.setTimestamp(java.time.ZonedDateTime.now());
            }
            
            return event;
            
        } catch (Exception e) {
            log.error("Error al convertir Map a NewSensorReadingEvent: {}", e.getMessage(), e);
            return null;
        }
    }
}
