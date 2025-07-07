# Environmental Analyzer - Microservicio de Análisis Ambiental

## Descripción

El microservicio `EnvironmentalAnalyzer` es responsable de analizar los datos de sensores ambientales y generar alertas basadas en umbrales predefinidos. Forma parte del sistema de monitoreo ambiental distribuido.

## Funcionalidades Principales

### 🔍 Análisis de Lecturas de Sensores
- **Escucha eventos**: Recibe `NewSensorReadingEvent` desde RabbitMQ
- **Aplica reglas de negocio**:
  - Temperatura alta: > 40°C → `HighTemperatureAlert`
  - Humedad baja: < 20% → `LowHumidityWarning`
  - Actividad sísmica: > 3.0 Richter → `SeismicActivityDetected`

### 💾 Persistencia de Alertas
- Almacena alertas en CockroachDB (tabla `alerts`)
- Campos: `alert_id`, `type`, `sensor_id`, `value`, `threshold`, `timestamp`

### 📡 Publicación de Eventos
- Publica eventos de alerta al bus global de RabbitMQ
- Formato según especificaciones del documento

### ⏰ Tareas Programadas
- **Reportes diarios**: Cada día a medianoche (00:00)
- **Verificación de sensores inactivos**: Cada 6 horas

## Arquitectura

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ SensorCollector │───▶│  RabbitMQ Bus    │───▶│ Environmental   │
│                 │    │ (global-events)  │    │ Analyzer        │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
                              ┌─────────────────┐    ┌─────────────────┐
                              │ CockroachDB     │◀───│ Alert Repository│
                              │ (alerts table)  │    │                 │
                              └─────────────────┘    └─────────────────┘
```

## Requisitos

### Software Necesario
- **Java 17+**
- **Maven 3.6+**
- **CockroachDB** (puerto 26257)
- **RabbitMQ** (puerto 5672)
- **Eureka Server** (puerto 8761)

### Dependencias
- Spring Boot 3.5.3
- Spring Cloud 2025.0.0
- Spring Data JPA
- Spring AMQP (RabbitMQ)
- Eureka Client
- PostgreSQL Driver (para CockroachDB)
- Lombok

## Configuración

### Base de Datos
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:26257/defaultdb?sslmode=disable
    username: root
    password:
```

### RabbitMQ
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin
```

### Eureka
```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

## Ejecución

### 1. Preparar el entorno
```bash
# Iniciar CockroachDB
cockroach start-single-node --insecure --host=localhost --port=26257

# Iniciar RabbitMQ
rabbitmq-server

# Iniciar Eureka Server (en otro proyecto)
```

### 2. Compilar y ejecutar
```bash
# Compilar el proyecto
mvn clean compile

# Ejecutar el microservicio
mvn spring-boot:run
```

### 3. Verificar funcionamiento
```bash
# Verificar registro en Eureka
curl http://localhost:8761/eureka/apps

# Verificar salud del servicio
curl http://localhost:8082/api/analyzer/health

# Ver estadísticas de alertas
curl http://localhost:8082/api/analyzer/statistics
```

## API Endpoints

| Endpoint | Método | Descripción |
|----------|---------|-------------|
| `/api/analyzer/health` | GET | Estado del servicio |
| `/api/analyzer/statistics` | GET | Estadísticas de alertas |
| `/api/analyzer/alerts/recent` | GET | Alertas recientes |
| `/api/analyzer/alerts/type/{type}` | GET | Alertas por tipo |
| `/api/analyzer/alerts/sensor/{sensorId}` | GET | Alertas por sensor |
| `/api/analyzer/info` | GET | Información del servicio |

## Estructura del Proyecto

```
src/main/java/ec/edu/espe/EnvironmentalAnalyzer/
├── EnvironmentalAnalyzerApplication.java  # Clase principal
├── config/
│   ├── RabbitMQConfig.java               # Configuración RabbitMQ
│   └── JacksonConfig.java                # Configuración JSON
├── controller/
│   └── AnalyzerController.java           # Endpoints REST
├── dto/
│   └── NewSensorReadingEvent.java        # DTO eventos entrada
├── entity/
│   └── Alert.java                        # Entidad alertas
├── listener/
│   └── EventBusListener.java             # Listener RabbitMQ
├── repository/
│   └── AlertRepository.java              # Repositorio alertas
└── service/
    └── AnalysisService.java              # Lógica de negocio
```

## Eventos

### Eventos Entrantes
```json
{
  "eventId": "EVT-001",
  "sensorId": "S001",
  "type": "temperature",
  "value": 42.0,
  "timestamp": "2024-04-05T12:06:00Z"
}
```

### Eventos Salientes (Alertas)
```json
{
  "alertId": "ALT-001",
  "type": "HighTemperatureAlert",
  "sensorId": "S001",
  "value": 42.0,
  "threshold": 40.0,
  "timestamp": "2024-04-05T12:06:00Z"
}
```

## Monitoreo y Logs

### Logs importantes
```bash
# Ver logs en tiempo real
tail -f logs/environmental-analyzer.log

# Logs de análisis
grep "Analizando lectura" logs/environmental-analyzer.log

# Logs de alertas generadas
grep "ALERTA GENERADA" logs/environmental-analyzer.log
```

### Métricas disponibles
- `/actuator/health` - Estado del servicio
- `/actuator/metrics` - Métricas de la aplicación
- `/actuator/info` - Información del servicio

## Troubleshooting

### Problemas Comunes

1. **Error de conexión a CockroachDB**
   ```bash
   # Verificar que CockroachDB esté ejecutándose
   ps aux | grep cockroach
   ```

2. **Error de conexión a RabbitMQ**
   ```bash
   # Verificar estado de RabbitMQ
   rabbitmqctl status
   ```

3. **No se registra en Eureka**
   ```bash
   # Verificar configuración de red
   curl http://localhost:8761/
   ```

## Desarrollo

### Agregar nuevos tipos de sensores
1. Modificar `AnalysisService.analyzeSensorReading()`
2. Agregar nuevos umbrales en configuración
3. Definir nuevos tipos de alertas

### Modificar umbrales
```yaml
environmental-analyzer:
  thresholds:
    temperature: 45.0  # Cambiar umbral
    newSensorType: 10.0  # Agregar nuevo
```

## Testing

```bash
# Ejecutar tests unitarios
mvn test

# Ejecutar tests de integración
mvn verify

# Generar reporte de cobertura
mvn jacoco:report
```

## Contribución

1. Fork del repositorio
2. Crear branch para la feature
3. Implementar cambios con tests
4. Crear Pull Request

## Licencia

Este proyecto es parte del sistema de monitoreo ambiental desarrollado para fines educativos.
