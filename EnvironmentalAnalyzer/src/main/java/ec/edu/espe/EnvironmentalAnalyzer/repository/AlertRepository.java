package ec.edu.espe.EnvironmentalAnalyzer.repository;

import ec.edu.espe.EnvironmentalAnalyzer.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
    
    // Método para encontrar alertas por tipo
    List<Alert> findByType(String type);
    
    // Método para encontrar alertas por sensor
    List<Alert> findBySensorId(String sensorId);
    
    // Método para encontrar alertas en un rango de fechas
    @Query("SELECT a FROM Alert a WHERE a.timestamp BETWEEN :startDate AND :endDate")
    List<Alert> findByTimestampBetween(@Param("startDate") ZonedDateTime startDate, 
                                      @Param("endDate") ZonedDateTime endDate);
    
    // Método para encontrar alertas recientes (útil para reportes)
    @Query("SELECT a FROM Alert a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<Alert> findRecentAlerts(@Param("since") ZonedDateTime since);
}
