package com.catastrophe.analytics.domain.model;

/**
 * Traits de personalidad felina.
 * <p>
 * El score de cada trait se calcula con exponential smoothing a partir
 * de los eventos del bus: cada acción del gato aporta un impulso a uno
 * o varios traits y mueve el score en la dirección correspondiente.
 */
public enum Trait {

    /** Activo, le gustan los retos y aventuras. */
    PLAYFUL,

    /** Inactivo, perfil contemplativo. Inverso de PLAYFUL. */
    LAZY,

    /** Competitivo, gana retos PvP, persigue badges. */
    HUNTER,

    /** Posts, follows, comentarios — vida social activa. */
    SOCIAL,

    /** Pocos eventos pero impactantes (badges raros, level-ups). */
    MYSTERIOUS
}
