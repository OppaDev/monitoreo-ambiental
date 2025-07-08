package ec.edu.espe.NotificationDispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal del NotificationDispatcher
 * Microservicio encargado de gestionar el envío de notificaciones
 * basadas en eventos de alerta del sistema de monitoreo ambiental
 */
@SpringBootApplication
@EnableDiscoveryClient // Habilita el registro en Eureka
@EnableScheduling // Activa la ejecución de tareas @Scheduled
public class NotificationDispatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationDispatcherApplication.class, args);
	}
}
