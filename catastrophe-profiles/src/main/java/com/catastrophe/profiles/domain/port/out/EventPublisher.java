package com.catastrophe.profiles.domain.port.out;

import com.catastrophe.commons.event.CatastropheEvent;

/**
 * Puerto de salida — Publicación de eventos de dominio.
 * El servicio de perfiles emite eventos cuando se crea o actualiza un gato.
 */
public interface EventPublisher {

    void publish(CatastropheEvent event);
}
