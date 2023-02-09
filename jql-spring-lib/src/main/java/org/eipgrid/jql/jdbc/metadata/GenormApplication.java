package org.eipgrid.jql.jdbc.metadata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GenormApplication {

    public static void main(String[] args) {
        if (true) {
            SpringApplication.run(GenormApplication.class, args);
        }
        else {
            SpringApplication app = new SpringApplication(GenormApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            app.run(args);
        }
    }

}
