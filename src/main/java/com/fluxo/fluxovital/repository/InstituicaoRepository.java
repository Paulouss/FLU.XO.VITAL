package com.fluxo.fluxovital.repository;

import com.fluxo.fluxovital.model.Instituicao;
import com.fluxo.fluxovital.model.CategoriaInstituicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstituicaoRepository extends JpaRepository<Instituicao, Long> {
    Optional<Instituicao> findByEmail(String email);
    Optional<Instituicao> findByEmailAndCategoria(String email, CategoriaInstituicao categoria);
    boolean existsByCnpj(String cnpj);

    // ─── Busca de instituições pelo doador (tela "Ver locais") ────────────────
    List<Instituicao> findAllByOrderByNomeAsc();
    List<Instituicao> findByCategoriaOrderByNomeAsc(CategoriaInstituicao categoria);
    List<Instituicao> findByCidadeContainingIgnoreCaseOrderByNomeAsc(String cidade);
    List<Instituicao> findByCategoriaAndCidadeContainingIgnoreCaseOrderByNomeAsc(
            CategoriaInstituicao categoria, String cidade);
}
