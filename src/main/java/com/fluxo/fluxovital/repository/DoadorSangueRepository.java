package com.fluxo.fluxovital.repository;

import com.fluxo.fluxovital.model.DoadorSangue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoadorSangueRepository extends JpaRepository<DoadorSangue, Long> {
    List<DoadorSangue> findByDoadorIdOrderByCriadoEmDesc(Long doadorId);
    List<DoadorSangue> findAllByOrderByCriadoEmDesc();
    boolean existsByCpf(String cpf);
    long countByRg(String rg);
}
