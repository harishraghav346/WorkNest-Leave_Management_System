package com.examly.springapp.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;

@Configuration
@OpenAPIDefinition(
  info = @io.swagger.v3.oas.annotations.info.Info(
    title = "My Spring Boot API",
    version ="1.0",
    description = "API documentation for my Spring Boot Application"
  )
)
public class SwaggerConfig {

}