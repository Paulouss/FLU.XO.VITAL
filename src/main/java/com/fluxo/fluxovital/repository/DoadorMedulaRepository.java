package com.fluxo.fluxovital.repository;

import com.fluxo.fluxovital.model.DoadorMedula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoadorMedulaRepository extends JpaRepository<DoadorMedula, Long> {
    List<DoadorMedula> findByDoadorIdOrderByCriadoEmDesc(Long doadorId);
    List<DoadorMedula> findAllByOrderByCriadoEmDesc();
    boolean existsByCpf(String cpf);
    long countByRg(String rg);
}
