package ec.edu.espe.SensorDataCollector.controller;

import ec.edu.espe.SensorDataCollector.dto.SensorReadingRequest;
import ec.edu.espe.SensorDataCollector.model.SensorReading;
import ec.edu.espe.SensorDataCollector.service.SensorDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sensor-readings") // Ruta base para todos los endpoints de este controlador
@RequiredArgsConstructor
public class SensorDataController {

    private final SensorDataService sensorDataService;

    @PostMapping
    public ResponseEntity<String> receiveSensorReading(@Valid @RequestBody SensorReadingRequest request) {
        try {
            SensorReading savedReading = sensorDataService.processAndSaveReading(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Lectura recibida y procesada. ID: " + savedReading.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Datos inválidos: " + e.getMessage());
        } catch (Exception e) {
            // Captura errores generales, como el fallo de publicación en RabbitMQ
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno al procesar la solicitud: " + e.getMessage());
        }
    }

    @GetMapping("/{sensorId}")
    public ResponseEntity<List<SensorReading>> getSensorReadings(@PathVariable String sensorId) {
        List<SensorReading> readings = sensorDataService.getReadingsBySensorId(sensorId);
        if (readings.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(readings);
    }
}