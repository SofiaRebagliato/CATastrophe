package com.catastrophe.profiles.domain.service;

import com.catastrophe.profiles.domain.port.out.HumanRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de UserDetailsService que conecta Spring Security
 * con nuestro dominio de humanos.
 *
 * Spring Security llama a loadUserByUsername() durante la autenticación
 * para obtener las credenciales almacenadas y verificar la contraseña.
 */
@Service
public class CatastropheUserDetailsService implements UserDetailsService {

    private final HumanRepository humanRepository;

    public CatastropheUserDetailsService(HumanRepository humanRepository) {
        this.humanRepository = humanRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var human = humanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Humano '%s' no encontrado. ¿Seguro que eres un asistente de gatos?"
                                .formatted(username)));

        if (!human.active()) {
            throw new UsernameNotFoundException(
                    "La cuenta de '%s' está desactivada.".formatted(username));
        }

        return new User(
                human.username(),
                human.passwordHash(),
                human.active(),      // enabled
                true,                // accountNonExpired
                true,                // credentialsNonExpired
                true,                // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_HUMAN"))
        );
    }
}
