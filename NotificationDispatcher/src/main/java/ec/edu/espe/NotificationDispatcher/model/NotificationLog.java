package ec.edu.espe.NotificationDispatcher.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entidad que representa el log de notificaciones enviadas
 * Se persiste en la tabla 'notifications' de CockroachDB
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "sensor_id")
    private String sensorId;

    @Column(name = "alert_id")
    private String alertId;

    @Column(nullable = false)
    private String recipient; // "email", "sms", "push"

    @Column(nullable = false)
    private String status; // "SENT", "FAILED", "PENDING"

    @Column(name = "recipient_address")
    private String recipientAddress; // Email address, phone number, etc.

    @Column(nullable = false)
    private ZonedDateTime timestamp;

    @Column(name = "sent_at")
    private ZonedDateTime sentAt;

    @Lob // Para guardar el contenido completo del evento
    @Column(name = "payload")
    private String payload;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "priority")
    private String priority; // "CRITICAL", "WARNING", "INFO"
}
