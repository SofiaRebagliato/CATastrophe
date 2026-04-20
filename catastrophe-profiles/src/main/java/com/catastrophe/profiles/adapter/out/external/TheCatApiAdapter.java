package com.catastrophe.profiles.adapter.out.external;

import com.catastrophe.profiles.domain.port.out.CatAvatarProvider;
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
 *
 * Usa RestClient (Spring 6.1+) con Virtual Threads.
 * Si la API falla, devuelve Optional.empty() y el servicio usa el fallback.
 */
@Component
public class TheCatApiAdapter implements CatAvatarProvider {

    private static final Logger log = LoggerFactory.getLogger(TheCatApiAdapter.class);

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
    @SuppressWarnings("unchecked")
    public Optional<String> fetchRandomAvatar(String breed) {
        try {
            var uri = "/images/search?limit=1";
            if (breed != null && !breed.isBlank()) {
                uri += "&breed_ids=" + breed;
            }

            var response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(List.class);

            if (response != null && !response.isEmpty()) {
                var first = (Map<String, Object>) response.get(0);
                var url = (String) first.get("url");
                return Optional.ofNullable(url);
            }
        } catch (Exception e) {
            log.warn("Error al obtener avatar de TheCatAPI: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
