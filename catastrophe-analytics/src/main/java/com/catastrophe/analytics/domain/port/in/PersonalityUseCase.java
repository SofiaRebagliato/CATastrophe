package com.catastrophe.analytics.domain.port.in;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.analytics.domain.model.Personality;

import java.util.UUID;

/**
 * Puerto de entrada — Personalidades felinas.
 */
public interface PersonalityUseCase {

    /**
     * Procesa un evento del bus y actualiza los traits del gato si procede.
     * Es idempotente: si el {@code eventId} ya se procesó, no aplica nada.
     */
    void handleEvent(CatastropheEvent event);

    /**
     * Devuelve la personalidad completa del gato. Si nunca se ha procesado
     * ningún evento para él, devuelve una {@link Personality#empty}.
     */
    Personality findByCatId(UUID catId);
}
