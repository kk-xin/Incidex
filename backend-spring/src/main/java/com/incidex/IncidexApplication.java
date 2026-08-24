package com.incidex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IncidexApplication {

    public static void main(String[] args) {
        // 启动内置 Tomcat 容器并加载所有的 Spring Bean
        SpringApplication.run(IncidexApplication.class, args);
    }
}