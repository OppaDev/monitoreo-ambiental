package ec.edu.espe.NotificationDispatcher.controller;

import ec.edu.espe.NotificationDispatcher.model.NotificationLog;
import ec.edu.espe.NotificationDispatcher.repository.NotificationLogRepository;
import ec.edu.espe.NotificationDispatcher.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador REST para gestionar las notificaciones del sistema
 * Proporciona endpoints para consultar logs y estadísticas
 */
@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Obtiene todas las notificaciones con paginación
     */
    @GetMapping
    public ResponseEntity<Page<NotificationLog>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<NotificationLog> notifications = notificationLogRepository.findAll(pageable);
            
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener notificaciones: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene una notificación específica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationLog> getNotificationById(@PathVariable UUID id) {
        try {
            Optional<NotificationLog> notification = notificationLogRepository.findById(id);
            
            if (notification.isPresent()) {
                return ResponseEntity.ok(notification.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("❌ Error al obtener notificación {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene notificaciones por tipo de evento
     */
    @GetMapping("/by-type/{eventType}")
    public ResponseEntity<List<NotificationLog>> getNotificationsByType(@PathVariable String eventType) {
        try {
            List<NotificationLog> notifications = notificationLogRepository.findByEventType(eventType);
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener notificaciones por tipo {}: {}", eventType, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene notificaciones por estado
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<NotificationLog>> getNotificationsByStatus(@PathVariable String status) {
        try {
            List<NotificationLog> notifications = notificationLogRepository.findByStatus(status);
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener notificaciones por estado {}: {}", status, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene notificaciones en un rango de fechas
     */
    @GetMapping("/by-date-range")
    public ResponseEntity<List<NotificationLog>> getNotificationsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        try {
            ZonedDateTime start = ZonedDateTime.parse(startDate);
            ZonedDateTime end = ZonedDateTime.parse(endDate);
            
            List<NotificationLog> notifications = notificationLogRepository
                    .findByTimestampBetween(start, end);
            
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener notificaciones por rango de fechas: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtiene estadísticas detalladas del servicio
     */
    @GetMapping("/stats/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedStats() {
        try {
            NotificationService.NotificationStats stats = notificationService.getNotificationStats();
            
            // Estadísticas adicionales de la base de datos
            long totalInDB = notificationLogRepository.count();
            long sentCount = notificationLogRepository.countByStatusAndEventType("SENT", "");
            long failedCount = notificationLogRepository.countByStatusAndEventType("FAILED", "");
            
            Map<String, Object> detailedStats = new HashMap<>();
            detailedStats.put("last24Hours", Map.of(
                "total", stats.totalNotifications,
                "sent", stats.sentNotifications,
                "failed", stats.failedNotifications
            ));
            detailedStats.put("database", Map.of(
                "totalRecords", totalInDB
            ));
            detailedStats.put("queue", Map.of(
                "pendingLowPriority", stats.queuedNotifications
            ));
            detailedStats.put("timestamp", ZonedDateTime.now().toString());
            
            return ResponseEntity.ok(detailedStats);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener estadísticas detalladas: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint de salud del servicio de notificaciones
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "notification-dispatcher");
            health.put("timestamp", ZonedDateTime.now().toString());
            
            // Verificar conectividad a la base de datos
            long recordCount = notificationLogRepository.count();
            health.put("database", Map.of(
                "status", "UP",
                "recordCount", recordCount
            ));
            
            // Verificar estado de la cola
            NotificationService.NotificationStats stats = notificationService.getNotificationStats();
            health.put("queue", Map.of(
                "status", "UP",
                "pendingItems", stats.queuedNotifications
            ));
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("❌ Error en health check: {}", e.getMessage());
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", ZonedDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(health);
        }
    }
}
