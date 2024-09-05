package com.tericcabrel.authorization.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Identity Authorization Service")
                        .description("REST API to manage user's registration and authentication, role management, and token generation and validation")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Eric Cabrel TIOGO")
                                .url("http://tericcabrel.com")
                                .email("tericcabrel@gmail.com"))
                        .license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(new Server().url("/").description("Default Server")))
                .components(new Components());
    }
}
