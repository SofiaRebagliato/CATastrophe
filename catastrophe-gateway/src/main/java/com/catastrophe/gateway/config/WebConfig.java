package com.catastrophe.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * Configuración para servir el frontend estático (HTMX + Tailwind).
 * Spring Cloud Gateway (WebFlux) no sirve estáticos por defecto como Spring MVC.
 */
@Configuration
public class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> indexRouter() {
        return RouterFunctions.route(GET("/"),
                request -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .bodyValue(new ClassPathResource("static/index.html")));
    }

    @Bean
    public RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions.resources("/pages/**", new ClassPathResource("static/pages/"))
                .and(RouterFunctions.resources("/js/**", new ClassPathResource("static/js/")))
                .and(RouterFunctions.resources("/css/**", new ClassPathResource("static/css/")));
    }
}
