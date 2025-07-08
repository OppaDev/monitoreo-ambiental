package ec.edu.espe.NotificationDispatcher.service;

import ec.edu.espe.NotificationDispatcher.dto.AlertEvent;
import ec.edu.espe.NotificationDispatcher.model.NotificationLog;
import ec.edu.espe.NotificationDispatcher.repository.NotificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Servicio principal del NotificationDispatcher
 * Maneja la clasificación, envío y persistencia de notificaciones
 */
@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Configuración externalizada para keywords de prioridad
    @Value("${app-config.alert-priorities.critical-keywords}")
    private String criticalKeywordsString;

    @Value("${app-config.alert-priorities.warning-keywords}")
    private String warningKeywordsString;

    // Configuración de canales de notificación
    @Value("${app-config.notification-channels.email}")
    private String emailChannel;

    @Value("${app-config.notification-channels.sms}")
    private String smsChannel;

    @Value("${app-config.notification-channels.push}")
    private String pushChannel;

    // Estados de notificación
    @Value("${app-config.statuses.sent}")
    private String statusSent;

    @Value("${app-config.statuses.failed}")
    private String statusFailed;

    @Value("${app-config.statuses.pending}")
    private String statusPending;

    // Destinatarios configurables
    @Value("${notification-dispatcher.recipients.email}")
    private String defaultEmailRecipient;

    @Value("${notification-dispatcher.recipients.sms}")
    private String defaultSmsRecipient;

    // Cola en memoria para agrupar notificaciones de baja prioridad
    private final ConcurrentLinkedQueue<AlertEvent> lowPriorityAlertsQueue = new ConcurrentLinkedQueue<>();

    /**
     * Procesa un evento de alerta recibido desde RabbitMQ
     */
    public void processAlert(AlertEvent event) {
        AlertPriority priority = classifyAlert(event.getType());
        log.info("Alerta recibida: id={}, tipo={}, prioridad={}, sensor={}", 
                event.getAlertId(), event.getType(), priority, event.getSensorId());

        if (priority == AlertPriority.CRITICAL) {
            dispatchNotificationsImmediately(event);
        } else {
            lowPriorityAlertsQueue.add(event);
            log.info("Alerta de {} prioridad encolada. Tamaño actual de la cola: {}", 
                    priority.name().toLowerCase(), lowPriorityAlertsQueue.size());
        }
    }

    /**
     * Envía notificaciones críticas inmediatamente por todos los canales
     */
    private void dispatchNotificationsImmediately(AlertEvent event) {
        log.warn("🚨 DESPACHANDO NOTIFICACIONES CRÍTICAS para la alerta: {}", event.getAlertId());
        
        // Envío paralelo por todos los canales
        sendEmailNotification(event, AlertPriority.CRITICAL);
        sendSmsNotification(event, AlertPriority.CRITICAL);
        sendPushNotification(event, AlertPriority.CRITICAL);
    }

    /**
     * Tarea programada para enviar alertas de baja prioridad agrupadas
     * Se ejecuta cada 30 minutos según configuración
     */
    @Scheduled(cron = "${notification-dispatcher.scheduling.low-priority-dispatch}")
    public void dispatchLowPriorityNotifications() {
        if (lowPriorityAlertsQueue.isEmpty()) {
            log.debug("No hay alertas de baja prioridad pendientes de envío");
            return;
        }

        log.info("📦 Despachando {} alertas de baja prioridad agrupadas...", lowPriorityAlertsQueue.size());
        List<AlertEvent> alertsToProcess = new ArrayList<>(lowPriorityAlertsQueue);
        lowPriorityAlertsQueue.clear();

        for (AlertEvent event : alertsToProcess) {
            AlertPriority priority = classifyAlert(event.getType());
            sendEmailNotification(event, priority);
            sendSmsNotification(event, priority);
            sendPushNotification(event, priority);
        }
        
        log.info("✅ Completado el envío de alertas de baja prioridad");
    }

    /**
     * Simulación de envío de notificación por correo electrónico
     */
    private void sendEmailNotification(AlertEvent event, AlertPriority priority) {
        try {
            log.info("📧 [SIMULACIÓN CORREO] Enviando a '{}': {} - Sensor {} reportó valor {}", 
                    defaultEmailRecipient, event.getType(), event.getSensorId(), event.getValue());
            
            saveNotificationLog(event, emailChannel, statusSent, defaultEmailRecipient, priority);
            
        } catch (Exception e) {
            log.error("❌ Error al enviar notificación por email: {}", e.getMessage());
            saveNotificationLog(event, emailChannel, statusFailed, defaultEmailRecipient, priority, e.getMessage());
        }
    }

    /**
     * Simulación de envío de notificación por SMS
     */
    private void sendSmsNotification(AlertEvent event, AlertPriority priority) {
        try {
            log.info("📱 [SIMULACIÓN SMS] Enviando a '{}': Alerta: {}, Sensor: {}, Valor: {}", 
                    defaultSmsRecipient, event.getType(), event.getSensorId(), event.getValue());
            
            saveNotificationLog(event, smsChannel, statusSent, defaultSmsRecipient, priority);
            
        } catch (Exception e) {
            log.error("❌ Error al enviar notificación por SMS: {}", e.getMessage());
            saveNotificationLog(event, smsChannel, statusFailed, defaultSmsRecipient, priority, e.getMessage());
        }
    }

    /**
     * Simulación de envío de notificación push
     */
    private void sendPushNotification(AlertEvent event, AlertPriority priority) {
        try {
            System.out.println("🔔 ================= PUSH NOTIFICATION ==================");
            System.out.printf("📢 ALERTA %s: %s\n", priority.name(), event.getType());
            System.out.printf("🌡️  Sensor: %s\n", event.getSensorId());
            System.out.printf("📊 Valor: %s (Umbral: %s)\n", event.getValue(), event.getThreshold());
            System.out.printf("⏰ Timestamp: %s\n", event.getTimestamp());
            System.out.println("======================================================");
            
            saveNotificationLog(event, pushChannel, statusSent, "mobile-app", priority);
            
        } catch (Exception e) {
            log.error("❌ Error al enviar notificación push: {}", e.getMessage());
            saveNotificationLog(event, pushChannel, statusFailed, "mobile-app", priority, e.getMessage());
        }
    }

    /**
     * Guarda el log de notificación en la base de datos
     */
    private void saveNotificationLog(AlertEvent event, String channel, String status, 
                                    String recipient, AlertPriority priority) {
        saveNotificationLog(event, channel, status, recipient, priority, null);
    }

    /**
     * Guarda el log de notificación en la base de datos con mensaje de error opcional
     */
    private void saveNotificationLog(AlertEvent event, String channel, String status, 
                                    String recipient, AlertPriority priority, String errorMessage) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            ZonedDateTime now = ZonedDateTime.now();
            
            NotificationLog logEntry = NotificationLog.builder()
                    .eventType(event.getType())
                    .sensorId(event.getSensorId())
                    .alertId(event.getAlertId())
                    .recipient(channel)
                    .recipientAddress(recipient)
                    .status(status)
                    .priority(priority.name())
                    .timestamp(now)
                    .sentAt(statusSent.equals(status) ? now : null)
                    .payload(payload)
                    .errorMessage(errorMessage)
                    .build();
                    
            notificationLogRepository.save(logEntry);
            log.debug("💾 Log de notificación guardado: {} -> {} [{}]", channel, status, event.getAlertId());
            
        } catch (Exception e) {
            log.error("❌ Error al guardar log de notificación: {}", e.getMessage(), e);
        }
    }

    /**
     * Clasifica la prioridad de una alerta basándose en su tipo
     */
    private AlertPriority classifyAlert(String alertType) {
        if (alertType == null) {
            return AlertPriority.INFO;
        }
        
        String typeLower = alertType.toLowerCase();
        
        // Convertir cadenas de configuración a listas
        List<String> criticalKeywords = Arrays.asList(criticalKeywordsString.split(","));
        List<String> warningKeywords = Arrays.asList(warningKeywordsString.split(","));
        
        // Verificar palabras clave críticas
        for (String keyword : criticalKeywords) {
            if (typeLower.contains(keyword.trim().toLowerCase())) {
                return AlertPriority.CRITICAL;
            }
        }
        
        // Verificar palabras clave de advertencia
        for (String keyword : warningKeywords) {
            if (typeLower.contains(keyword.trim().toLowerCase())) {
                return AlertPriority.WARNING;
            }
        }
        
        return AlertPriority.INFO;
    }

    /**
     * Enumeración para los niveles de prioridad de alertas
     */
    private enum AlertPriority {
        CRITICAL, WARNING, INFO
    }

    /**
     * Obtiene estadísticas del servicio de notificaciones
     */
    public NotificationStats getNotificationStats() {
        ZonedDateTime last24Hours = ZonedDateTime.now().minusDays(1);
        List<NotificationLog> recentNotifications = notificationLogRepository
                .findByTimestampBetween(last24Hours, ZonedDateTime.now());
        
        long sentCount = recentNotifications.stream()
                .filter(n -> statusSent.equals(n.getStatus()))
                .count();
        
        long failedCount = recentNotifications.stream()
                .filter(n -> statusFailed.equals(n.getStatus()))
                .count();
        
        return new NotificationStats(
                recentNotifications.size(),
                sentCount,
                failedCount,
                lowPriorityAlertsQueue.size()
        );
    }

    /**
     * Clase interna para estadísticas de notificaciones
     */
    public static class NotificationStats {
        public final long totalNotifications;
        public final long sentNotifications;
        public final long failedNotifications;
        public final long queuedNotifications;

        public NotificationStats(long total, long sent, long failed, long queued) {
            this.totalNotifications = total;
            this.sentNotifications = sent;
            this.failedNotifications = failed;
            this.queuedNotifications = queued;
        }
    }
}
