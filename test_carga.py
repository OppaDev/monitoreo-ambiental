#!/usr/bin/env python3
"""
Pruebas de Carga para Sistema de Monitoreo Ambiental
=====================================================

Sistema de microservicios con:
- Eureka Server (8761)
- API Gateway (8010) 
- SensorDataCollector
- EnvironmentalAnalyzer  
- NotificationDispatcher

Uso: python -m locust -f test_carga.py --host=http://localhost:8010

Autor: Sistema de Monitoreo Ambiental
Fecha: Enero 2025
"""

from locust import HttpUser, task, between, events
import random
import uuid
import json
from datetime import datetime, timezone, timedelta
import logging

# Configurar logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class EnvironmentalMonitoringUser(HttpUser):
    """
    Usuario simulado para el sistema de monitoreo ambiental.
    Simula sensores enviando datos, consultas de análisis y notificaciones.
    """
    
    # Tiempo de espera entre tareas (1-5 segundos)
    wait_time = between(1, 5)
    
    def on_start(self):
        """Inicialización del usuario - se ejecuta una vez al inicio."""
        self.sensor_ids = [
            f"TEMP-{random.randint(1000, 9999)}",
            f"HUM-{random.randint(1000, 9999)}",
            f"SEIS-{random.randint(1000, 9999)}",
            f"AIR-{random.randint(1000, 9999)}"
        ]
        
        # Verificar que el sistema esté funcionando
        self.check_system_health()
    
    def check_system_health(self):
        """Verificar que todos los servicios estén funcionando."""
        try:
            # Verificar Eureka Server directamente
            response = self.client.get("http://localhost:8761/", 
                                     name="[HEALTH] Eureka Server",
                                     catch_response=True)
            if response.status_code == 200:
                logger.info("✅ Eureka Server está activo")
            else:
                logger.warning(f"⚠️ Eureka Server respondió con código {response.status_code}")
        except Exception as e:
            logger.error(f"❌ Error conectando a Eureka: {e}")
    
    # ========================================================================
    # TAREAS PRINCIPALES - SENSOR DATA COLLECTOR
    # ========================================================================
    
    @task(30)  # 30% de probabilidad
    def send_temperature_reading(self):
        """Enviar lectura de temperatura (puede generar alertas críticas)."""
        sensor_id = random.choice(self.sensor_ids)
        
        # Generar temperaturas que ocasionalmente excedan umbrales
        if random.random() < 0.1:  # 10% de probabilidad de temperatura crítica
            temperature = round(random.uniform(45.0, 55.0), 2)  # Crítica >40°C
        else:
            temperature = round(random.uniform(15.0, 35.0), 2)  # Normal
        
        payload = {
            "sensorId": sensor_id,
            "type": "temperature",
            "value": temperature,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        
        with self.client.post("/api/v1/sensor-readings",
                            json=payload,
                            name="[SENSOR] Enviar temperatura",
                            catch_response=True) as response:
            if response.status_code in [200, 201]:
                response.success()
                if temperature > 40:
                    logger.info(f"🌡️ TEMPERATURA CRÍTICA enviada: {temperature}°C")
            else:
                response.failure(f"Error enviando temperatura: {response.status_code}")
    
    @task(25)  # 25% de probabilidad
    def send_humidity_reading(self):
        """Enviar lectura de humedad."""
        sensor_id = random.choice(self.sensor_ids)
        
        # Generar humedad que ocasionalmente sea muy baja
        if random.random() < 0.15:  # 15% de probabilidad de humedad baja
            humidity = round(random.uniform(5.0, 18.0), 2)  # Crítica <20%
        else:
            humidity = round(random.uniform(30.0, 80.0), 2)  # Normal
        
        payload = {
            "sensorId": sensor_id,
            "type": "humidity", 
            "value": humidity,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        
        with self.client.post("/api/v1/sensor-readings",
                            json=payload,
                            name="[SENSOR] Enviar humedad",
                            catch_response=True) as response:
            if response.status_code in [200, 201]:
                response.success()
                if humidity < 20:
                    logger.info(f"💧 HUMEDAD BAJA enviada: {humidity}%")
            else:
                response.failure(f"Error enviando humedad: {response.status_code}")
    
    @task(15)  # 15% de probabilidad
    def send_seismic_reading(self):
        """Enviar lectura sísmica (alta probabilidad de alertas críticas)."""
        sensor_id = random.choice(self.sensor_ids)
        
        # Generar actividad sísmica ocasionalmente alta
        if random.random() < 0.08:  # 8% de probabilidad de sismo fuerte  
            magnitude = round(random.uniform(4.0, 7.5), 2)  # Crítica >3.0
        else:
            magnitude = round(random.uniform(0.1, 2.8), 2)  # Normal
        
        payload = {
            "sensorId": sensor_id,
            "type": "seismic",
            "value": magnitude,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        
        with self.client.post("/api/v1/sensor-readings",
                            json=payload,
                            name="[SENSOR] Enviar sismo",
                            catch_response=True) as response:
            if response.status_code in [200, 201]:
                response.success()
                if magnitude > 3.0:
                    logger.info(f"🌍 SISMO DETECTADO: {magnitude} Richter")
            else:
                response.failure(f"Error enviando dato sísmico: {response.status_code}")
    
    @task(10)  # 10% de probabilidad
    def get_sensor_readings(self):
        """Consultar lecturas históricas de un sensor."""
        sensor_id = random.choice(self.sensor_ids)
        
        with self.client.get(f"/api/v1/sensor-readings/{sensor_id}",
                           name="[SENSOR] Consultar historial",
                           catch_response=True) as response:
            if response.status_code in [200, 204]:  # 204 = No Content (sin datos)
                response.success()
            else:
                response.failure(f"Error consultando sensor: {response.status_code}")
    
    # ========================================================================
    # TAREAS - ENVIRONMENTAL ANALYZER
    # ========================================================================
    
    @task(8)  # 8% de probabilidad
    def get_analyzer_health(self):
        """Verificar salud del analizador ambiental."""
        with self.client.get("/api/v1/analyzer/health",
                           name="[ANALYZER] Health check",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Analyzer health error: {response.status_code}")
    
    @task(12)  # 12% de probabilidad
    def get_alert_statistics(self):
        """Obtener estadísticas de alertas."""
        with self.client.get("/api/v1/analyzer/statistics",
                           name="[ANALYZER] Estadísticas",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error estadísticas: {response.status_code}")
    
    @task(7)  # 7% de probabilidad
    def get_recent_alerts(self):
        """Obtener alertas recientes."""
        hours = random.choice([6, 12, 24, 48])
        limit = random.choice([10, 25, 50])
        
        with self.client.get(f"/api/v1/analyzer/alerts/recent?hours={hours}&limit={limit}",
                           name="[ANALYZER] Alertas recientes",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error alertas recientes: {response.status_code}")
    
    @task(5)  # 5% de probabilidad
    def get_alerts_by_type(self):
        """Obtener alertas por tipo."""
        alert_type = random.choice(["HighTemperatureAlert", "LowHumidityWarning", 
                                  "SeismicActivityDetected", "AirQualityAlert"])
        
        with self.client.get(f"/api/v1/analyzer/alerts/type/{alert_type}",
                           name="[ANALYZER] Alertas por tipo",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error alertas por tipo: {response.status_code}")
    
    @task(4)  # 4% de probabilidad
    def get_alerts_by_sensor(self):
        """Obtener alertas de un sensor específico."""
        sensor_id = random.choice(self.sensor_ids)
        
        with self.client.get(f"/api/v1/analyzer/alerts/sensor/{sensor_id}",
                           name="[ANALYZER] Alertas por sensor",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error alertas por sensor: {response.status_code}")
    
    @task(6)  # 6% de probabilidad
    def get_all_alerts(self):
        """Obtener todas las alertas con paginación."""
        page = random.randint(0, 5)
        size = random.choice([20, 50, 100])
        
        with self.client.get(f"/api/v1/analyzer/alerts?page={page}&size={size}",
                           name="[ANALYZER] Todas las alertas",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error todas las alertas: {response.status_code}")
    
    @task(3)  # 3% de probabilidad
    def get_analyzer_info(self):
        """Obtener información del servicio analizador."""
        with self.client.get("/api/v1/analyzer/info",
                           name="[ANALYZER] Info servicio",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error info analyzer: {response.status_code}")
    
    # ========================================================================
    # TAREAS - NOTIFICATION DISPATCHER
    # ========================================================================
    
    @task(8)  # 8% de probabilidad
    def get_notification_health(self):
        """Verificar salud del despachador de notificaciones."""
        with self.client.get("/api/v1/notifications/health",
                           name="[NOTIFICATION] Health check",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Notification health error: {response.status_code}")
    
    @task(6)  # 6% de probabilidad
    def get_notification_stats(self):
        """Obtener estadísticas de notificaciones."""
        with self.client.get("/api/v1/notifications/stats/detailed",
                           name="[NOTIFICATION] Estadísticas",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error stats notificaciones: {response.status_code}")
    
    @task(4)  # 4% de probabilidad
    def get_notifications_by_status(self):
        """Obtener notificaciones por estado."""
        status = random.choice(["SENT", "FAILED", "PENDING"])
        
        with self.client.get(f"/api/v1/notifications/by-status/{status}",
                           name="[NOTIFICATION] Por estado",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error notificaciones por estado: {response.status_code}")
    
    @task(3)  # 3% de probabilidad
    def get_notifications_by_type(self):
        """Obtener notificaciones por tipo."""
        event_type = random.choice(["HighTemperatureAlert", "LowHumidityWarning", 
                                  "SeismicActivityDetected"])
        
        with self.client.get(f"/api/v1/notifications/by-type/{event_type}",
                           name="[NOTIFICATION] Por tipo",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error notificaciones por tipo: {response.status_code}")
    
    @task(5)  # 5% de probabilidad
    def get_all_notifications(self):
        """Obtener todas las notificaciones con paginación."""
        page = random.randint(0, 3)
        size = random.choice([10, 20, 50])
        sort_by = random.choice(["timestamp", "status", "eventType"])
        sort_dir = random.choice(["asc", "desc"])
        
        params = f"page={page}&size={size}&sortBy={sort_by}&sortDir={sort_dir}"
        with self.client.get(f"/api/v1/notifications?{params}",
                           name="[NOTIFICATION] Todas paginadas",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Error todas las notificaciones: {response.status_code}")
    
    # ========================================================================
    # TAREAS - MOCK APIS (Servicios externos simulados)
    # ========================================================================
    
    @task(4)  # 4% de probabilidad
    def get_mock_health(self):
        """Verificar APIs mock de servicios externos."""
        with self.client.get("/api/v1/mock/health",
                           name="[MOCK] Health check",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Mock health error: {response.status_code}")
    
    @task(3)  # 3% de probabilidad
    def get_mock_stats(self):
        """Obtener estadísticas de servicios mock."""
        with self.client.get("/api/v1/mock/stats",
                           name="[MOCK] Estadísticas",
                           catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Mock stats error: {response.status_code}")

# ============================================================================
# USUARIOS ESPECIALIZADOS PARA ESCENARIOS ESPECÍFICOS
# ============================================================================

class HighVolumeDataUser(HttpUser):
    """Usuario que genera alto volumen de datos de sensores."""
    wait_time = between(0.5, 2)  # Más agresivo
    weight = 3  # Menor peso, solo algunos usuarios
    
    def on_start(self):
        self.sensor_id = f"BULK-{uuid.uuid4().hex[:8]}"
    
    @task
    def rapid_sensor_data(self):
        """Envío rápido de datos de sensores."""
        sensor_types = ["temperature", "humidity", "seismic", "air_quality"]
        sensor_type = random.choice(sensor_types)
        
        # Valores más aleatorios para generar variedad
        if sensor_type == "temperature":
            value = round(random.uniform(-10, 60), 2)
        elif sensor_type == "humidity":
            value = round(random.uniform(0, 100), 2)
        elif sensor_type == "seismic":
            value = round(random.uniform(0, 8), 2)
        else:  # air_quality
            value = round(random.uniform(0, 500), 2)
        
        payload = {
            "sensorId": self.sensor_id,
            "type": sensor_type,
            "value": value,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        
        self.client.post("/api/v1/sensor-readings", 
                        json=payload,
                        name="[BULK] Datos masivos")

class AlertMonitorUser(HttpUser):
    """Usuario enfocado en monitorear alertas y notificaciones."""
    wait_time = between(2, 8)
    weight = 2  # Peso medio
    
    @task(40)
    def monitor_recent_alerts(self):
        """Monitoreo constante de alertas recientes."""
        self.client.get("/api/v1/analyzer/alerts/recent?hours=1&limit=20",
                       name="[MONITOR] Alertas última hora")
    
    @task(30)  
    def check_critical_notifications(self):
        """Verificar notificaciones críticas."""
        self.client.get("/api/v1/notifications/by-status/SENT",
                       name="[MONITOR] Notificaciones enviadas")
    
    @task(20)
    def get_system_statistics(self):
        """Obtener estadísticas del sistema."""
        self.client.get("/api/v1/analyzer/statistics",
                       name="[MONITOR] Stats sistema")
    
    @task(10)
    def check_failed_notifications(self):
        """Verificar notificaciones fallidas."""
        self.client.get("/api/v1/notifications/by-status/FAILED",
                       name="[MONITOR] Notificaciones fallidas")

# ============================================================================
# EVENT LISTENERS PARA LOGGING AVANZADO
# ============================================================================

@events.request.add_listener
def on_request(request_type, name, response_time, response_length, response, 
               context, exception, start_time, url, **kwargs):
    """Listener para registrar eventos de solicitudes."""
    if exception:
        logger.error(f"❌ {name}: {exception}")
    elif response.status_code >= 400:
        logger.warning(f"⚠️ {name}: HTTP {response.status_code}")
    elif "[SENSOR]" in name and response_time > 1000:
        logger.info(f"🐌 {name}: Respuesta lenta ({response_time:.0f}ms)")

@events.user_error.add_listener
def on_user_error(user_instance, exception, tb, **kwargs):
    """Listener para errores de usuario."""
    logger.error(f"💥 Error de usuario: {exception}")

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Evento al inicio de las pruebas."""
    logger.info("🚀 Iniciando pruebas de carga del Sistema de Monitoreo Ambiental")
    logger.info("📊 Configuración:")
    logger.info(f"   • Host: {environment.host}")
    logger.info(f"   • Usuarios: {environment.user_classes}")

@events.test_stop.add_listener  
def on_test_stop(environment, **kwargs):
    """Evento al finalizar las pruebas."""
    stats = environment.stats
    logger.info("🏁 Pruebas finalizadas")
    logger.info(f"📈 Resumen:")
    logger.info(f"   • Total requests: {stats.total.num_requests}")
    logger.info(f"   • Failures: {stats.total.num_failures}")
    logger.info(f"   • Avg response time: {stats.total.avg_response_time:.2f}ms")
    logger.info(f"   • RPS: {stats.total.current_rps:.2f}")

if __name__ == "__main__":
    print("""
🌍 Sistema de Monitoreo Ambiental - Pruebas de Carga
====================================================

Para ejecutar las pruebas:

1. Asegúrate de que todos los servicios estén ejecutándose:
   - Eureka Server: http://localhost:8761
   - API Gateway: http://localhost:8010
   - SensorDataCollector
   - EnvironmentalAnalyzer  
   - NotificationDispatcher

2. Ejecuta las pruebas:
   python -m locust -f test_carga.py --host=http://localhost:8010

3. Abre la interfaz web: http://localhost:8089

Escenarios de prueba incluidos:
• Envío masivo de datos de sensores
• Generación de alertas críticas y warnings
• Consultas de análisis histórico
• Monitoreo de notificaciones
• Verificación de APIs de salud

⚡ Los datos generados incluyen:
   - Temperaturas críticas >40°C (10% probabilidad)
   - Humedad baja <20% (15% probabilidad) 
   - Sismos >3.0 Richter (8% probabilidad)
""")