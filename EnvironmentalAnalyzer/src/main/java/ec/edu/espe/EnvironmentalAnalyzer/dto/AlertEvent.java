package ec.edu.espe.EnvironmentalAnalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO para eventos de alerta generados por el EnvironmentalAnalyzer
 * Este DTO será enviado vía RabbitMQ al NotificationDispatcher
 * IMPORTANTE: Debe coincidir con el AlertEvent del NotificationDispatcher
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent implements Serializable {
    
    private String alertId;
    private String type; // ej. "HighTemperatureAlert", "SeismicActivityDetected", "LowHumidityWarning"
    private String sensorId;
    private BigDecimal value;      // Valor que disparó la alerta
    private Double threshold;      // Umbral que se superó
    private OffsetDateTime timestamp; // Momento de la alerta
    
    // Campos adicionales para mayor contexto
    private String message;
    private String severity;
    
    @Override
    public String toString() {
        return String.format("AlertEvent{id='%s', type='%s', sensor='%s', value=%s, threshold=%s}", 
                alertId, type, sensorId, value, threshold);
    }
}
