package com.fluxo.fluxovital.controller;

import com.fluxo.fluxovital.model.Doador;
import com.fluxo.fluxovital.model.DoadorMedula;
import com.fluxo.fluxovital.model.DoadorSangue;
import com.fluxo.fluxovital.model.DoadoraLeite;
import com.fluxo.fluxovital.repository.DoadorMedulaRepository;
import com.fluxo.fluxovital.repository.DoadorRepository;
import com.fluxo.fluxovital.repository.DoadorSangueRepository;
import com.fluxo.fluxovital.repository.DoadoraLeiteRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fluxo.fluxovital.util.ValidacaoDoacao;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;

@Controller
public class DoadorController {

    private final DoadorRepository        doadorRepository;
    private final DoadorSangueRepository  doadorSangueRepository;
    private final DoadoraLeiteRepository  doadoraLeiteRepository;
    private final DoadorMedulaRepository  doadorMedulaRepository;
    private final PasswordEncoder         passwordEncoder;

    public DoadorController(DoadorRepository doadorRepository,
                            DoadorSangueRepository doadorSangueRepository,
                            DoadoraLeiteRepository doadoraLeiteRepository,
                            DoadorMedulaRepository doadorMedulaRepository,
                            PasswordEncoder passwordEncoder) {
        this.doadorRepository       = doadorRepository;
        this.doadorSangueRepository = doadorSangueRepository;
        this.doadoraLeiteRepository = doadoraLeiteRepository;
        this.doadorMedulaRepository = doadorMedulaRepository;
        this.passwordEncoder        = passwordEncoder;
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @GetMapping("/doador/login")
    public String exibirLogin() {
        return "doador/login";
    }

    @PostMapping("/doador/login")
    public String processarLogin(
            @RequestParam String email,
            @RequestParam String senha,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (email.isBlank() || senha.isBlank()) {
            redirectAttributes.addFlashAttribute("erro", "Preencha e-mail e senha.");
            return "redirect:/doador/login";
        }

        Optional<Doador> opt = doadorRepository.findByEmail(email.trim());
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "E-mail não encontrado.");
            return "redirect:/doador/login";
        }

        Doador d = opt.get();
        if (!passwordEncoder.matches(senha, d.getSenha())) {
            redirectAttributes.addFlashAttribute("erro", "Senha incorreta.");
            return "redirect:/doador/login";
        }

        preencherSessao(session, d);
        return "redirect:/doador/home";
    }

    // ─── CADASTRO ─────────────────────────────────────────────────────────────

    @GetMapping("/doador/cadastro")
    public String exibirCadastro(HttpSession session, Model model) {
        if (session.getAttribute("tipo_doacao") == null)
            session.setAttribute("tipo_doacao", "sangue");
        model.addAttribute("tipoDoacao", session.getAttribute("tipo_doacao"));
        model.addAttribute("anoAtual", Year.now().getValue());
        return "doador/cadastro/cadastro_doador";
    }

    @PostMapping("/doador/cadastro")
    public String processarCadastro(
            @RequestParam String nome,
            @RequestParam String email,
            @RequestParam String telefone,
            @RequestParam String senha,
            @RequestParam("confirma_senha") String confirmaSenha,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String tipoDoador = (String) session.getAttribute("tipo_doacao");
        if (tipoDoador == null || !List.of("sangue","medula","leite").contains(tipoDoador))
            tipoDoador = "sangue";

        if (nome.isBlank() || email.isBlank() || telefone.isBlank() || senha.isBlank()) {
            redirectAttributes.addFlashAttribute("erro", "Preencha todos os campos.");
            return "redirect:/doador/cadastro";
        }
        if (!senha.equals(confirmaSenha)) {
            redirectAttributes.addFlashAttribute("erro", "As senhas nao coincidem.");
            return "redirect:/doador/cadastro";
        }

        Optional<Doador> existente = doadorRepository.findByEmail(email.trim());
        if (existente.isPresent()) {
            Doador d = existente.get();
            if (passwordEncoder.matches(senha, d.getSenha())) {
                preencherSessao(session, d);
                return "redirect:/doador/home";
            }
            redirectAttributes.addFlashAttribute("erro", "E-mail ja cadastrado com senha diferente.");
            return "redirect:/doador/cadastro";
        }

        Doador novo = new Doador();
        novo.setNome(nome.trim());
        novo.setEmail(email.trim());
        novo.setTelefone(telefone.trim());
        novo.setSenha(passwordEncoder.encode(senha));
        novo.setTipoDoador(tipoDoador);
        doadorRepository.save(novo);

        preencherSessao(session, novo);
        redirectAttributes.addFlashAttribute("sucesso",
            "Cadastro realizado com sucesso! Agora preencha seu formulario de doacao.");
        return "redirect:/doador/home";
    }

    // ─── HOME ─────────────────────────────────────────────────────────────────

    @GetMapping("/doador/home")
    public String homeDoador(HttpSession session, Model model) {
        if (session.getAttribute("doador_id") == null)
            return "redirect:/doador/login";
        model.addAttribute("nome",       session.getAttribute("doador_nome"));
        model.addAttribute("tipoDoador", session.getAttribute("tipo_doacao"));

        Long id = (Long) session.getAttribute("doador_id");
        model.addAttribute("formulariosSangue",
            doadorSangueRepository.findByDoadorIdOrderByCriadoEmDesc(id));
        model.addAttribute("formulariosLeite",
            doadoraLeiteRepository.findByDoadorIdOrderByCriadoEmDesc(id));
        model.addAttribute("formulariosMedula",
            doadorMedulaRepository.findByDoadorIdOrderByCriadoEmDesc(id));
        return "doador/home";
    }

    // ─── FORMULÁRIO DE SANGUE ─────────────────────────────────────────────────

    @GetMapping("/doador/sangue/form")
    public String formSangue(HttpSession session, Model model) {
        if (session.getAttribute("doador_id") == null) return "redirect:/doador/login";
        model.addAttribute("doadorNome", session.getAttribute("doador_nome"));
        model.addAttribute("anoAtual", Year.now().getValue());
        return "doador/sangue/form";
    }

    @PostMapping("/doador/sangue/form")
    public String salvarSangue(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String rg,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nascimento,
            @RequestParam(required = false) String sexo,
            @RequestParam(required = false) Double peso,
            @RequestParam(required = false) Double altura,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telefone,
            @RequestParam(required = false) String endereco,
            @RequestParam(required = false) String tipo_sanguineo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ultima_doacao,
            @RequestParam(required = false) String historico_doacao,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) String interesse_doador,
            @RequestParam(required = false) String condicao_saude,
            @RequestParam(required = false) String descricao_condicao,
            @RequestParam(required = false) String unidade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_agendamento,
            @RequestParam(required = false) String turno,
            @RequestParam(required = false) String como_soube,
            @RequestParam(required = false) String consentimento,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("doador_id") == null) return "redirect:/doador/login";

        Long doadorId = (Long) session.getAttribute("doador_id");
        Doador doador = doadorRepository.findById(doadorId).orElse(null);

        String cpfLimpo = cpf != null ? cpf.replaceAll("\\D", "") : null;
        if (cpfLimpo != null && !cpfLimpo.isBlank() && doadorSangueRepository.existsByCpf(cpfLimpo)) {
            redirectAttributes.addFlashAttribute("erro",
                "Este CPF ja possui um agendamento de doacao de sangue. Verifique seus formularios em 'Meus agendamentos'.");
            return "redirect:/doador/sangue/form";
        }

        List<String> erros = ValidacaoDoacao.coletarErros(
            ValidacaoDoacao.validarCpf(cpf),
            ValidacaoDoacao.validarRg(rg),
            ValidacaoDoacao.validarIdade(nascimento, 16, 69, "Doação de sangue"),
            ValidacaoDoacao.validarImc(peso, altura),
            doador != null ? ValidacaoDoacao.validarConsistenciaCpf(doador.getCpf(), cpf) : null,
            doador != null ? ValidacaoDoacao.validarConsistenciaEmail(doador.getEmail(), email) : null
        );
        if (!erros.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", String.join(" ", erros));
            return "redirect:/doador/sangue/form";
        }
        if (doador != null && (doador.getCpf() == null || doador.getCpf().isBlank())) {
            doador.setCpf(cpfLimpo);
            doadorRepository.save(doador);
        }

        DoadorSangue ds = new DoadorSangue();
        ds.setDoador(doador);
        ds.setNome(nome != null ? nome.trim() : (String) session.getAttribute("doador_nome"));
        ds.setCpf(cpfLimpo);
        ds.setRg(rg);
        ds.setNascimento(nascimento);
        if (sexo != null && !sexo.isBlank()) {
            try { ds.setSexo(DoadorSangue.Sexo.valueOf(sexo.trim().toLowerCase())); }
            catch (IllegalArgumentException e) { ds.setSexo(null); }
        }
        ds.setPeso(peso);
        ds.setAltura(altura);
        ds.setEmail(email);
        ds.setTelefone(telefone);
        ds.setEndereco(endereco);
        ds.setTipoSanguineo(tipo_sanguineo);
        ds.setUltimaDoacao(ultima_doacao);
        if (historico_doacao != null && !historico_doacao.isBlank()) {
            try { ds.setHistoricoDoacao(DoadorSangue.HistoricoDoacao.valueOf(historico_doacao)); }
            catch (Exception ignored) {}
        }
        ds.setObservacoes(observacoes);
        ds.setInteresseDoador("nao".equals(interesse_doador) ? Boolean.FALSE : Boolean.TRUE);
        ds.setCondicaoSaude("1".equals(condicao_saude) || "sim".equals(condicao_saude));
        ds.setDescricaoCondicao(descricao_condicao);
        ds.setUnidade(unidade);
        ds.setDataAgendamento(data_agendamento);
        if (turno != null && !turno.isBlank()) {
            try { ds.setTurno(DoadorSangue.Turno.valueOf(turno)); } catch (Exception ignored) {}
        }
        ds.setComoSoube(como_soube);
        ds.setConsentimento("1".equals(consentimento));
        doadorSangueRepository.save(ds);

        redirectAttributes.addFlashAttribute("sucesso",
            "Formulario de doacao de sangue enviado com sucesso! Seu agendamento foi registrado como pendente ate a confirmacao da instituicao.");
        return "redirect:/doador/home";
    }

    // ─── FORMULÁRIO DE MEDULA ─────────────────────────────────────────────────

    @GetMapping("/doador/medula/form")
    public String formMedula(HttpSession session, Model model) {
        if (session.getAttribute("doador_id") == null) return "redirect:/doador/login";
        model.addAttribute("doadorNome", session.getAttribute("doador_nome"));
        model.addAttribute("anoAtual", Year.now().getValue());
        return "doador/medula/form";
    }

    @PostMapping("/doador/medula/form")
    public String salvarMedula(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String rg,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nascimento,
            @RequestParam(required = false) String sexo,
            @RequestParam(required = false) Double peso,
            @RequestParam(required = false) Double altura,
            @RequestParam(required = false) String etnia,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telefone,
            @RequestParam(required = false) String historico_medula,
            @RequestParam(required = false) String cadastro_redome,
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) String interesse_doador,
            @RequestParam(required = false) String condicao_saude,
            @RequestParam(required = false) String descricao_condicao,
            @RequestParam(required = false) String unidade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_coleta,
            @RequestParam(required = false) String turno,
            @RequestParam(required = false) String como_soube,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) String consentimento,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("doador_id") == null) return "redirect:/doador/login";

        Long doadorId = (Long) session.getAttribute("doador_id");
        Doador doador = doadorRepository.findById(doadorId).orElse(null);

        String cpfLimpo = cpf != null ? cpf.replaceAll("\\D", "") : null;
        if (cpfLimpo != null && !cpfLimpo.isBlank() && doadorMedulaRepository.existsByCpf(cpfLimpo)) {
            redirectAttributes.addFlashAttribute("erro",
                "Este CPF ja possui um cadastro de doador de medula. Verifique seus formularios em 'Meus agendamentos'.");
            return "redirect:/doador/medula/form";
        }

        List<String> erros = ValidacaoDoacao.coletarErros(
            ValidacaoDoacao.validarCpf(cpf),
            ValidacaoDoacao.validarRg(rg),
            ValidacaoDoacao.validarIdade(nascimento, 18, 55, "Doação de medula óssea (REDOME)"),
            ValidacaoDoacao.validarImc(peso, altura),
            doador != null ? ValidacaoDoacao.validarConsistenciaCpf(doador.getCpf(), cpf) : null,
            doador != null ? ValidacaoDoacao.validarConsistenciaEmail(doador.getEmail(), email) : null
        );
        if (!erros.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", String.join(" ", erros));
            return "redirect:/doador/medula/form";
        }
        if (doador != null && (doador.getCpf() == null || doador.getCpf().isBlank())) {
            doador.setCpf(cpfLimpo);
            doadorRepository.save(doador);
        }

        DoadorMedula dm = new DoadorMedula();
        dm.setDoador(doador);
        dm.setNome(nome != null ? nome.trim() : (String) session.getAttribute("doador_nome"));
        dm.setCpf(cpfLimpo);
        dm.setRg(rg);
        dm.setNascimento(nascimento);
        dm.setSexo(sexo);
        dm.setPeso(peso);
        dm.setAltura(altura);
        dm.setEtnia(etnia);
        dm.setEmail(email);
        dm.setTelefone(telefone);

        dm.setHistoricoMedula(historico_medula);
        dm.setCadastroRedome(cadastro_redome);
        dm.setModalidade(modalidade);
        dm.setInteresseDoador("nao".equals(interesse_doador) ? Boolean.FALSE : Boolean.TRUE);
        dm.setCondicaoSaude("1".equals(condicao_saude) || "sim".equals(condicao_saude));
        dm.setDescricaoCondicao(descricao_condicao);
        dm.setUnidade(unidade);
        dm.setDataColeta(data_coleta);
        dm.setTurno(turno);
        dm.setComoSoube(como_soube);
        dm.setObservacoes(observacoes);
        dm.setConsentimento("1".equals(consentimento));

        doadorMedulaRepository.save(dm);

        redirectAttributes.addFlashAttribute("sucesso",
            "Cadastro de doador de medula ossea enviado com sucesso! Seu agendamento ficara pendente ate a confirmacao da instituicao.");
        return "redirect:/doador/home";
    }

    // ─── FORMULÁRIO DE LEITE ──────────────────────────────────────────────────

    @GetMapping("/doador/leite/form")
    public String formLeite(HttpSession session, Model model) {
        if (session.getAttribute("doador_id") == null) return "redirect:/doador/login";
        model.addAttribute("doadorNome", session.getAttribute("doador_nome"));
        model.addAttribute("anoAtual", Year.now().getValue());
        return "doador/leite/form";
    }

    @PostMapping("/doador/leite/form")
    public String salvarLeite(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String rg,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nascimento,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telefone,
            @RequestParam(required = false) String endereco,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_parto,
            @RequestParam(required = false) String tipo_parto,
            @RequestParam(required = false) String idade_gestacional,
            @RequestParam(required = false) String num_filhos_amamentando,
            @RequestParam(required = false) String amamentacao_exclusiva,
            @RequestParam(required = false) String producao_estimada,
            @RequestParam(required = false) String historico_doacao,
            @RequestParam(required = false) String modalidade_coleta,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) String interesse_doador,
            @RequestParam(required = false) String condicao_saude,
            @RequestParam(required = false) String descricao_condicao,
            @RequestParam(required = false) String unidade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_agendamento,
            @RequestParam(required = false) String turno,
            @RequestParam(required = false) String como_soube,
            @RequestParam(required = false) String consentimento,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("doador_id") == null) return "redirect:/doador/login";

        Long doadorId = (Long) session.getAttribute("doador_id");
        Doador doador = doadorRepository.findById(doadorId).orElse(null);

        String cpfLimpo = cpf != null ? cpf.replaceAll("\\D", "") : null;
        if (cpfLimpo != null && !cpfLimpo.isBlank() && doadoraLeiteRepository.existsByCpf(cpfLimpo)) {
            redirectAttributes.addFlashAttribute("erro",
                "Este CPF ja possui um agendamento de doacao de leite. Verifique seus formularios em 'Meus agendamentos'.");
            return "redirect:/doador/leite/form";
        }

        List<String> erros = ValidacaoDoacao.coletarErros(
            ValidacaoDoacao.validarCpf(cpf),
            ValidacaoDoacao.validarRg(rg),
            ValidacaoDoacao.validarIdade(nascimento, 16, 45, "Doação de leite materno"),
            doador != null ? ValidacaoDoacao.validarConsistenciaCpf(doador.getCpf(), cpf) : null,
            doador != null ? ValidacaoDoacao.validarConsistenciaEmail(doador.getEmail(), email) : null
        );
        if (!erros.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", String.join(" ", erros));
            return "redirect:/doador/leite/form";
        }
        if (doador != null && (doador.getCpf() == null || doador.getCpf().isBlank())) {
            doador.setCpf(cpfLimpo);
            doadorRepository.save(doador);
        }

        DoadoraLeite dl = new DoadoraLeite();
        dl.setDoador(doador);
        dl.setNome(nome != null ? nome.trim() : (String) session.getAttribute("doador_nome"));
        dl.setCpf(cpfLimpo);
        dl.setRg(rg);
        dl.setNascimento(nascimento);
        dl.setEmail(email);
        dl.setTelefone(telefone);
        dl.setEndereco(endereco);
        dl.setDataParto(data_parto);
        if (tipo_parto != null && !tipo_parto.isBlank()) {
            try { dl.setTipoParto(DoadoraLeite.TipoParto.valueOf(tipo_parto)); } catch (Exception ignored) {}
        }
        if (idade_gestacional != null && !idade_gestacional.isBlank()) {
            try { dl.setIdadeGestacional(DoadoraLeite.IdadeGestacional.valueOf(idade_gestacional)); } catch (Exception ignored) {}
        }
        dl.setNumFilhosAmamentando(num_filhos_amamentando);
        if (amamentacao_exclusiva != null && !amamentacao_exclusiva.isBlank()) {
            try { dl.setAmamentacaoExclusiva(DoadoraLeite.AmamentacaoExclusiva.valueOf(amamentacao_exclusiva)); } catch (Exception ignored) {}
        }
        dl.setProducaoEstimada(producao_estimada);
        if (historico_doacao != null && !historico_doacao.isBlank()) {
            try { dl.setHistoricoDoacao(DoadoraLeite.HistoricoDoacaoLeite.valueOf(historico_doacao)); } catch (Exception ignored) {}
        }
        if (modalidade_coleta != null && !modalidade_coleta.isBlank()) {
            try { dl.setModalidadeColeta(DoadoraLeite.ModalidadeColeta.valueOf(modalidade_coleta)); } catch (Exception ignored) {}
        }
        dl.setObservacoes(observacoes);
        dl.setInteresseDoador("nao".equals(interesse_doador) ? Boolean.FALSE : Boolean.TRUE);
        dl.setCondicaoSaude("1".equals(condicao_saude) || "sim".equals(condicao_saude));
        dl.setDescricaoCondicao(descricao_condicao);
        dl.setUnidade(unidade);
        dl.setDataAgendamento(data_agendamento);
        if (turno != null && !turno.isBlank()) {
            try { dl.setTurno(DoadoraLeite.TurnoLeite.valueOf(turno)); } catch (Exception ignored) {}
        }
        dl.setComoSoube(como_soube);
        dl.setConsentimento("1".equals(consentimento));
        doadoraLeiteRepository.save(dl);

        redirectAttributes.addFlashAttribute("sucesso",
            "Cadastro de doadora de leite enviado com sucesso! Seu agendamento ficara pendente ate a confirmacao da instituicao.");
        return "redirect:/doador/home";
    }

    // ─── CONFIRMAR / CANCELAR AGENDAMENTO (lado do doador) ─────────────────────
    // O proprio doador pode confirmar presenca ou cancelar seu agendamento
    // diretamente em /doador/home, para qualquer um dos 3 tipos de doacao.

    @PostMapping("/doador/agendamento/{tipo}/{id}/confirmar")
    public String confirmarAgendamento(@PathVariable String tipo,
                                        @PathVariable Long id,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        if (session.getAttribute("doador_id") == null) return "redirect:/doador/login";
        Long doadorId = (Long) session.getAttribute("doador_id");

        switch (tipo) {
            case "sangue" -> doadorSangueRepository.findById(id).ifPresent(ds -> {
                if (pertenceAoDoador(ds.getDoador(), doadorId) && ds.getStatus() == DoadorSangue.StatusTriagem.pendente) {
                    ds.setStatus(DoadorSangue.StatusTriagem.confirmado);
                    ds.setConfirmadoEm(java.time.LocalDateTime.now());
                    doadorSangueRepository.save(ds);
                }
            });
            case "medula" -> doadorMedulaRepository.findById(id).ifPresent(dm -> {
                if (pertenceAoDoador(dm.getDoador(), doadorId) && dm.getStatus() == DoadorMedula.StatusMedula.pendente) {
                    dm.setStatus(DoadorMedula.StatusMedula.confirmado);
                    dm.setConfirmadoEm(java.time.LocalDateTime.now());
                    doadorMedulaRepository.save(dm);
                }
            });
            case "leite" -> doadoraLeiteRepository.findById(id).ifPresent(dl -> {
                if (pertenceAoDoador(dl.getDoador(), doadorId) && dl.getStatus() == DoadoraLeite.StatusDoadoraLeite.pendente) {
                    dl.setStatus(DoadoraLeite.StatusDoadoraLeite.confirmado);
                    dl.setConfirmadoEm(java.time.LocalDateTime.now());
                    doadoraLeiteRepository.save(dl);
                }
            });
            default -> { }
        }

        redirectAttributes.addFlashAttribute("sucesso", "Presenca confirmada com sucesso! Te esperamos na data agendada.");
        return "redirect:/doador/home";
    }

    @PostMapping("/doador/agendamento/{tipo}/{id}/cancelar")
    public String cancelarAgendamentoDoador(@PathVariable String tipo,
                                             @PathVariable Long id,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        if (session.getAttribute("doador_id") == null) return "redirect:/doador/login";
        Long doadorId = (Long) session.getAttribute("doador_id");

        switch (tipo) {
            case "sangue" -> doadorSangueRepository.findById(id).ifPresent(ds -> {
                if (pertenceAoDoador(ds.getDoador(), doadorId) && podeCancelar(ds.getStatus().name())) {
                    ds.setStatus(DoadorSangue.StatusTriagem.cancelado);
                    ds.setCanceladoEm(java.time.LocalDateTime.now());
                    ds.setCanceladoPor("doador");
                    doadorSangueRepository.save(ds);
                }
            });
            case "medula" -> doadorMedulaRepository.findById(id).ifPresent(dm -> {
                if (pertenceAoDoador(dm.getDoador(), doadorId) && podeCancelar(dm.getStatus().name())) {
                    dm.setStatus(DoadorMedula.StatusMedula.cancelado);
                    dm.setCanceladoEm(java.time.LocalDateTime.now());
                    dm.setCanceladoPor("doador");
                    doadorMedulaRepository.save(dm);
                }
            });
            case "leite" -> doadoraLeiteRepository.findById(id).ifPresent(dl -> {
                if (pertenceAoDoador(dl.getDoador(), doadorId) && podeCancelar(dl.getStatus().name())) {
                    dl.setStatus(DoadoraLeite.StatusDoadoraLeite.cancelado);
                    dl.setCanceladoEm(java.time.LocalDateTime.now());
                    dl.setCanceladoPor("doador");
                    doadoraLeiteRepository.save(dl);
                }
            });
            default -> { }
        }

        redirectAttributes.addFlashAttribute("sucesso", "Agendamento cancelado com sucesso.");
        return "redirect:/doador/home";
    }

    private boolean pertenceAoDoador(Doador doadorDoRegistro, Long doadorIdDaSessao) {
        return doadorDoRegistro != null && doadorDoRegistro.getId() != null
                && doadorDoRegistro.getId().equals(doadorIdDaSessao);
    }

    private boolean podeCancelar(String statusAtual) {
        return "pendente".equals(statusAtual) || "confirmado".equals(statusAtual);
    }

    // ─── LOGOUT ───────────────────────────────────────────────────────────────

    @GetMapping("/doador/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private void preencherSessao(HttpSession session, Doador d) {
        session.setAttribute("doador_id",   d.getId());
        session.setAttribute("doador_nome", d.getNome());
        session.setAttribute("doador_nome_exibicao", abreviarNome(d.getNome()));
        session.setAttribute("tipo_doacao", d.getTipoDoador());
    }

    /**
     * Resume o nome para exibição em saudações: primeiro nome + inicial do segundo nome.
     * Ex.: "Paulo Vinicius Souza" -> "Paulo V."
     */
    private String abreviarNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) return nomeCompleto;
        String[] partes = nomeCompleto.trim().split("\\s+");
        if (partes.length == 1) return partes[0];
        return partes[0] + " " + Character.toUpperCase(partes[1].charAt(0)) + ".";
    }
}
