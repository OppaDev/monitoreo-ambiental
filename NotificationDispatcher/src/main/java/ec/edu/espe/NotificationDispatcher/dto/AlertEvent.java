package ec.edu.espe.NotificationDispatcher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO para eventos de alerta recibidos desde el EnvironmentalAnalyzer
 * IMPORTANTE: Esta clase debe ser IDÉNTICA a la del EnvironmentalAnalyzer
 * para que RabbitMQ pueda deserializar correctamente los mensajes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent implements Serializable {
    
    private String alertId;
    private String type; // ej. "HighTemperatureAlert", "SeismicActivityDetected", "LowHumidityWarning"
    private String sensorId;
    private BigDecimal value;      // Valor que disparó la alerta (CAMBIADO: era Double)
    private Double threshold;      // Umbral que se superó
    private OffsetDateTime timestamp; // Momento de la alerta (CAMBIADO: era ZonedDateTime)
    
    // Campos adicionales para mayor contexto
    private String message;
    private String severity;
    
    @Override
    public String toString() {
        return String.format("AlertEvent{id='%s', type='%s', sensor='%s', value=%s, threshold=%s}", 
                alertId, type, sensorId, value, threshold);
    }
}
