package ec.edu.espe.EnvironmentalAnalyzer.controller;

import ec.edu.espe.EnvironmentalAnalyzer.entity.Alert;
import ec.edu.espe.EnvironmentalAnalyzer.repository.AlertRepository;
import ec.edu.espe.EnvironmentalAnalyzer.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("analyzer")
@Slf4j
public class AnalyzerController {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private AlertRepository alertRepository;

    /**
     * Endpoint de salud del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "EnvironmentalAnalyzer",
            "timestamp", ZonedDateTime.now().toString()
        );
        return ResponseEntity.ok(health);
    }

    /**
     * Obtener estadísticas de alertas
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = analysisService.getAlertStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    /**
     * Obtener alertas recientes
     */
    @GetMapping("/alerts/recent")
    public ResponseEntity<List<Alert>> getRecentAlerts(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            ZonedDateTime since = ZonedDateTime.now().minusHours(hours);
            
            // Crear PageRequest para limitar resultados
            PageRequest pageRequest = PageRequest.of(0, limit, 
                    Sort.by(Sort.Direction.DESC, "timestamp"));
            
            List<Alert> alerts = alertRepository.findRecentAlerts(since);
            
            // Limitar manualmente los resultados (en un caso real usarías Pageable en el repository)
            List<Alert> limitedAlerts = alerts.stream()
                    .limit(limit)
                    .toList();
            
            return ResponseEntity.ok(limitedAlerts);
        } catch (Exception e) {
            log.error("Error al obtener alertas recientes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener alertas por tipo
     */
    @GetMapping("/alerts/type/{type}")
    public ResponseEntity<List<Alert>> getAlertsByType(@PathVariable String type) {
        try {
            List<Alert> alerts = alertRepository.findByType(type);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error al obtener alertas por tipo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener alertas por sensor
     */
    @GetMapping("/alerts/sensor/{sensorId}")
    public ResponseEntity<List<Alert>> getAlertsBySensor(@PathVariable String sensorId) {
        try {
            List<Alert> alerts = alertRepository.findBySensorId(sensorId);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error al obtener alertas por sensor: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener todas las alertas (con paginación básica)
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size, 
                    Sort.by(Sort.Direction.DESC, "timestamp"));
            
            List<Alert> alerts = alertRepository.findAll(pageRequest).getContent();
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error al obtener todas las alertas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint para obtener información del servicio
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = Map.of(
            "serviceName", "EnvironmentalAnalyzer",
            "version", "1.0.0",
            "description", "Analizar datos de sensores y generar alertas basadas en umbrales",
            "thresholds", Map.of(
                "temperature", "> 40°C",
                "humidity", "< 20%",
                "seismic", "> 3.0 Richter"
            ),
            "scheduledTasks", Map.of(
                "dailyReport", "Cada día a medianoche",
                "inactiveSensors", "Cada 6 horas"
            ),
            "timestamp", ZonedDateTime.now().toString()
        );
        return ResponseEntity.ok(info);
    }
}
