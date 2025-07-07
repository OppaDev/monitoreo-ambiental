package ec.edu.espe.EnvironmentalAnalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.ZonedDateTime;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @Column(name = "alert_id", nullable = false, unique = true)
    private String alertId;

    @Column(nullable = false)
    private String type; // ej. "HighTemperatureAlert"

    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private Double threshold;

    @Column(nullable = false)
    private ZonedDateTime timestamp;
}
