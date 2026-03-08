package com.example.hello.config;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {
    
    @Bean
    public Argon2 argon2() {
        return Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 32, 64);
    }
}
