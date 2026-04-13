package com.catastrophe.profiles.adapter.out.persistence;

import com.catastrophe.profiles.adapter.out.persistence.entity.CatEntity;
import com.catastrophe.profiles.adapter.out.persistence.entity.HumanEntity;
import com.catastrophe.profiles.adapter.out.persistence.mapper.CatMapper;
import com.catastrophe.profiles.adapter.out.persistence.mapper.HumanMapper;
import com.catastrophe.profiles.adapter.out.persistence.repository.JpaCatRepository;
import com.catastrophe.profiles.adapter.out.persistence.repository.JpaHumanRepository;
import com.catastrophe.profiles.domain.model.Cat;
import com.catastrophe.profiles.domain.model.Human;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración de la capa de persistencia.
 *
 * Levantan un PostgreSQL real con Testcontainers y verifican
 * que las migraciones Flyway funcionan, las queries JPA son correctas,
 * y los mappers dominio ↔ entidad son coherentes.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({HumanPersistenceAdapter.class, CatPersistenceAdapter.class,
        HumanMapper.class, CatMapper.class})
class PersistenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catastrophe_profiles")
            .withUsername("catastrophe")
            .withPassword("catastrophe_dev");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired private HumanPersistenceAdapter humanAdapter;
    @Autowired private CatPersistenceAdapter catAdapter;
    @Autowired private JpaHumanRepository jpaHumanRepository;
    @Autowired private JpaCatRepository jpaCatRepository;

    @BeforeEach
    void cleanUp() {
        jpaCatRepository.deleteAll();
        jpaHumanRepository.deleteAll();
    }

    @Nested
    @DisplayName("Persistencia de humanos")
    class HumanPersistence {

        @Test
        @DisplayName("Guardar y recuperar un humano por id")
        void saveAndFindById() {
            var human = Human.create("testuser", "test@mail.com", "hashedPwd", "Test User");
            var saved = humanAdapter.save(human);

            assertNotNull(saved.id());
            assertNotNull(saved.createdAt());

            var found = humanAdapter.findById(saved.id());
            assertTrue(found.isPresent());
            assertEquals("testuser", found.get().username());
        }

        @Test
        @DisplayName("Buscar humano por username")
        void findByUsername() {
            var human = Human.create("buscable", "bus@mail.com", "hash", "Buscable");
            humanAdapter.save(human);

            var found = humanAdapter.findByUsername("buscable");
            assertTrue(found.isPresent());
            assertEquals("bus@mail.com", found.get().email());
        }

        @Test
        @DisplayName("existsByUsername devuelve true si existe")
        void existsByUsername() {
            var human = Human.create("existe", "e@mail.com", "hash", "Existe");
            humanAdapter.save(human);

            assertTrue(humanAdapter.existsByUsername("existe"));
            assertFalse(humanAdapter.existsByUsername("noexiste"));
        }

        @Test
        @DisplayName("existsByEmail devuelve true si existe")
        void existsByEmail() {
            var human = Human.create("emailtest", "existe@mail.com", "hash", "Email");
            humanAdapter.save(human);

            assertTrue(humanAdapter.existsByEmail("existe@mail.com"));
            assertFalse(humanAdapter.existsByEmail("noexiste@mail.com"));
        }
    }

    @Nested
    @DisplayName("Persistencia de gatos")
    class CatPersistence {

        private UUID savedHumanId;

        @BeforeEach
        void createHuman() {
            var human = Human.create("cathuman", "cat@mail.com", "hash", "Cat Human");
            savedHumanId = humanAdapter.save(human).id();
        }

        @Test
        @DisplayName("Guardar y recuperar un gato por id")
        void saveAndFindById() {
            var cat = Cat.create(savedHumanId, "Luna", "siamese", 12, "Una gatita");
            var saved = catAdapter.save(cat);

            assertNotNull(saved.id());
            assertNotNull(saved.createdAt());
            assertEquals(0, saved.xp());
            assertEquals(1, saved.level());

            var found = catAdapter.findById(saved.id());
            assertTrue(found.isPresent());
            assertEquals("Luna", found.get().name());
        }

        @Test
        @DisplayName("Listar gatos de un humano")
        void findByHumanId() {
            catAdapter.save(Cat.create(savedHumanId, "Luna", null, 12, null));
            catAdapter.save(Cat.create(savedHumanId, "Sol", null, 8, null));

            var cats = catAdapter.findByHumanId(savedHumanId);
            assertEquals(2, cats.size());
        }

        @Test
        @DisplayName("existsByHumanIdAndName detecta nombres duplicados")
        void existsByHumanIdAndName() {
            catAdapter.save(Cat.create(savedHumanId, "Bigotes", null, 24, null));

            assertTrue(catAdapter.existsByHumanIdAndName(savedHumanId, "Bigotes"));
            assertFalse(catAdapter.existsByHumanIdAndName(savedHumanId, "OtroNombre"));
        }

        @Test
        @DisplayName("Eliminar gato por id")
        void deleteById() {
            var saved = catAdapter.save(Cat.create(savedHumanId, "Temporal", null, 1, null));

            catAdapter.deleteById(saved.id());

            assertTrue(catAdapter.findById(saved.id()).isEmpty());
        }

        @Test
        @DisplayName("El mismo nombre de gato puede existir para humanos diferentes")
        void sameNameDifferentHumans() {
            var human2 = Human.create("otro", "otro@mail.com", "hash", "Otro");
            var human2Id = humanAdapter.save(human2).id();

            catAdapter.save(Cat.create(savedHumanId, "Luna", null, 12, null));
            catAdapter.save(Cat.create(human2Id, "Luna", null, 6, null));

            assertEquals(1, catAdapter.findByHumanId(savedHumanId).size());
            assertEquals(1, catAdapter.findByHumanId(human2Id).size());
        }
    }
}
