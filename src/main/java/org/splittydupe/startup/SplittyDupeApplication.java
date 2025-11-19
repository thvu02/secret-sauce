package org.splittydupe.startup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SplittyDupeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SplittyDupeApplication.class, args);
    }
}
