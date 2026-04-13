package com.catastrophe.commons.exception;

import java.util.UUID;

/**
 * Excepciones de dominio de CATastrophe.
 * 
 * Todas extienden de RuntimeException (unchecked) siguiendo el patrón Spring.
 * Cada microservicio las captura en su @ControllerAdvice.
 */
public final class CatastropheExceptions {

    private CatastropheExceptions() {} // No instanciable

    /**
     * Recurso no encontrado (404).
     */
    public static class ResourceNotFoundException extends RuntimeException {
        private final String resource;
        private final UUID id;

        public ResourceNotFoundException(String resource, UUID id) {
            super("%s con id %s no encontrado".formatted(resource, id));
            this.resource = resource;
            this.id = id;
        }

        public String getResource() { return resource; }
        public UUID getId() { return id; }
    }

    /**
     * Conflicto de unicidad (409) — ej: username o email duplicado.
     */
    public static class DuplicateResourceException extends RuntimeException {
        private final String resource;
        private final String field;
        private final String value;

        public DuplicateResourceException(String resource, String field, String value) {
            super("%s con %s '%s' ya existe".formatted(resource, field, value));
            this.resource = resource;
            this.field = field;
            this.value = value;
        }

        public String getResource() { return resource; }
        public String getField() { return field; }
        public String getValue() { return value; }
    }

    /**
     * Operación no permitida por reglas de negocio (422).
     */
    public static class BusinessRuleViolationException extends RuntimeException {
        private final String rule;

        public BusinessRuleViolationException(String rule, String message) {
            super(message);
            this.rule = rule;
        }

        public String getRule() { return rule; }
    }

    /**
     * Servicio externo no disponible (503).
     */
    public static class ExternalServiceException extends RuntimeException {
        private final String serviceName;

        public ExternalServiceException(String serviceName, String message, Throwable cause) {
            super("Error en servicio externo '%s': %s".formatted(serviceName, message), cause);
            this.serviceName = serviceName;
        }

        public String getServiceName() { return serviceName; }
    }
}
