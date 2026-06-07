package com.catastrophe.profiles.adapter.out.persistence.repository;

import com.catastrophe.profiles.adapter.out.persistence.entity.HumanEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaHumanRepository extends JpaRepository<HumanEntity, UUID> {

    Optional<HumanEntity> findByUsername(String username);

    Optional<HumanEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Búsqueda de humanos activos por username o nombre visible
     * (parcial, case-insensitive).
     */
    @Query("""
            SELECT h FROM HumanEntity h
            WHERE h.active = true
              AND (LOWER(h.username) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(h.displayName) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY h.username ASC
            """)
    List<HumanEntity> searchActive(@Param("q") String q, Pageable pageable);
}
