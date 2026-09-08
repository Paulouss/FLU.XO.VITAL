package com.fluxo.fluxovital.repository;

import com.fluxo.fluxovital.model.Doador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoadorRepository extends JpaRepository<Doador, Long> {
    Optional<Doador> findByEmail(String email);
}
