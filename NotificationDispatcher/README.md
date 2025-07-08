# NotificationDispatcher Microservice

Microservicio encargado de gestionar el envío de notificaciones basadas en eventos de alerta del sistema de monitoreo ambiental.

## 🎯 Funcionalidades

### ✅ **Clasificación Automática de Alertas**
- **CRÍTICAS**: Sismos y alta temperatura → Envío inmediato
- **WARNING**: Advertencias de humedad → Envío agrupado cada 30 min
- **INFO**: Eventos informativos → Envío agrupado cada 30 min

### ✅ **Canales de Notificación**
- **📧 Email**: Notificaciones por correo electrónico
- **📱 SMS**: Mensajes de texto
- **🔔 Push**: Notificaciones push en consola

### ✅ **Persistencia y Auditoría**
- Registro completo de todas las notificaciones enviadas
- Estados: `SENT`, `FAILED`, `PENDING`
- Timestamps de envío y confirmación

### ✅ **APIs Mock para Simulación**
- Endpoints para simular servicios externos
- Confirmación de entrega
- Estadísticas y salud del servicio

## 🚀 Endpoints Disponibles

### **APIs Mock** (`/mock`)
```
POST /mock/email              # Simula servicio de email externo
POST /mock/sms                 # Simula servicio de SMS externo
POST /mock/delivery-confirmation # Webhook de confirmación
GET  /mock/stats               # Estadísticas del servicio
GET  /mock/health              # Estado del servicio
```

### **Gestión de Notificaciones** (`/api/notifications`)
```
GET  /api/notifications                    # Lista todas las notificaciones (paginado)
GET  /api/notifications/{id}               # Obtiene notificación específica
GET  /api/notifications/by-type/{type}     # Por tipo de evento
GET  /api/notifications/by-status/{status} # Por estado
GET  /api/notifications/by-date-range      # Por rango de fechas
GET  /api/notifications/stats/detailed     # Estadísticas detalladas
GET  /api/notifications/health             # Health check completo
```

## ⚙️ Configuración

### **Base de Datos**
- **Puerto**: 26258 (CockroachDB)
- **Base de datos**: `notifications_db`
- **Tabla**: `notifications`

### **RabbitMQ**
- **Exchange**: `environmental.events.exchange`
- **Cola**: `q.events.notification-dispatcher`
- **Tipo**: Fanout (recibe todos los eventos)

### **Programación**
- **Envío agrupado**: Cada 30 minutos
- **Cron**: `0 */30 * * * ?`

## 🔄 Flujo de Procesamiento

1. **📨 Recepción**: Evento de alerta llega por RabbitMQ
2. **🔍 Clasificación**: Determina prioridad (CRITICAL/WARNING/INFO)
3. **⚡ Procesamiento**:
   - **CRÍTICA** → Envío inmediato por todos los canales
   - **WARNING/INFO** → Agregado a cola para envío agrupado
4. **💾 Persistencia**: Guarda log en base de datos
5. **📤 Envío**: Simula envío a servicios externos

## 🏗️ Arquitectura

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Environmental   │───▶│   RabbitMQ       │───▶│ Notification    │
│ Analyzer        │    │   Exchange       │    │ Dispatcher      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
                       ┌─────────────────────────────────────────────┐
                       │              Clasificador                   │
                       │          (CRITICAL/WARNING/INFO)           │
                       └─────────────────────────────────────────────┘
                                      │
                        ┌─────────────┴─────────────┐
                        ▼                           ▼
              ┌─────────────────┐         ┌─────────────────┐
              │ Envío Inmediato │         │ Cola de Espera  │
              │   (CRITICAL)    │         │ (WARNING/INFO)  │
              └─────────────────┘         └─────────────────┘
                        │                           │
                        ▼                           ▼
              ┌─────────────────┐         ┌─────────────────┐
              │ 📧📱🔔 Canales  │         │ ⏰ Scheduler    │
              │   Múltiples     │         │  (30 minutos)   │
              └─────────────────┘         └─────────────────┘
                        │                           │
                        └─────────────┬─────────────┘
                                      ▼
                            ┌─────────────────┐
                            │ 💾 Base de Datos │
                            │ (Audit Log)     │
                            └─────────────────┘
```

## 📊 Configuración Externalizada

Todas las configuraciones importantes están externalizadas en `application.yaml`:

- **Umbrales de clasificación**: Keywords para CRITICAL/WARNING
- **Destinatarios**: Emails y teléfonos por defecto
- **Horarios**: Cron expressions para tareas programadas
- **Conexiones**: RabbitMQ y base de datos

## 🧪 Testing

### **Probar Envío Inmediato** (Eventos Críticos)
```bash
# Los eventos con "seismic" o "temperature" se envían inmediatamente
# Ejemplo: HighTemperatureAlert, SeismicActivityDetected
```

### **Probar Envío Agrupado** (Eventos de Baja Prioridad)
```bash
# Los eventos con "warning" o "humidity" se agrupan
# Ejemplo: LowHumidityWarning
```

### **Verificar APIs Mock**
```bash
curl -X GET http://localhost:8083/mock/health
curl -X GET http://localhost:8083/mock/stats
curl -X GET http://localhost:8083/api/notifications/health
```

## 📝 Logs

El servicio proporciona logs detallados con emojis para fácil identificación:

- 📨 **Recepción** de eventos
- 🔍 **Clasificación** de prioridad
- ⚡ **Envío inmediato** para críticos
- 📦 **Envío agrupado** para baja prioridad
- 💾 **Persistencia** en base de datos
- ❌ **Errores** y excepciones

## 🔧 Mantenimiento

- **Logs de auditoría**: Tabla `notifications` con historial completo
- **Monitoreo**: Endpoints de health y estadísticas
- **Configuración**: Sin hardcoding, todo en `application.yaml`
- **Escalabilidad**: Cola en memoria thread-safe para alta concurrencia
