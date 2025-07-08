package ec.edu.espe.EnvironmentalAnalyzer.listener;

import ec.edu.espe.EnvironmentalAnalyzer.dto.NewSensorReadingEvent;
import ec.edu.espe.EnvironmentalAnalyzer.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventBusListener {

    @Autowired
    private AnalysisService analysisService;

    /**
     * Escucha eventos del bus global desde la cola q.events.environmental-analyzer
     * y procesa los que son relevantes para el análisis
     */
    @RabbitListener(
        queues = "${app-config.queues.analyzer}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleGlobalEvent(NewSensorReadingEvent event) {
        log.info("Evento NewSensorReadingEvent recibido: sensor={}, tipo={}, valor={}", 
                event.getSensorId(), event.getType(), event.getValue());
        
        try {
            analysisService.analyzeSensorReading(event);
            log.debug("Análisis de sensor completado exitosamente para: {}", event.getSensorId());
        } catch (Exception e) {
            log.error("Error al procesar evento de sensor: {}", e.getMessage(), e);
        }
    }
}
