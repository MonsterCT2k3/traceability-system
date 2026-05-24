package vn.edu.kma.traceability_core_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TraceabilityCoreServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TraceabilityCoreServiceApplication.class, args);
	}

}

