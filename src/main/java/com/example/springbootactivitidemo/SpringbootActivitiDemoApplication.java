package com.example.springbootactivitidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {org.activiti.spring.boot.SecurityAutoConfiguration.class
        , org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
public class SpringbootActivitiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootActivitiDemoApplication.class, args);
    }

}
