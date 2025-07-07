package ec.edu.espe.ms_eureca_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class MsEurecaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsEurecaServerApplication.class, args);
	}

}
