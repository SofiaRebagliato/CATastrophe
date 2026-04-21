package com.catastrophe.adventures.adapter.out.persistence.repository;

import com.catastrophe.adventures.adapter.out.persistence.entity.ChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaChallengeRepository extends JpaRepository<ChallengeEntity, UUID> {
}
