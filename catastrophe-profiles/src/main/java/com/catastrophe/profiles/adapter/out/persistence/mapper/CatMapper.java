package com.catastrophe.profiles.adapter.out.persistence.mapper;

import com.catastrophe.profiles.adapter.out.persistence.entity.CatEntity;
import com.catastrophe.profiles.domain.model.Cat;

public final class CatMapper {

    private CatMapper() {}

    public static Cat toDomain(CatEntity entity) {
        return new Cat(
                entity.getId(),
                entity.getHumanId(),
                entity.getName(),
                entity.getBreed(),
                entity.getAgeMonths(),
                entity.getAvatarUrl(),
                entity.getBio(),
                entity.getXp(),
                entity.getLevel(),
                entity.getMood(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static CatEntity toEntity(Cat cat) {
        return new CatEntity(
                cat.id(),
                cat.humanId(),
                cat.name(),
                cat.breed(),
                cat.ageMonths(),
                cat.avatarUrl(),
                cat.bio(),
                cat.xp(),
                cat.level(),
                cat.mood(),
                cat.createdAt(),
                cat.updatedAt()
        );
    }
}
