package ec.edu.espe.SensorDataCollector.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class SensorReadingRequest {

    @NotBlank(message = "El ID del sensor no puede estar vacío.")
    @Size(min = 1, max = 50, message = "El ID del sensor debe tener entre 1 y 50 caracteres.")
    private String sensorId;

    @NotBlank(message = "El tipo de lectura no puede estar vacío.")
    private String type;

    @NotNull(message = "El valor no puede ser nulo.")
    @DecimalMin(value = "-100.0", message = "El valor no puede ser menor a -100.")
    @DecimalMax(value = "100.0", message = "El valor no puede ser mayor a 100.") // Rango de validación genérico
    private BigDecimal value;

    @NotNull(message = "El timestamp no puede ser nulo.")
    @PastOrPresent(message = "El timestamp no puede ser una fecha futura.")
    private OffsetDateTime timestamp;
}