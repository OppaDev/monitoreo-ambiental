package ec.edu.espe.NotificationDispatcher.repository;

import ec.edu.espe.NotificationDispatcher.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la gestión de logs de notificaciones
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    
    /**
     * Encuentra notificaciones por tipo de evento
     */
    List<NotificationLog> findByEventType(String eventType);
    
    /**
     * Encuentra notificaciones por estado
     */
    List<NotificationLog> findByStatus(String status);
    
    /**
     * Encuentra notificaciones por rango de fechas
     */
    List<NotificationLog> findByTimestampBetween(ZonedDateTime startDate, ZonedDateTime endDate);
    
    /**
     * Encuentra notificaciones pendientes de envío
     */
    @Query("SELECT n FROM NotificationLog n WHERE n.status = 'PENDING' AND n.timestamp < :cutoffTime")
    List<NotificationLog> findPendingNotificationsOlderThan(@Param("cutoffTime") ZonedDateTime cutoffTime);
    
    /**
     * Cuenta notificaciones por estado y tipo
     */
    @Query("SELECT COUNT(n) FROM NotificationLog n WHERE n.status = :status AND n.eventType = :eventType")
    Long countByStatusAndEventType(@Param("status") String status, @Param("eventType") String eventType);
}
