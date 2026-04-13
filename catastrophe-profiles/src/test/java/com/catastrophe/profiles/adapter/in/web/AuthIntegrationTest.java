package com.catastrophe.profiles.adapter.in.web;

import com.catastrophe.profiles.adapter.out.persistence.entity.HumanEntity;
import com.catastrophe.profiles.adapter.out.persistence.repository.JpaHumanRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del flujo de autenticación completo.
 *
 * Levanta contenedores reales de PostgreSQL, Redis y Kafka
 * con Testcontainers para probar el flujo end-to-end:
 *  - Registro → Login → Acceso autenticado → Logout
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catastrophe_profiles")
            .withUsername("catastrophe")
            .withPassword("catastrophe_dev");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JpaHumanRepository humanRepository;

    @BeforeEach
    void cleanUp() {
        humanRepository.deleteAll();
    }

    @Test
    @DisplayName("Registro exitoso devuelve 201 y datos del humano")
    void registerReturns201() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "gatero99",
                                    "email": "gatero@mail.com",
                                    "password": "MiPassword123",
                                    "displayName": "El Gatero"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("gatero99")))
                .andExpect(jsonPath("$.email", is("gatero@mail.com")))
                .andExpect(jsonPath("$.displayName", is("El Gatero")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.message", containsString("Bienvenido")));
    }

    @Test
    @DisplayName("Registro con username duplicado devuelve 409")
    void registerDuplicateUsernameReturns409() throws Exception {
        // Registrar primero
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"duplicado","email":"uno@mail.com","password":"Pass12345","displayName":"Uno"}
                        """));

        // Intentar duplicar
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"duplicado","email":"dos@mail.com","password":"Pass12345","displayName":"Dos"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Registro con datos inválidos devuelve 400")
    void registerInvalidDataReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","email":"invalido","password":"123","displayName":"X"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login exitoso devuelve 200 y crea sesión")
    void loginReturns200WithSession() throws Exception {
        // Registrar
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"loginer","email":"login@mail.com","password":"Pass12345","displayName":"Login User"}
                        """));

        // Login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"loginer","password":"Pass12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("loginer")))
                .andExpect(cookie().exists("SESSION"));
    }

    @Test
    @DisplayName("Login con credenciales incorrectas devuelve 401")
    void loginBadCredentialsReturns401() throws Exception {
        // Registrar
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"wrongpwd","email":"wrong@mail.com","password":"Pass12345","displayName":"Wrong"}
                        """));

        // Login con contraseña incorrecta
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"wrongpwd","password":"WrongPassword"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Acceso a /api/v1/auth/me sin sesión devuelve 401")
    void meWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Flujo completo: registro → login → me → logout")
    void fullAuthFlow() throws Exception {
        // 1. Registrar
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"flowuser","email":"flow@mail.com","password":"FlowPass123","displayName":"Flow User"}
                        """))
                .andExpect(status().isCreated());

        // 2. Login (capturar cookie de sesión)
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"flowuser","password":"FlowPass123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertNotNull(sessionCookie, "Debe devolver cookie SESSION de Redis");

        // 3. Acceder a /me con sesión
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("flowuser")));

        // 4. Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("cerrada")));
    }

    @Test
    @DisplayName("GET /api/v1/cats es público (no requiere autenticación)")
    void getCatsIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/cats/human/" + java.util.UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/cats sin autenticación devuelve 401")
    void createCatWithoutAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/cats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Luna","breed":"siamese","ageMonths":12,"bio":"Una gatita"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
