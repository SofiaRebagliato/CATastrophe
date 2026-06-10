package com.catastrophe.profiles.adapter.out.persistence;

import com.catastrophe.profiles.adapter.out.persistence.mapper.HumanMapper;
import com.catastrophe.profiles.adapter.out.persistence.repository.JpaHumanRepository;
import com.catastrophe.profiles.domain.model.Human;
import com.catastrophe.profiles.domain.port.out.HumanRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class HumanPersistenceAdapter implements HumanRepository {

    private final JpaHumanRepository jpaRepository;

    public HumanPersistenceAdapter(JpaHumanRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Human save(Human human) {
        var entity = HumanMapper.toEntity(human);
        return HumanMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Human> findById(UUID id) {
        return jpaRepository.findById(id).map(HumanMapper::toDomain);
    }

    @Override
    public Optional<Human> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(HumanMapper::toDomain);
    }

    @Override
    public List<Human> searchActive(String query, int limit) {
        return jpaRepository.searchActive(query, PageRequest.of(0, limit))
                .stream().map(HumanMapper::toDomain).toList();
    }

    @Override
    public Optional<Human> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(HumanMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
