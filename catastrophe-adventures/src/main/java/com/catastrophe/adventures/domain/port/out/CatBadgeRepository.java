package com.catastrophe.adventures.domain.port.out;

import com.catastrophe.adventures.domain.model.CatBadge;

import java.util.List;
import java.util.UUID;

public interface CatBadgeRepository {
    CatBadge save(CatBadge catBadge);
    List<CatBadge> findByCatId(UUID catId);
    boolean exists(UUID catId, UUID badgeId);
    int countByCatId(UUID catId);
}
