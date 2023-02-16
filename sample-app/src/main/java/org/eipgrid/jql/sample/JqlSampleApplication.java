package org.eipgrid.jql.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


@SpringBootApplication
public class JqlSampleApplication {
	public static void main(String[] args) {
		SpringApplication.run(JqlSampleApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void showStartup() {
		System.out.println("#####################################################");
		System.out.println("# Tutorial: http://localhost:7007                   #");
		System.out.println("# Swagger:  http://localhost:7007/swagger-ui.html   #");
	}
}

