package com.catastrophe.profiles.adapter.out.persistence.mapper;

import com.catastrophe.profiles.adapter.out.persistence.entity.HumanEntity;
import com.catastrophe.profiles.domain.model.Human;

public final class HumanMapper {

    private HumanMapper() {}

    public static Human toDomain(HumanEntity entity) {
        return new Human(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                entity.getCreatedAt(),
                entity.getLastLogin(),
                entity.isActive()
        );
    }

    public static HumanEntity toEntity(Human human) {
        return new HumanEntity(
                human.id(),
                human.username(),
                human.email(),
                human.passwordHash(),
                human.displayName(),
                human.createdAt(),
                human.lastLogin(),
                human.active()
        );
    }
}
