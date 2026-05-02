package com.catastrophe.profiles.adapter.out.external;

import com.catastrophe.profiles.domain.port.out.CatAvatarProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador de salida — Integración con TheCatAPI para obtener avatares.
 * <p>
 * <strong>Tolerancia a fallos</strong> (cumple sección 2.4 de la spec):
 * la llamada a la API está envuelta en un {@code @CircuitBreaker} de
 * Resilience4j. Cuando la API falla repetidamente:
 * <ol>
 *   <li>Tras N fallos consecutivos en la ventana deslizante, el circuito
 *       <em>se abre</em> y todas las llamadas siguientes van directas al
 *       fallback sin tocar la API.</li>
 *   <li>Pasado el tiempo de espera, el circuito pasa a <em>half-open</em>:
 *       deja pasar unas pocas llamadas de prueba.</li>
 *   <li>Si esas pruebas tienen éxito el circuito se cierra; si no, vuelve
 *       a abrirse.</li>
 * </ol>
 * El método {@link #fallbackAvatar} se invoca tanto cuando la API responde
 * con error como cuando el circuito está abierto. Devuelve {@link Optional#empty()}
 * para que el servicio aplique su avatar por defecto.
 * <p>
 * Notas técnicas:
 * <ul>
 *   <li>Usa {@code RestClient} (Spring 6.1+) — funciona con virtual threads.</li>
 *   <li>El bean se invoca a través del proxy AOP que añade Resilience4j; por eso
 *       el método debe ser público (lo es, viene del interfaz).</li>
 * </ul>
 */
@Component
public class TheCatApiAdapter implements CatAvatarProvider {

    private static final Logger log = LoggerFactory.getLogger(TheCatApiAdapter.class);
    private static final String CB_NAME = "thecatapi";

    private final RestClient restClient;

    public TheCatApiAdapter(
            @Value("${catastrophe.catapi.base-url:https://api.thecatapi.com/v1}") String baseUrl,
            @Value("${catastrophe.catapi.api-key:}") String apiKey) {

        var builder = RestClient.builder().baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("x-api-key", apiKey);
        }
        this.restClient = builder.build();
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "fallbackAvatar")
    @SuppressWarnings("unchecked")
    public Optional<String> fetchRandomAvatar(String breed) {
        var uri = "/images/search?limit=1";
        if (breed != null && !breed.isBlank()) {
            uri += "&breed_ids=" + breed;
        }

        var response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(List.class);

        if (response == null || response.isEmpty()) {
            return Optional.empty();
        }

        var first = (Map<String, Object>) response.get(0);
        var url = (String) first.get("url");
        return Optional.ofNullable(url);
    }

    /**
     * Fallback invocado por Resilience4j cuando la llamada principal falla
     * o el circuito está abierto.
     * <p>
     * La firma debe ser idéntica a la del método principal más un
     * parámetro final {@code Throwable} con la causa.
     * <p>
     * Devolvemos {@link Optional#empty()} para que {@code CatService} aplique
     * su avatar por defecto. La spec habla de "fallback a avatar por defecto":
     * preferimos delegar la URL al servicio en lugar de hardcodearla aquí
     * para que el dominio no acople la URL del fallback al adapter externo.
     */
    @SuppressWarnings("unused")
    Optional<String> fallbackAvatar(String breed, Throwable ex) {
        log.warn("Fallback TheCatAPI activado (breed='{}'): {}", breed, ex.getMessage());
        return Optional.empty();
    }
}
