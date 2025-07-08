package ec.edu.espe.EnvironmentalAnalyzer.service;

import ec.edu.espe.EnvironmentalAnalyzer.dto.AlertEvent;
import ec.edu.espe.EnvironmentalAnalyzer.dto.NewSensorReadingEvent;
import ec.edu.espe.EnvironmentalAnalyzer.entity.Alert;
import ec.edu.espe.EnvironmentalAnalyzer.repository.AlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AnalysisService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Umbrales configurables desde application.yaml
    @Value("${app-config.thresholds.temperature}")
    private double tempThreshold;

    @Value("${app-config.thresholds.humidity}")
    private double humidityThreshold;

    @Value("${app-config.thresholds.seismic}")
    private double seismicThreshold;

    // Tipos de sensores
    @Value("${app-config.sensor-types.temperature}")
    private String temperatureSensorType;

    @Value("${app-config.sensor-types.humidity}")
    private String humiditySensorType;

    @Value("${app-config.sensor-types.seismic}")
    private String seismicSensorType;

    // Tipos de eventos de alerta
    @Value("${app-config.event-types.high-temp-alert}")
    private String highTempAlertType;

    @Value("${app-config.event-types.low-humidity-warning}")
    private String lowHumidityWarningType;

    @Value("${app-config.event-types.seismic-activity-detected}")
    private String seismicActivityDetectedType;

    @Value("${app-config.event-types.daily-report-generated}")
    private String dailyReportGeneratedType;

    @Value("${app-config.event-types.sensor-inactive-alert}")
    private String sensorInactiveAlertType;

    @Value("${app-config.exchanges.global-events}")
    private String globalEventsExchange;
    
    // Constante para evitar duplicación de literales
    private static final String TIMESTAMP_KEY = "timestamp";

    /**
     * Analiza las lecturas de sensores y genera alertas según los umbrales definidos
     * Recibe eventos desde la cola q.events.environmental-analyzer
     */
    public void analyzeSensorReading(NewSensorReadingEvent event) {
        log.info("Analizando lectura del sensor {}: tipo={}, valor={} (desde cola q.events.environmental-analyzer)", 
                event.getSensorId(), event.getType(), event.getValue());

        if (event.getValue() == null) {
            log.warn("Valor nulo recibido para el sensor {}", event.getSensorId());
            return;
        }

        switch (event.getType().toLowerCase()) {
            case "temperature":
                if (event.getValue().doubleValue() > tempThreshold) {
                    createAndPublishAlert(event, highTempAlertType, tempThreshold);
                }
                break;
            case "humidity":
                if (event.getValue().doubleValue() < humidityThreshold) {
                    createAndPublishAlert(event, lowHumidityWarningType, humidityThreshold);
                }
                break;
            case "seismic":
                if (event.getValue().doubleValue() > seismicThreshold) {
                    createAndPublishAlert(event, seismicActivityDetectedType, seismicThreshold);
                }
                break;
            default:
                log.info("Tipo de sensor procesado sin reglas específicas: {}", event.getType());
        }
    }

    /**
     * Crea una alerta, la persiste en la base de datos y publica el evento
     */
    private void createAndPublishAlert(NewSensorReadingEvent reading, String alertType, double threshold) {
        log.warn("¡ALERTA GENERADA! Tipo: {}, Sensor: {}, Valor: {}, Umbral: {}", 
                alertType, reading.getSensorId(), reading.getValue(), threshold);
        
        try {
            // 1. Crear y persistir la alerta en la base de datos
            Alert alert = Alert.builder()
                    .alertId("ALT-" + String.format("%03d", (int)(Math.random() * 1000)))
                    .type(alertType)
                    .sensorId(reading.getSensorId())
                    .value(reading.getValue().doubleValue()) // Convertir BigDecimal a Double para la entidad
                    .threshold(threshold)
                    .timestamp(ZonedDateTime.now())
                    .build();
            
            alertRepository.save(alert);
            log.info("Alerta persistida en la base de datos con ID: {}", alert.getAlertId());

            // 2. Crear el AlertEvent DTO para enviar vía RabbitMQ
            AlertEvent alertEvent = AlertEvent.builder()
                    .alertId(alert.getAlertId())
                    .type(alertType)
                    .sensorId(reading.getSensorId())
                    .value(reading.getValue()) // Mantener BigDecimal en el DTO
                    .threshold(threshold)
                    .timestamp(OffsetDateTime.now()) // Usar OffsetDateTime para compatibilidad
                    .message(String.format("Sensor %s reportó valor %s que excede umbral %s", 
                            reading.getSensorId(), reading.getValue(), threshold))
                    .severity(determineSeverity(alertType))
                    .build();

            // 3. Publicar el AlertEvent DTO al exchange global
            rabbitTemplate.convertAndSend(globalEventsExchange, "", alertEvent);
            log.info("Evento de alerta '{}' publicado en RabbitMQ: {}", alertType, alertEvent);
            
        } catch (Exception e) {
            log.error("Error al procesar alerta para sensor {}: {}", reading.getSensorId(), e.getMessage(), e);
        }
    }

    /**
     * Determina la severidad basada en el tipo de alerta
     */
    private String determineSeverity(String alertType) {
        if (alertType.toLowerCase().contains("seismic")) {
            return "CRITICAL";
        } else if (alertType.toLowerCase().contains("temperature")) {
            return "HIGH";
        } else if (alertType.toLowerCase().contains("warning")) {
            return "MEDIUM";
        }
        return "LOW";
    }
    
    /**
     * Tarea programada para generar reportes diarios
     * Se ejecuta todos los días a medianoche
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyReport() {
        log.info("Iniciando generación de reporte diario...");
        
        try {
            ZonedDateTime yesterday = ZonedDateTime.now().minusDays(1);
            ZonedDateTime today = ZonedDateTime.now();
            
            // Obtener alertas de las últimas 24 horas
            List<Alert> dailyAlerts = alertRepository.findByTimestampBetween(yesterday, today);
            
            // Calcular estadísticas básicas
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("eventType", dailyReportGeneratedType);
            reportData.put("reportDate", today.toLocalDate().toString());
            reportData.put("totalAlerts", dailyAlerts.size());
            reportData.put(TIMESTAMP_KEY, ZonedDateTime.now().toString());
            
            // Contar alertas por tipo
            Map<String, Long> alertsByType = new HashMap<>();
            dailyAlerts.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Alert::getType,
                            java.util.stream.Collectors.counting()))
                    .forEach(alertsByType::put);
            
            reportData.put("alertsByType", alertsByType);
            
            // Publicar evento de reporte generado
            rabbitTemplate.convertAndSend(globalEventsExchange, "", reportData);
            log.info("Reporte diario generado y publicado. Total de alertas: {}", dailyAlerts.size());
            
        } catch (Exception e) {
            log.error("Error al generar reporte diario: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Tarea programada para verificar sensores inactivos
     * Se ejecuta cada 6 horas
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void checkForInactiveSensors() {
        log.info("Verificando sensores inactivos...");
        
        try {
            ZonedDateTime cutoffTime = ZonedDateTime.now().minusHours(24);
            
            // En un escenario real, esto requeriría acceso a la tabla sensor_readings
            // Por ahora, simulamos la verificación de sensores inactivos
            
            // Obtener alertas recientes para identificar sensores activos
            List<Alert> recentAlerts = alertRepository.findRecentAlerts(cutoffTime);
            
            if (recentAlerts.isEmpty()) {
                log.warn("No se encontraron alertas recientes, posibles sensores inactivos");
                
                // Simular evento de sensor inactivo
                Map<String, Object> inactiveAlert = new HashMap<>();
                inactiveAlert.put("eventType", sensorInactiveAlertType);
                inactiveAlert.put("sensorId", "UNKNOWN-SENSORS");
                inactiveAlert.put("lastSeen", cutoffTime.toString());
                inactiveAlert.put(TIMESTAMP_KEY, ZonedDateTime.now().toString());
                inactiveAlert.put("message", "No se detectaron lecturas de sensores en las últimas 24 horas");
                
                rabbitTemplate.convertAndSend(globalEventsExchange, "", inactiveAlert);
                log.info("Evento SensorInactiveAlert emitido para sensores sin actividad reciente.");
            } else {
                log.info("Verificación de sensores inactivos completada. {} sensores con actividad reciente.", 
                        recentAlerts.stream().map(Alert::getSensorId).distinct().count());
            }
            
        } catch (Exception e) {
            log.error("Error al verificar sensores inactivos: {}", e.getMessage(), e);
        }
    }

    /**
     * Método para obtener estadísticas de alertas (útil para endpoints de monitoreo)
     */
    public Map<String, Object> getAlertStatistics() {
        try {
            ZonedDateTime last24Hours = ZonedDateTime.now().minusHours(24);
            List<Alert> recentAlerts = alertRepository.findRecentAlerts(last24Hours);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAlerts", recentAlerts.size());
            stats.put("last24Hours", recentAlerts.size());
            
            // Alertas por tipo
            Map<String, Long> alertsByType = recentAlerts.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Alert::getType,
                            java.util.stream.Collectors.counting()));
            
            stats.put("alertsByType", alertsByType);
            stats.put(TIMESTAMP_KEY, ZonedDateTime.now().toString());
            
            return stats;
        } catch (Exception e) {
            log.error("Error al obtener estadísticas de alertas: {}", e.getMessage(), e);
            return Map.of("error", "Error al obtener estadísticas");
        }
    }
}
