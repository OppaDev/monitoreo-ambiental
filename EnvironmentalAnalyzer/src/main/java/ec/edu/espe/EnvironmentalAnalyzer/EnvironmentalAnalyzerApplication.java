package ec.edu.espe.EnvironmentalAnalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient // Para registrarse en Eureka
@EnableScheduling      // Para activar las tareas @Scheduled
public class EnvironmentalAnalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnvironmentalAnalyzerApplication.class, args);
	}

}
