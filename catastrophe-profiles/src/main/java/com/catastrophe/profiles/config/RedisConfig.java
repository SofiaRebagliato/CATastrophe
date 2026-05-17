package com.catastrophe.profiles.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Configuración de Redis para sesiones HTTP.
 *
 * Las sesiones se almacenan en Redis, lo que permite:
 *  - Escalabilidad horizontal (varios nodos comparten sesiones)
 *  - Persistencia de sesiones entre reinicios del servicio
 *  - TTL automático (expiración configurable)
 *
 * maxInactiveIntervalInSeconds = 3600 → La sesión expira tras 1 hora de inactividad.
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class RedisConfig {

    /**
     * Serializador JSON para las sesiones en Redis.
     * Usa los mixins de Spring Security para poder serializar/deserializar
     * correctamente los objetos de autenticación (UsernamePasswordAuthenticationToken, etc.).
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
