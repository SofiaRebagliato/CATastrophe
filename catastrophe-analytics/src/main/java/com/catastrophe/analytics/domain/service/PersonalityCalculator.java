package com.catastrophe.analytics.domain.service;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.CatastropheEvent.AdventureCompleted;
import com.catastrophe.commons.event.CatastropheEvent.AdventureStarted;
import com.catastrophe.commons.event.CatastropheEvent.BadgeEarned;
import com.catastrophe.commons.event.CatastropheEvent.CatCreated;
import com.catastrophe.commons.event.CatastropheEvent.CatFollowed;
import com.catastrophe.commons.event.CatastropheEvent.CatUpdated;
import com.catastrophe.commons.event.CatastropheEvent.ChallengeCompleted;
import com.catastrophe.commons.event.CatastropheEvent.MeowPosted;
import com.catastrophe.commons.event.CatastropheEvent.PostCommented;
import com.catastrophe.commons.event.CatastropheEvent.PostLiked;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.analytics.domain.model.Trait;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Calcula impulsos por trait a partir de eventos del bus, y aplica
 * <em>exponential smoothing</em> para mezclar el nuevo impulso con el score
 * existente.
 * <p>
 * Fórmula: {@code score_new = α · impulse + (1 − α) · score_old}, con
 * {@code α = 0.15} (suavizado moderado: el score se mueve hacia los nuevos
 * datos pero no olvida el pasado de golpe).
 * <p>
 * El mapping evento → impulso vive en {@link #impulseFor(CatastropheEvent)}.
 * Pattern matching exhaustivo sobre el sealed: añadir un evento nuevo en
 * {@code commons} obligará a tomar una decisión aquí.
 */
@Component
public class PersonalityCalculator {

    /** Factor de suavizado: cuánto pesa el nuevo dato vs la historia. */
    private static final double ALPHA = 0.15;

    /**
     * Aplica el suavizado: combina el score actual con el impulso entrante.
     * Si el evento no afecta al trait (impulso 0), devuelve el score actual
     * sin cambios.
     */
    public double smooth(double currentScore, double impulse) {
        if (impulse == 0.0) {
            return currentScore;
        }
        double combined = ALPHA * impulse + (1 - ALPHA) * currentScore;
        // Acotamos al rango [0.0, 1.0] por si una secuencia inusual lo desborda.
        return Math.max(0.0, Math.min(1.0, combined));
    }

    /**
     * Devuelve el mapa de impulsos por trait que produce este evento.
     * <p>
     * Convenciones:
     * <ul>
     *   <li>El receptor de la acción social es quien recibe el impulso (el dueño
     *       del post para los likes/comments, el seguido para los follows).</li>
     *   <li>El actor de la acción de gamificación (gato que completa aventura,
     *       que gana o pierde reto) recibe el impulso.</li>
     *   <li>Los eventos sin valor de personalidad (ej. {@code AdventureStarted})
     *       devuelven mapa vacío.</li>
     * </ul>
     *
     * @return mapa con los impulsos a aplicar; clave = trait, valor = impulso [0,1]
     */
    public Map<Trait, Double> impulseFor(CatastropheEvent event) {
        var impulses = new EnumMap<Trait, Double>(Trait.class);

        switch (event) {
            // ── Sociales: el actor publica/comenta (social activo) ──
            case MeowPosted _ -> impulses.put(Trait.SOCIAL, 0.8);

            case PostCommented _ -> {
                // El que comenta también es social, aunque menos que publicar.
                impulses.put(Trait.SOCIAL, 0.6);
            }

            // ── Sociales: el receptor recibe atención ──
            // Aplica al postOwnerId / followedCatId, no al catId.
            // El service decide a qué gato aplicar mirando el campo correcto.
            case PostLiked _ -> impulses.put(Trait.SOCIAL, 0.4);
            case CatFollowed _ -> impulses.put(Trait.SOCIAL, 0.6);

            // ── Gamificación ──
            case AdventureCompleted _ -> {
                impulses.put(Trait.PLAYFUL, 1.0);
                impulses.put(Trait.HUNTER, 0.6);
                // Completar aventuras es lo opuesto a ser perezoso
                impulses.put(Trait.LAZY, 0.0);
            }

            case ChallengeCompleted e -> {
                // El resultado matiza el impulso
                switch (e.result()) {
                    case WON -> {
                        impulses.put(Trait.HUNTER, 1.0);
                        impulses.put(Trait.PLAYFUL, 0.5);
                    }
                    case LOST -> {
                        // Aún así estuvo activo, pero menos contundente
                        impulses.put(Trait.PLAYFUL, 0.3);
                    }
                    case DRAW -> {
                        impulses.put(Trait.HUNTER, 0.4);
                        impulses.put(Trait.PLAYFUL, 0.4);
                    }
                }
            }

            case BadgeEarned e -> {
                // Las rarezas altas son más "misteriosas" — lograrlas requiere
                // logros poco frecuentes
                double mysteriousImpulse = switch (e.rarity()) {
                    case "LEGENDARY" -> 1.0;
                    case "EPIC"      -> 0.8;
                    case "RARE"      -> 0.5;
                    default          -> 0.2;
                };
                impulses.put(Trait.MYSTERIOUS, mysteriousImpulse);
            }

            case XpGained e -> {
                // Solo level-up: detectar level-up requiere comparar con el nivel
                // anterior, pero aquí no tenemos ese contexto. Usamos la heurística
                // simple: si newLevel >= 2 y amount >= 50, asumimos progreso notable.
                if (e.newLevel() >= 2 && e.amount() >= 50) {
                    impulses.put(Trait.PLAYFUL, 0.5);
                }
            }

            // ── Eventos ignorados ──
            // El compilador nos obliga a listarlos: si commons añade un evento,
            // este switch deja de compilar hasta que decidamos qué hacer.
            case CatCreated _,
                 CatUpdated _,
                 AdventureStarted _ -> {
                     // Sin impulso
                 }
        }

        return impulses;
    }
}
