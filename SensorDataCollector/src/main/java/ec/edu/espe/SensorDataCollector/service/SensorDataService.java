package ec.edu.espe.SensorDataCollector.service;

import ec.edu.espe.SensorDataCollector.dto.NewSensorReadingEvent;
import ec.edu.espe.SensorDataCollector.dto.SensorReadingRequest;
import ec.edu.espe.SensorDataCollector.model.SensorReading;
import ec.edu.espe.SensorDataCollector.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Inyección de dependencias por constructor (Lombok)
@Slf4j // Logger de SLF4J (Lombok)
public class SensorDataService {

    // Inyección de dependencias. @RequiredArgsConstructor crea el constructor.
    private final SensorReadingRepository sensorReadingRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange-name}")
    private String exchangeName;

    // Límite de validación para rechazar valores absurdos
    private static final BigDecimal MAX_TEMP_VALUE = new BigDecimal("60.0");
    private static final BigDecimal MIN_TEMP_VALUE = new BigDecimal("-40.0");

    @Transactional // Asegura que guardar en DB y enviar a RabbitMQ sea atómico (o casi)
    public SensorReading processAndSaveReading(SensorReadingRequest request) {
        // 1. Validar la lógica de negocio (más allá de las anotaciones del DTO)
        if ("temperature".equalsIgnoreCase(request.getType())) {
            if (request.getValue().compareTo(MAX_TEMP_VALUE) > 0 || request.getValue().compareTo(MIN_TEMP_VALUE) < 0) {
                log.warn("Lectura de temperatura fuera de rango rechazada: {}", request.getValue());
                // En un caso real, podrías lanzar una excepción personalizada aquí
                throw new IllegalArgumentException("Temperatura fuera del rango aceptable (-40°C a 60°C).");
            }
        }

        // 2. Mapear del DTO a la Entidad del modelo
        SensorReading reading = new SensorReading();
        reading.setSensorId(request.getSensorId());
        reading.setType(request.getType());
        reading.setValue(request.getValue());
        reading.setTimestamp(request.getTimestamp());

        // 3. Guardar en la base de datos
        SensorReading savedReading = sensorReadingRepository.save(reading);
        log.info("Lectura de sensor guardada con ID: {}", savedReading.getId());

        // 4. Crear y publicar el evento en RabbitMQ
        publishNewReadingEvent(savedReading);

        return savedReading;
    }

    private void publishNewReadingEvent(SensorReading reading) {
        NewSensorReadingEvent event = new NewSensorReadingEvent(
                "EVT-" + UUID.randomUUID().toString(),
                reading.getSensorId(),
                reading.getType(),
                reading.getValue(),
                reading.getTimestamp()
        );

        try {
            // El routingKey es ignorado por un exchange Fanout, pero es buena práctica no dejarlo vacío.
            rabbitTemplate.convertAndSend(exchangeName, "", event);
            log.info("Evento NewSensorReadingEvent publicado para el sensor ID: {}", event.getSensorId());
        } catch (Exception e) {
            log.error("Error al publicar evento en RabbitMQ para sensor ID: {}. Error: {}", event.getSensorId(), e.getMessage());
            // Aquí iría la lógica de resiliencia: guardar el evento localmente en SQLite.
            // Lo implementaremos en un paso posterior para no complicar este.
            throw new RuntimeException("Fallo al publicar evento", e); // Re-lanzar para que la transacción haga rollback
        }
    }

    public List<SensorReading> getReadingsBySensorId(String sensorId) {
        log.debug("Buscando lecturas para el sensor ID: {}", sensorId);
        return sensorReadingRepository.findBySensorIdOrderByTimestampDesc(sensorId);
    }
}