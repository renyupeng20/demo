package com.example.springbootactivitidemo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.text.SimpleDateFormat;

@SpringBootApplication(exclude = {org.activiti.spring.boot.SecurityAutoConfiguration.class
        , org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
public class SpringbootActivitiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootActivitiDemoApplication.class, args);
    }

}
