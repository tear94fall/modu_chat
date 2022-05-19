package com.example.modumessenger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableWebMvc
public class SwaggerConfig {
    private ApiInfo commonInfo() {
        return new ApiInfoBuilder()
                .title("Modu Api")
                //.description("")
                //.license("")
                //.licenseUrl("")
                .version("1.0")
                .build();
    }

    @Bean
    public Docket allApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("Member")
                .useDefaultResponseMessages(false)
                .select()
                //.apis(RequestHandlerSelectors.any())
                .apis(RequestHandlerSelectors.basePackage("com.example.modumessaenger.member.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(commonInfo());
    }
}