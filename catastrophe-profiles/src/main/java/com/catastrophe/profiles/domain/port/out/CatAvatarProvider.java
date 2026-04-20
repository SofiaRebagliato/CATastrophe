package com.catastrophe.profiles.domain.port.out;

import java.util.Optional;

/**
 * Puerto de salida — Proveedor de avatares para gatos.
 * Abstrae la llamada a TheCatAPI u otro servicio de imágenes.
 */
public interface CatAvatarProvider {

    /**
     * Obtiene una URL de avatar aleatorio, opcionalmente filtrado por raza.
     * @param breed raza del gato (puede ser null)
     * @return URL de la imagen, o empty si el servicio no está disponible
     */
    Optional<String> fetchRandomAvatar(String breed);
}
