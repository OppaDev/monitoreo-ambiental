package ec.edu.espe.EnvironmentalAnalyzer.config;

/**
 * Constantes del sistema de monitoreo ambiental
 * Define la nomenclatura estándar para colas y eventos
 */
public final class SystemConstants {
    
    private SystemConstants() {
        // Evitar instanciación
    }
    
    // Nomenclatura de colas según especificaciones del sistema
    public static final class Queues {
        public static final String SENSOR_DATA_COLLECTOR = "q.events.sensor-data-collector";
        public static final String ENVIRONMENTAL_ANALYZER = "q.events.environmental-analyzer";
        public static final String NOTIFICATION_DISPATCHER = "q.events.notification-dispatcher";
    }
    
    // Tipos de eventos del sistema
    public static final class EventTypes {
        // Eventos entrantes
        public static final String NEW_SENSOR_READING = "NewSensorReadingEvent";
        
        // Eventos de alerta (salientes)
        public static final String HIGH_TEMPERATURE_ALERT = "HighTemperatureAlert";
        public static final String LOW_HUMIDITY_WARNING = "LowHumidityWarning";
        public static final String SEISMIC_ACTIVITY_DETECTED = "SeismicActivityDetected";
        
        // Eventos programados
        public static final String DAILY_REPORT_GENERATED = "DailyReportGenerated";
        public static final String SENSOR_INACTIVE_ALERT = "SensorInactiveAlert";
    }
    
    // Tipos de sensores
    public static final class SensorTypes {
        public static final String TEMPERATURE = "temperature";
        public static final String HUMIDITY = "humidity";
        public static final String SEISMIC = "seismic";
    }
    
    // Umbrales por defecto
    public static final class DefaultThresholds {
        public static final double TEMPERATURE = 40.0; // °C
        public static final double HUMIDITY = 20.0; // %
        public static final double SEISMIC = 3.0; // Escala Richter
    }
}
