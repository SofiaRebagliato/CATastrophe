package com.catastrophe.analytics.adapter.out.external;

import com.catastrophe.analytics.domain.model.CatFact;
import com.catastrophe.analytics.domain.port.out.CatFactProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Proveedor de curiosidades felinas — pool fijo en código.
 * <p>
 * Decisión de simplicidad (acordada con la alumna): un pool de 20 curiosidades
 * hardcoded sirve siempre, sin depender de servicios externos ni añadir tablas.
 * El servicio queda 100% determinista en dev y testing.
 * <p>
 * En una iteración futura este adapter podría llamar a la API real
 * (catfact.ninja) para refrescar el pool periódicamente, manteniendo la
 * lista hardcoded como fallback definitivo.
 */
@Component
public class CatFactAdapter implements CatFactProvider {

    private static final List<String> POOL = List.of(
            "los gatos pueden hacer más de 100 sonidos diferentes (los perros, unos 10)",
            "un gato puede saltar hasta 6 veces su propia altura",
            "los gatos duermen entre 12 y 16 horas al día",
            "el ronroneo está en una frecuencia entre 25 y 150 Hz, y se ha demostrado que ayuda a sanar huesos",
            "los gatos tienen 32 músculos en cada oreja",
            "el sentido del olfato de un gato es 14 veces más potente que el humano",
            "los gatos no pueden saborear lo dulce",
            "un grupo de gatos se llama 'clowder'",
            "los gatos solo maúllan a los humanos, no entre ellos",
            "el gato más viejo registrado vivió 38 años",
            "los gatos tienen un tercer párpado llamado membrana nictitante",
            "un gato puede correr hasta 50 km/h en distancias cortas",
            "los gatos tienen huellas únicas en sus narices, como las huellas dactilares humanas",
            "el corazón de un gato late al doble de velocidad que el humano",
            "los gatos pueden rotar sus orejas 180 grados",
            "los bigotes de un gato son tan anchos como su cuerpo y le ayudan a saber si cabe por un hueco",
            "los gatos prefieren el agua que se mueve, por eso les fascina el grifo",
            "un gato adulto tiene 30 dientes, los gatitos solo 26",
            "los gatos pasan entre el 30 y el 50% de su tiempo despiertos acicalándose",
            "el dueño promedio dedica 1500 horas a su gato a lo largo de su vida — el gato pasaría exactamente cero pensando en eso"
    );

    @Override
    public Optional<CatFact> fetchRandomFact() {
        int index = ThreadLocalRandom.current().nextInt(POOL.size());
        return Optional.of(new CatFact(POOL.get(index)));
    }
}
