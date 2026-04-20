package com.catastrophe.profiles.adapter.out.persistence;

import com.catastrophe.profiles.adapter.out.persistence.mapper.CatMapper;
import com.catastrophe.profiles.adapter.out.persistence.repository.JpaCatRepository;
import com.catastrophe.profiles.domain.model.Cat;
import com.catastrophe.profiles.domain.port.out.CatRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CatPersistenceAdapter implements CatRepository {

    private final JpaCatRepository jpaRepository;

    public CatPersistenceAdapter(JpaCatRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Cat save(Cat cat) {
        var entity = CatMapper.toEntity(cat);
        return CatMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Cat> findById(UUID id) {
        return jpaRepository.findById(id).map(CatMapper::toDomain);
    }

    @Override
    public List<Cat> findByHumanId(UUID humanId) {
        return jpaRepository.findByHumanId(humanId).stream()
                .map(CatMapper::toDomain).toList();
    }

    @Override
    public boolean existsByHumanIdAndName(UUID humanId, String name) {
        return jpaRepository.existsByHumanIdAndName(humanId, name);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
