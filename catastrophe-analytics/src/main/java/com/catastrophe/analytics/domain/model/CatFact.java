package com.catastrophe.analytics.domain.model;

/**
 * Curiosidad felina, devuelta por {@code CatFactProvider}.
 * <p>
 * Modelo intencionalmente minimalista: solo el texto. El dominio no
 * necesita más para el "pronóstico de humor".
 */
public record CatFact(String text) {
}
