package com.robokey.fw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// Habilita o scheduler para tarefas agendadas (i.e, progresso e conexão USB)
@EnableScheduling
public class FWRobokeyApplication {
    public static void main(String[] args) {
        SpringApplication.run(FWRobokeyApplication.class, args);
    }
}
