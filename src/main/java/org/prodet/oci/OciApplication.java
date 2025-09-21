package org.prodet.oci;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OciApplication {

    public static void main(String[] args) {
        SpringApplication.run(OciApplication.class, args);
    }

}
