package ec.edu.espe.NotificationDispatcher.controller;

import ec.edu.espe.NotificationDispatcher.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador que simula servicios externos de notificación
 * Proporciona endpoints mock para email, SMS y estadísticas
 */
@RestController
@RequestMapping("/mock")
@Slf4j
public class MockExternalApiController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Endpoint mock para simular la recepción de emails por un servicio externo
     */
    @PostMapping("/email")
    public ResponseEntity<Map<String, Object>> receiveMockEmail(@RequestBody Map<String, Object> payload) {
        log.info("📧 [MOCK EMAIL SERVICE] Correo recibido en servicio externo: {}", payload);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Mock email service: Notificación recibida con éxito");
        response.put("timestamp", ZonedDateTime.now().toString());
        response.put("service", "external-email-provider");
        response.put("messageId", "email-" + System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint mock para simular la recepción de SMS por un servicio externo
     */
    @PostMapping("/sms")
    public ResponseEntity<Map<String, Object>> receiveMockSms(@RequestBody Map<String, Object> payload) {
        log.info("📱 [MOCK SMS SERVICE] SMS recibido en servicio externo: {}", payload);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Mock SMS service: Notificación recibida con éxito");
        response.put("timestamp", ZonedDateTime.now().toString());
        response.put("service", "external-sms-provider");
        response.put("messageId", "sms-" + System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener estadísticas del servicio de notificaciones
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        try {
            NotificationService.NotificationStats stats = notificationService.getNotificationStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalNotifications24h", stats.totalNotifications);
            response.put("sentNotifications24h", stats.sentNotifications);
            response.put("failedNotifications24h", stats.failedNotifications);
            response.put("queuedNotifications", stats.queuedNotifications);
            response.put("timestamp", ZonedDateTime.now().toString());
            response.put("service", "notification-dispatcher");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener estadísticas: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al obtener estadísticas: " + e.getMessage());
            errorResponse.put("timestamp", ZonedDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Endpoint de salud del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "notification-dispatcher");
        health.put("timestamp", ZonedDateTime.now().toString());
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Endpoint para simular webhook de confirmación de entrega
     */
    @PostMapping("/delivery-confirmation")
    public ResponseEntity<Map<String, Object>> deliveryConfirmation(@RequestBody Map<String, Object> payload) {
        log.info("✅ [DELIVERY CONFIRMATION] Confirmación de entrega recibida: {}", payload);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "acknowledged");
        response.put("message", "Delivery confirmation processed successfully");
        response.put("timestamp", ZonedDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
