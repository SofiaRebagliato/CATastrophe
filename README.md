# 🐱 CATastrophe

**Plataforma social gamificada para gatos** — Arquitectura de microservicios con Java 21 y Spring Boot 3.

> Los gatos son los verdaderos usuarios. Los humanos solo son sus "asistentes".

## Arquitectura

```
                    ┌─────────────────┐
                    │   API Gateway   │ :8080
                    │ Spring Cloud GW │
                    └────────┬────────┘
                             │
         ┌───────────┬───────┼───────┬──────────────┐
         │           │       │       │              │
    ┌────▼───┐  ┌────▼──┐ ┌──▼───┐ ┌─▼──────────┐ ┌▼─────────┐
    │Profiles│  │Social │ │Advent│ │Notifications│ │Analytics │
    │ :8081  │  │ :8082 │ │:8083 │ │   :8084     │ │  :8085   │
    └────┬───┘  └───┬───┘ └──┬───┘ └──────┬──────┘ └────┬─────┘
         │          │        │            │              │
         └──────────┴────────┴─────┬──────┴──────────────┘
                                   │
                          ┌────────▼────────┐
                          │   Apache Kafka  │
                          │ (Bus de eventos)│
                          └─────────────────┘
```

Cada microservicio sigue **arquitectura hexagonal** (Ports & Adapters):
```
adapter/in/web/        → Controllers REST (adaptadores de entrada)
domain/model/          → Records inmutables (modelos de dominio)
domain/port/in/        → Use Cases (puertos de entrada)
domain/port/out/       → Interfaces de persistencia/mensajería (puertos de salida)
domain/service/        → Lógica de negocio (servicios de dominio)
adapter/out/persistence/ → JPA entities, mappers, repositories
adapter/out/messaging/   → Kafka producers
adapter/out/external/    → Clientes de APIs externas
```

## Features de Java 21 utilizadas

| Feature | Uso en CATastrophe |
|---------|-------------------|
| **Records** | DTOs, eventos de dominio, modelos inmutables |
| **Sealed Interfaces** | CatMood, CatastropheEvent (exhaustividad en switch) |
| **Pattern Matching** | Handlers de eventos Kafka |
| **Virtual Threads** | Llamadas concurrentes a APIs externas |
| **Structured Concurrency** | Orquestación de llamadas paralelas |

## Stack

- Java 21, Spring Boot 3.4, Spring Cloud Gateway
- PostgreSQL 16, Redis 7, Apache Kafka
- Flyway (migraciones), Resilience4j, Testcontainers
- Thymeleaf + HTMX (frontend)

## Quick Start

```bash
# 1. Levantar infraestructura
docker compose up -d

# 2. Compilar todo
./mvnw clean install -DskipTests

# 3. Arrancar el servicio de perfiles (primer microservicio)
cd catastrophe-profiles
../mvnw spring-boot:run

# 4. Arrancar el gateway
cd ../catastrophe-gateway
../mvnw spring-boot:run
```

## Módulos

| Módulo | Puerto | Descripción |
|--------|--------|-------------|
| `catastrophe-commons` | — | Eventos, DTOs, excepciones compartidas |
| `catastrophe-gateway` | 8080 | API Gateway, enrutamiento, rate limiting |
| `catastrophe-profiles` | 8081 | Humanos, gatos, autenticación |
| `catastrophe-social` | 8082 | Posts, comentarios, likes, follows, mensajería |
| `catastrophe-adventures` | 8083 | Aventuras, retos PvP, badges, rankings |
| `catastrophe-notifications` | 8084 | Procesamiento de notificaciones |
| `catastrophe-analytics` | 8085 | Personalidades felinas |

## Fases de desarrollo

- [x] **F1**: Cimientos — Estructura multi-módulo, Docker Compose, migraciones
- [x] **F2**: Identidad y perfiles — Registro, login, CRUD gatos, TheCatAPI, sesiones Redis
- [x] **F3**: Red social — Meows, feed, comentarios, likes, follows, mensajería
- [x] **F4**: Gamificación — XP, aventuras, retos PvP, badges, rankings
- [x] **F5**: Inteligencia — Notificaciones, personalidades, OpenWeatherMap, Cat Facts, Resilience4j
- [ ] **F6**: Frontend — Thymeleaf + HTMX, dashboard, documentación
