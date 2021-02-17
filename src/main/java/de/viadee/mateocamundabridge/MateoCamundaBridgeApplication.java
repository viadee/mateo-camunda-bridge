package de.viadee.mateocamundabridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableScheduling
@ConfigurationPropertiesScan
public class MateoCamundaBridgeApplication {

    public static void main(final String... args) {
        SpringApplication.run(MateoCamundaBridgeApplication.class, args);
    }

}
