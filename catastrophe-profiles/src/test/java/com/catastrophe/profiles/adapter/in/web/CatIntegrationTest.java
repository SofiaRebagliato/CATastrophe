package com.catastrophe.profiles.adapter.in.web;

import com.catastrophe.profiles.adapter.out.persistence.repository.JpaCatRepository;
import com.catastrophe.profiles.adapter.out.persistence.repository.JpaHumanRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
 * Tests de integración del CRUD de gatos con autenticación.
 *
 * Valida que:
 *  - Solo usuarios autenticados pueden crear/editar/borrar gatos
 *  - Los gatos se vinculan al humano autenticado
 *  - Un humano no puede modificar gatos de otro humano
 *  - La lectura de perfiles de gatos es pública
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class CatIntegrationTest {

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
    @Autowired private JpaCatRepository catRepository;

    @BeforeEach
    void cleanUp() {
        catRepository.deleteAll();
        humanRepository.deleteAll();
    }

    /**
     * Registra un humano y devuelve la cookie de sesión tras login.
     */
    private Cookie registerAndLogin(String username, String email) throws Exception {
        // Registrar
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"TestPass123","displayName":"%s"}
                        """.formatted(username, email, username)));

        // Login
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"TestPass123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();

        return loginResult.getResponse().getCookie("SESSION");
    }

    @Test
    @DisplayName("Crear gato autenticado devuelve 201 con datos completos")
    void createCatAuthenticated() throws Exception {
        Cookie session = registerAndLogin("catowner", "owner@mail.com");

        mockMvc.perform(post("/api/v1/cats")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bigotes","breed":"persian","ageMonths":24,"bio":"Un gato persa majestuoso"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Bigotes")))
                .andExpect(jsonPath("$.breed", is("persian")))
                .andExpect(jsonPath("$.bio", is("Un gato persa majestuoso")))
                .andExpect(jsonPath("$.xp", is(0)))
                .andExpect(jsonPath("$.level", is(1)))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.humanId").exists());
    }

    @Test
    @DisplayName("Listar mis gatos devuelve solo los míos")
    void listMyCats() throws Exception {
        Cookie session = registerAndLogin("multicats", "multi@mail.com");

        // Crear dos gatos
        mockMvc.perform(post("/api/v1/cats")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Luna","breed":"siamese","ageMonths":12}
                        """));

        mockMvc.perform(post("/api/v1/cats")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Sol","breed":"bengal","ageMonths":8}
                        """));

        // Listar
        mockMvc.perform(get("/api/v1/cats/mine")
                        .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Luna", "Sol")));
    }

    @Test
    @DisplayName("Actualizar gato propio funciona")
    void updateOwnCat() throws Exception {
        Cookie session = registerAndLogin("updater", "update@mail.com");

        // Crear gato
        String createResponse = mockMvc.perform(post("/api/v1/cats")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Luna","ageMonths":12}
                                """))
                .andReturn().getResponse().getContentAsString();

        String catId = com.fasterxml.jackson.databind.ObjectMapper.class
                .getDeclaredConstructor().newInstance()
                .readTree(createResponse).get("id").asText();

        // Actualizar
        mockMvc.perform(put("/api/v1/cats/" + catId)
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bio":"Bio actualizada con mucho estilo felino"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio", is("Bio actualizada con mucho estilo felino")))
                .andExpect(jsonPath("$.name", is("Luna"))); // No cambió
    }

    @Test
    @DisplayName("No se puede modificar el gato de otro humano")
    void cannotModifyOthersCat() throws Exception {
        // Humano 1 crea un gato
        Cookie session1 = registerAndLogin("humano1", "h1@mail.com");
        String createResponse = mockMvc.perform(post("/api/v1/cats")
                        .cookie(session1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"GatoDeUno","ageMonths":6}
                                """))
                .andReturn().getResponse().getContentAsString();

        String catId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asText();

        // Humano 2 intenta modificar ese gato
        Cookie session2 = registerAndLogin("humano2", "h2@mail.com");
        mockMvc.perform(put("/api/v1/cats/" + catId)
                        .cookie(session2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Robado"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("No se pueden crear dos gatos con el mismo nombre")
    void duplicateCatNameReturns422() throws Exception {
        Cookie session = registerAndLogin("duper", "duper@mail.com");

        // Primer gato
        mockMvc.perform(post("/api/v1/cats")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Duplicado","ageMonths":6}
                        """))
                .andExpect(status().isCreated());

        // Segundo gato con mismo nombre
        mockMvc.perform(post("/api/v1/cats")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicado","ageMonths":12}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Eliminar gato propio devuelve 204")
    void deleteOwnCat() throws Exception {
        Cookie session = registerAndLogin("deleter", "del@mail.com");

        String createResponse = mockMvc.perform(post("/api/v1/cats")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Temporal","ageMonths":1}
                                """))
                .andReturn().getResponse().getContentAsString();

        String catId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asText();

        mockMvc.perform(delete("/api/v1/cats/" + catId)
                        .cookie(session))
                .andExpect(status().isNoContent());

        // Verificar que ya no existe
        mockMvc.perform(get("/api/v1/cats/" + catId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ver perfil de gato es público (sin auth)")
    void viewCatProfileIsPublic() throws Exception {
        Cookie session = registerAndLogin("publicviewer", "pub@mail.com");

        String createResponse = mockMvc.perform(post("/api/v1/cats")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Publico","breed":"ragdoll","ageMonths":18,"bio":"Visible para todos"}
                                """))
                .andReturn().getResponse().getContentAsString();

        String catId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asText();

        // Acceder sin autenticación
        mockMvc.perform(get("/api/v1/cats/" + catId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Publico")))
                .andExpect(jsonPath("$.bio", is("Visible para todos")));
    }
}
