package ec.edu.espe.SensorDataCollector.repository;

import ec.edu.espe.SensorDataCollector.model.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, UUID> {

    /**
     * Busca todas las lecturas de un sensor específico, ordenadas por timestamp descendente.
     * Spring Data JPA creará la implementación de este método automáticamente a partir de su nombre.
     * @param sensorId El ID del sensor.
     * @return Una lista de lecturas del sensor.
     */
    List<SensorReading> findBySensorIdOrderByTimestampDesc(String sensorId);
}