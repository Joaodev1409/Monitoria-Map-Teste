package br.monitoriamap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableAsync
@EnableScheduling
public class InfraConfig {

    /**
     * Injetar o relógio (em vez de chamar {@code Instant.now()} direto) deixa o código
     * testável: nos testes passamos um {@code Clock.fixed(...)}.
     */
    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
