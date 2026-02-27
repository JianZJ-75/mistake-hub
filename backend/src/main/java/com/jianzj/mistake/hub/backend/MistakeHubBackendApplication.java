package com.jianzj.mistake.hub.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.jianzj.mistake.hub")
@MapperScan("com.jianzj.mistake.hub.backend.mapper")
@EnableScheduling
@EnableAspectJAutoProxy
public class MistakeHubBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MistakeHubBackendApplication.class, args);
    }

}
