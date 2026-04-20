package com.catastrophe.social.domain.port.out;

import com.catastrophe.commons.event.CatastropheEvent;

/**
 * Puerto de salida — Publicación de eventos de dominio.
 * 
 * El servicio social emite eventos cuando ocurren acciones sociales
 * (nuevo post, like, comentario, follow) para que otros microservicios
 * los procesen (notificaciones, analytics, personalidades).
 */
public interface EventPublisher {

    void publish(CatastropheEvent event);
}
