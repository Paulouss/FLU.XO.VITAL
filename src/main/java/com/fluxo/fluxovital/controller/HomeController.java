package com.fluxo.fluxovital.controller;

import com.fluxo.fluxovital.model.DoadorMedula;
import com.fluxo.fluxovital.model.DoadorSangue;
import com.fluxo.fluxovital.model.DoadoraLeite;
import com.fluxo.fluxovital.repository.DoadorMedulaRepository;
import com.fluxo.fluxovital.repository.DoadorSangueRepository;
import com.fluxo.fluxovital.repository.DoadoraLeiteRepository;
import com.fluxo.fluxovital.repository.InstituicaoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class HomeController {

    private final DoadorSangueRepository doadorSangueRepository;
    private final DoadoraLeiteRepository doadoraLeiteRepository;
    private final DoadorMedulaRepository doadorMedulaRepository;
    private final InstituicaoRepository  instituicaoRepository;

    public HomeController(DoadorSangueRepository doadorSangueRepository,
                           DoadoraLeiteRepository doadoraLeiteRepository,
                           DoadorMedulaRepository doadorMedulaRepository,
                           InstituicaoRepository instituicaoRepository) {
        this.doadorSangueRepository = doadorSangueRepository;
        this.doadoraLeiteRepository = doadoraLeiteRepository;
        this.doadorMedulaRepository = doadorMedulaRepository;
        this.instituicaoRepository  = instituicaoRepository;
    }

    /** Página inicial */
    @GetMapping("/")
    public String index() {
        return "home/index";
    }

    /** DTO simples só para exibir a lista unificada de agendamentos recentes na home */
    public static class AgendamentoResumo {
        private final String tipo;
        private final String icone;
        private final String unidade;
        private final java.time.LocalDate data;
        private final String status;
        private final LocalDateTime criadoEm;

        public AgendamentoResumo(String tipo, String icone, String unidade,
                                  java.time.LocalDate data, String status, LocalDateTime criadoEm) {
            this.tipo = tipo;
            this.icone = icone;
            this.unidade = unidade;
            this.data = data;
            this.status = status;
            this.criadoEm = criadoEm;
        }

        public String getTipo() { return tipo; }
        public String getIcone() { return icone; }
        public String getUnidade() { return unidade; }
        public java.time.LocalDate getData() { return data; }
        public String getStatus() { return status; }
        public LocalDateTime getCriadoEm() { return criadoEm; }
    }

    /** Escolha entre Doador e Instituição */
    @GetMapping("/cadastro/tipos")
    public String tiposCadastros() {
        return "cadastro/tipos_cadastros";
    }

    /** Escolha de categoria do doador */
    @GetMapping("/cadastro/doador/categoria")
    public String escolhaCategoriaDoador() {
        return "cadastro/escolha_categoria";
    }

    /** Seleciona tipo de doação e redireciona para cadastro */
    @GetMapping("/cadastro/doador/selecionar")
    public String selecionarTipo(@RequestParam String tipo, HttpSession session) {
        session.setAttribute("tipo_doacao", tipo);
        return "redirect:/doador/cadastro";
    }

    /** Login do doador (atalho na página inicial) */
    @GetMapping("/doador")
    public String loginDoadorRedirect() {
        return "redirect:/doador/login";
    }

    /** Página institucional: Sobre */
    @GetMapping("/sobre")
    public String sobre() {
        return "institucional/sobre";
    }

    /** Página institucional: Como funciona o processo de doação */
    @GetMapping("/processo")
    public String processo() {
        return "institucional/processo";
    }

    /** Página institucional: Perguntas frequentes */
    @GetMapping("/faq")
    public String faq() {
        return "institucional/faq";
    }

    /** Página institucional: Campanhas */
    @GetMapping("/campanhas")
    public String campanhas() {
        return "institucional/campanhas";
    }

    /** Página institucional: Relatórios — números reais do sistema (e resumo pessoal, se o doador estiver logado) */
    @GetMapping("/relatorios")
    public String relatorios(HttpSession session, Model model) {
        model.addAttribute("totalSangue", doadorSangueRepository.count());
        model.addAttribute("totalMedula", doadorMedulaRepository.count());
        model.addAttribute("totalLeite", doadoraLeiteRepository.count());
        model.addAttribute("totalInstituicoes", instituicaoRepository.count());

        long realizadosSangue = doadorSangueRepository.findAllByOrderByCriadoEmDesc().stream()
                .filter(s -> s.getStatus() == DoadorSangue.StatusTriagem.realizado).count();
        long realizadosMedula = doadorMedulaRepository.findAllByOrderByCriadoEmDesc().stream()
                .filter(m -> m.getStatus() == DoadorMedula.StatusMedula.realizado).count();
        long realizadosLeite = doadoraLeiteRepository.findAllByOrderByCriadoEmDesc().stream()
                .filter(l -> l.getStatus() == DoadoraLeite.StatusDoadoraLeite.realizado).count();
        model.addAttribute("totalRealizadas", realizadosSangue + realizadosMedula + realizadosLeite);

        // Resumo pessoal do doador, exibido aqui em vez da página inicial
        Long doadorId = (Long) session.getAttribute("doador_id");
        if (doadorId != null) {
            List<DoadorSangue> sangue = doadorSangueRepository.findByDoadorIdOrderByCriadoEmDesc(doadorId);
            List<DoadorMedula> medula = doadorMedulaRepository.findByDoadorIdOrderByCriadoEmDesc(doadorId);
            List<DoadoraLeite> leite  = doadoraLeiteRepository.findByDoadorIdOrderByCriadoEmDesc(doadorId);

            int total        = sangue.size() + medula.size() + leite.size();
            long pendentes   = sangue.stream().filter(s -> s.getStatus() == DoadorSangue.StatusTriagem.pendente).count()
                             + medula.stream().filter(m -> m.getStatus() == DoadorMedula.StatusMedula.pendente).count()
                             + leite.stream().filter(l -> l.getStatus() == DoadoraLeite.StatusDoadoraLeite.pendente).count();
            long confirmados = sangue.stream().filter(s -> s.getStatus() == DoadorSangue.StatusTriagem.confirmado).count()
                             + medula.stream().filter(m -> m.getStatus() == DoadorMedula.StatusMedula.confirmado).count()
                             + leite.stream().filter(l -> l.getStatus() == DoadoraLeite.StatusDoadoraLeite.confirmado).count();
            long realizados  = sangue.stream().filter(s -> s.getStatus() == DoadorSangue.StatusTriagem.realizado).count()
                             + medula.stream().filter(m -> m.getStatus() == DoadorMedula.StatusMedula.realizado).count()
                             + leite.stream().filter(l -> l.getStatus() == DoadoraLeite.StatusDoadoraLeite.realizado).count();

            List<AgendamentoResumo> recentes = new ArrayList<>();
            for (DoadorSangue s : sangue) {
                recentes.add(new AgendamentoResumo("Sangue", "bi-droplet-fill", s.getUnidade(),
                        s.getDataAgendamento(), s.getStatus().name(), s.getCriadoEm()));
            }
            for (DoadorMedula m : medula) {
                recentes.add(new AgendamentoResumo("Medula Óssea", "bi-heart-pulse-fill", m.getUnidade(),
                        m.getDataColeta(), m.getStatus().name(), m.getCriadoEm()));
            }
            for (DoadoraLeite l : leite) {
                recentes.add(new AgendamentoResumo("Leite Materno", "bi-cup-hot-fill", l.getUnidade(),
                        l.getDataAgendamento(), l.getStatus().name(), l.getCriadoEm()));
            }
            recentes.sort(Comparator.comparing(AgendamentoResumo::getCriadoEm,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            List<AgendamentoResumo> ultimos = recentes.size() > 5 ? recentes.subList(0, 5) : recentes;

            model.addAttribute("doadorLogado", true);
            model.addAttribute("doadorNome", session.getAttribute("doador_nome_exibicao"));
            model.addAttribute("resumoTotal", total);
            model.addAttribute("resumoPendentes", pendentes);
            model.addAttribute("resumoConfirmados", confirmados);
            model.addAttribute("resumoRealizados", realizados);
            model.addAttribute("resumoRecentes", ultimos);
        } else {
            model.addAttribute("doadorLogado", false);
        }

        return "institucional/relatorios";
    }

    /** Página institucional: Contato */
    @GetMapping("/contato")
    public String contato() {
        return "institucional/contato";
    }
}
