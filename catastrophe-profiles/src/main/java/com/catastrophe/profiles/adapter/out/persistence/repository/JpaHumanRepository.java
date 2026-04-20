package com.catastrophe.profiles.adapter.out.persistence.repository;

import com.catastrophe.profiles.adapter.out.persistence.entity.HumanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaHumanRepository extends JpaRepository<HumanEntity, UUID> {

    Optional<HumanEntity> findByUsername(String username);

    Optional<HumanEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
