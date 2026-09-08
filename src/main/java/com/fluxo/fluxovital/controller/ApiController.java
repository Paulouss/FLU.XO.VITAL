package com.fluxo.fluxovital.controller;

import com.fluxo.fluxovital.repository.DoadorMedulaRepository;
import com.fluxo.fluxovital.repository.DoadorSangueRepository;
import com.fluxo.fluxovital.repository.DoadoraLeiteRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * API simples usada pelos formulários de doação para checar, em tempo real,
 * se um CPF já está cadastrado e quantos cadastros existem com o mesmo RG.
 * Isso evita cadastros duplicados sem precisar reintroduzir perguntas de
 * triagem médica detalhada.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final DoadorSangueRepository doadorSangueRepository;
    private final DoadorMedulaRepository doadorMedulaRepository;
    private final DoadoraLeiteRepository doadoraLeiteRepository;

    public ApiController(DoadorSangueRepository doadorSangueRepository,
                          DoadorMedulaRepository doadorMedulaRepository,
                          DoadoraLeiteRepository doadoraLeiteRepository) {
        this.doadorSangueRepository = doadorSangueRepository;
        this.doadorMedulaRepository = doadorMedulaRepository;
        this.doadoraLeiteRepository = doadoraLeiteRepository;
    }

    // ─── VERIFICAR CPF ──────────────────────────────────────────────────────
    // GET /api/cpf/verificar?cpf=00000000000
    @GetMapping("/cpf/verificar")
    public Map<String, Object> verificarCpf(@RequestParam String cpf) {
        String cpfLimpo = cpf == null ? "" : cpf.replaceAll("\\D", "");

        boolean existe = !cpfLimpo.isBlank() && (
                doadorSangueRepository.existsByCpf(cpfLimpo) ||
                doadorMedulaRepository.existsByCpf(cpfLimpo) ||
                doadoraLeiteRepository.existsByCpf(cpfLimpo)
        );

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("cpf", cpfLimpo);
        resposta.put("existe", existe);
        resposta.put("valido", cpfLimpo.length() == 11);
        return resposta;
    }

    // ─── VERIFICAR RG (quantidade de cadastros) ────────────────────────────
    // GET /api/rg/verificar?rg=1234567
    @GetMapping("/rg/verificar")
    public Map<String, Object> verificarRg(@RequestParam String rg) {
        String rgLimpo = rg == null ? "" : rg.trim();

        long quantidade = 0;
        if (!rgLimpo.isBlank()) {
            quantidade += doadorSangueRepository.countByRg(rgLimpo);
            quantidade += doadorMedulaRepository.countByRg(rgLimpo);
            quantidade += doadoraLeiteRepository.countByRg(rgLimpo);
        }

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("rg", rgLimpo);
        resposta.put("quantidade", quantidade);
        resposta.put("existe", quantidade > 0);
        return resposta;
    }
}
