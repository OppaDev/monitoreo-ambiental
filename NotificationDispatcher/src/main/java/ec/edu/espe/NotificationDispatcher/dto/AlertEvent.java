package ec.edu.espe.NotificationDispatcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

/**
 * DTO para eventos de alerta recibidos desde el EnvironmentalAnalyzer
 * Representa cualquier tipo de alerta del sistema de monitoreo ambiental
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Ignora campos extra que no mapeamos
public class AlertEvent {
    
    private String alertId;
    private String type; // ej. "HighTemperatureAlert", "SeismicActivityDetected", "LowHumidityWarning"
    private String sensorId;
    private Double value;
    private Double threshold;
    private ZonedDateTime timestamp;
    
    // Campos adicionales que pueden venir en algunos eventos
    private String message;
    private String severity;
    
    @Override
    public String toString() {
        return String.format("AlertEvent{id='%s', type='%s', sensor='%s', value=%s, threshold=%s}", 
                alertId, type, sensorId, value, threshold);
    }
}
