package ec.edu.espe.EnvironmentalAnalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO para eventos de nueva lectura de sensor
 * IMPORTANTE: Esta clase debe ser IDÉNTICA a la del SensorDataCollector
 * para que RabbitMQ pueda deserializar correctamente los mensajes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewSensorReadingEvent implements Serializable {
    // Serializable es importante para que RabbitMQ pueda procesar el objeto.

    private String eventId;
    private String sensorId;
    private String type;
    private BigDecimal value;      // CAMBIADO: Era Double, ahora BigDecimal
    private OffsetDateTime timestamp; // CAMBIADO: Era ZonedDateTime, ahora OffsetDateTime
}
