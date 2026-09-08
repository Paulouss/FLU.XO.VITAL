package com.fluxo.fluxovital.repository;

import com.fluxo.fluxovital.model.DoadoraLeite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DoadoraLeiteRepository extends JpaRepository<DoadoraLeite, Long> {
    List<DoadoraLeite> findByDoadorIdOrderByCriadoEmDesc(Long doadorId);
    List<DoadoraLeite> findAllByOrderByCriadoEmDesc();
    boolean existsByCpf(String cpf);
    long countByRg(String rg);
}
