package br.monitoriamap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MonitoriaMapApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoriaMapApplication.class, args);
    }
}
