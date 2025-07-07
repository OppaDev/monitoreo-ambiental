package ec.edu.espe.SensorDataCollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewSensorReadingEvent implements Serializable {
    // Serializable es importante para que RabbitMQ pueda procesar el objeto.

    private String eventId;
    private String sensorId;
    private String type;
    private BigDecimal value;
    private OffsetDateTime timestamp;
}