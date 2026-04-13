package com.catastrophe.commons.event;

/**
 * Nombres de los topics de Kafka compartidos entre microservicios.
 * Centralizados aquí para evitar typos y facilitar refactoring.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String PROFILE_EVENTS    = "catastrophe.profiles";
    public static final String SOCIAL_EVENTS     = "catastrophe.social";
    public static final String GAMIFICATION_EVENTS = "catastrophe.gamification";
    public static final String NOTIFICATION_EVENTS = "catastrophe.notifications";
}
