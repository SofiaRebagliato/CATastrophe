package com.catastrophe.profiles.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.RedisSerializer;
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
     * Más legible que la serialización Java por defecto y evita
     * problemas de compatibilidad entre versiones.
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return RedisSerializer.json();
    }
}
