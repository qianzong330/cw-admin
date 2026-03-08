package com.example.hello.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.hello.mapper")
public class MyBatisConfig {
}
