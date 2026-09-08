package com.fluxo.fluxovital.controller;

import com.fluxo.fluxovital.model.CategoriaInstituicao;
import com.fluxo.fluxovital.model.DoadorMedula;
import com.fluxo.fluxovital.model.DoadorSangue;
import com.fluxo.fluxovital.model.DoadoraLeite;
import com.fluxo.fluxovital.model.Instituicao;
import com.fluxo.fluxovital.repository.DoadorMedulaRepository;
import com.fluxo.fluxovital.repository.DoadorRepository;
import com.fluxo.fluxovital.repository.DoadoraLeiteRepository;
import com.fluxo.fluxovital.repository.DoadorSangueRepository;
import com.fluxo.fluxovital.repository.InstituicaoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.ArrayList;

@Controller
public class InstituicaoController {

    private final InstituicaoRepository instituicaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoadorSangueRepository doadorSangueRepository;
    private final DoadoraLeiteRepository doadoraLeiteRepository;
    private final DoadorMedulaRepository doadorMedulaRepository;


    public InstituicaoController(InstituicaoRepository instituicaoRepository,
                                 PasswordEncoder passwordEncoder,
                                 DoadorSangueRepository doadorSangueRepository,
                                 DoadoraLeiteRepository doadoraLeiteRepository,
                                 DoadorMedulaRepository doadorMedulaRepository) {
        this.instituicaoRepository = instituicaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.doadorSangueRepository = doadorSangueRepository;
        this.doadoraLeiteRepository = doadoraLeiteRepository;
        this.doadorMedulaRepository = doadorMedulaRepository;
    }

    // ─── ESCOLHA DE CATEGORIA (para quem vai logar/cadastrar como instituição) ─

    @GetMapping("/instituicao/categoria")
    public String escolhaCategoria(Model model) {
        model.addAttribute("categorias", CategoriaInstituicao.values());
        return "instituicao/escolha_categoria";
    }

    // ─── BUSCA DE INSTITUIÇÕES (para o doador encontrar onde doar) ─────────────

    @GetMapping("/instituicao/buscar")
    public String buscarInstituicoes(@RequestParam(required = false) String tipo,
                                      @RequestParam(required = false) String cidade,
                                      Model model) {
        CategoriaInstituicao categoria = null;
        if (tipo != null && !tipo.isBlank()) {
            try {
                categoria = CategoriaInstituicao.valueOf(tipo);
            } catch (IllegalArgumentException ignored) { }
        }

        boolean temCidade = cidade != null && !cidade.isBlank();
        java.util.List<Instituicao> resultados;

        if (categoria != null && temCidade) {
            resultados = instituicaoRepository
                    .findByCategoriaAndCidadeContainingIgnoreCaseOrderByNomeAsc(categoria, cidade.trim());
        } else if (categoria != null) {
            resultados = instituicaoRepository.findByCategoriaOrderByNomeAsc(categoria);
        } else if (temCidade) {
            resultados = instituicaoRepository.findByCidadeContainingIgnoreCaseOrderByNomeAsc(cidade.trim());
        } else {
            resultados = instituicaoRepository.findAllByOrderByNomeAsc();
        }

        model.addAttribute("resultados", resultados);
        model.addAttribute("categorias", CategoriaInstituicao.values());
        model.addAttribute("tipoSelecionado", tipo);
        model.addAttribute("cidadeBuscada", cidade);
        return "instituicao/buscar";
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @GetMapping("/instituicao/login")
    public String exibirLogin(@RequestParam(required = false) String categoria, Model model) {
        if (categoria == null || categoria.isBlank()) {
            return "redirect:/instituicao/categoria";
        }
        // resolve enum para pegar a descrição legível (ex: BANCOS_DE_DOACAO → "Bancos de Doação")
        try {
            CategoriaInstituicao cat = CategoriaInstituicao.fromDescricao(categoria);
            model.addAttribute("categoria", cat.name());           // nome do enum para o hidden field
            model.addAttribute("categoriaLabel", cat.getDescricao()); // label para exibição
        } catch (IllegalArgumentException e) {
            model.addAttribute("categoria", categoria);
            model.addAttribute("categoriaLabel", categoria);
        }
        return "instituicao/login";
    }

    @PostMapping("/instituicao/login")
    public String processarLogin(
            @RequestParam String email,
            @RequestParam String senha,
            @RequestParam String categoria,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        CategoriaInstituicao cat;
        try {
            cat = CategoriaInstituicao.fromDescricao(categoria);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", "Categoria inválida.");
            return "redirect:/instituicao/categoria";
        }

        if (email.isBlank() || senha.isBlank()) {
            redirectAttributes.addFlashAttribute("erro", "Preencha email e senha.");
            return "redirect:/instituicao/login?categoria=" + categoria;
        }

        Optional<Instituicao> opt = instituicaoRepository.findByEmailAndCategoria(email.trim(), cat);

        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado na categoria selecionada.");
            return "redirect:/instituicao/login?categoria=" + categoria;
        }

        Instituicao inst = opt.get();

        if (!passwordEncoder.matches(senha, inst.getSenha())) {
            redirectAttributes.addFlashAttribute("erro", "Senha incorreta.");
            return "redirect:/instituicao/login?categoria=" + categoria;
        }

        preencherSessao(session, inst);
        return "redirect:/instituicao/home";
    }

    // ─── CADASTRO ─────────────────────────────────────────────────────────────

    @GetMapping("/instituicao/cadastro")
    public String exibirCadastro(@RequestParam(required = false) String categoria, Model model) {
        if (categoria == null || categoria.isBlank()) {
            return "redirect:/instituicao/categoria";
        }
        try {
            CategoriaInstituicao cat = CategoriaInstituicao.fromDescricao(categoria);
            model.addAttribute("categoria", cat.name());
            model.addAttribute("categoriaLabel", cat.getDescricao());
        } catch (IllegalArgumentException e) {
            model.addAttribute("categoria", categoria);
            model.addAttribute("categoriaLabel", categoria);
        }
        return "instituicao/cadastro";
    }

    @PostMapping("/instituicao/cadastro")
    public String processarCadastro(
            @RequestParam String nome,
            @RequestParam String cnpj,
            @RequestParam String email,
            @RequestParam String senha,
            @RequestParam("confirma_senha") String confirmaSenha,
            @RequestParam(required = false, defaultValue = "") String telefone,
            @RequestParam(required = false, defaultValue = "") String endereco,
            @RequestParam String cidade,
            @RequestParam String estado,
            @RequestParam(required = false, defaultValue = "") String cep,
            @RequestParam String categoria,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        // Validações básicas
        if (nome.isBlank() || cnpj.isBlank() || email.isBlank() || senha.isBlank()
                || cidade.isBlank() || estado.isBlank()) {
            redirectAttributes.addFlashAttribute("erro", "Preencha todos os campos obrigatórios.");
            return "redirect:/instituicao/cadastro?categoria=" + categoria;
        }

        if (!senha.equals(confirmaSenha)) {
            redirectAttributes.addFlashAttribute("erro", "As senhas não coincidem.");
            return "redirect:/instituicao/cadastro?categoria=" + categoria;
        }

        String cnpjNumerico = cnpj.replaceAll("\\D", "");
        if (cnpjNumerico.length() != 14) {
            redirectAttributes.addFlashAttribute("erro", "CNPJ inválido. Informe 14 dígitos.");
            return "redirect:/instituicao/cadastro?categoria=" + categoria;
        }

        if (instituicaoRepository.existsByCnpj(cnpjNumerico)) {
            redirectAttributes.addFlashAttribute("erro", "CNPJ já cadastrado.");
            return "redirect:/instituicao/cadastro?categoria=" + categoria;
        }

        if (instituicaoRepository.findByEmail(email.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("erro", "E-mail já cadastrado.");
            return "redirect:/instituicao/cadastro?categoria=" + categoria;
        }

        // Resolve categoria — aceita descrição ou nome do enum
        CategoriaInstituicao cat;
        try {
            cat = CategoriaInstituicao.fromDescricao(categoria);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", "Categoria inválida. Selecione novamente.");
            return "redirect:/instituicao/categoria";
        }

        // Salva a instituição
        Instituicao nova = new Instituicao();
        nova.setNome(nome.trim());
        nova.setCnpj(cnpjNumerico);
        nova.setEmail(email.trim());
        nova.setSenha(passwordEncoder.encode(senha));
        nova.setTelefone(telefone.trim());
        nova.setCategoria(cat);
        nova.setEndereco(endereco.trim());
        nova.setCidade(cidade.trim());
        nova.setEstado(estado.trim().toUpperCase());
        nova.setCep(cep.trim());

        instituicaoRepository.save(nova);

        // Já loga na sessão — não precisa fazer login de novo
        preencherSessao(session, nova);

        redirectAttributes.addFlashAttribute("sucesso", "Cadastro realizado com sucesso! Bem-vindo(a).");
        return "redirect:/instituicao/home";
    }

    // ─── HOME ─────────────────────────────────────────────────────────────────

    @GetMapping("/instituicao/home")
    public String home(HttpSession session, Model model) {
        if (session.getAttribute("instituicao_id") == null) {
            return "redirect:/instituicao/categoria";
        }
        model.addAttribute("nome", session.getAttribute("instituicao_nome"));
        model.addAttribute("categoria", session.getAttribute("instituicao_categoria"));
        return "instituicao/home";
    }

    // ─── GERENCIAR ────────────────────────────────────────────────────────────

    @GetMapping("/instituicao/gerenciar")
    public String gerenciar(HttpSession session, Model model) {
        if (session.getAttribute("instituicao_id") == null) {
            return "redirect:/instituicao/categoria";
        }
        Long id = (Long) session.getAttribute("instituicao_id");
        instituicaoRepository.findById(id).ifPresent(inst -> model.addAttribute("instituicao", inst));
        return "instituicao/gerenciar";
    }

    // ─── EDITAR ───────────────────────────────────────────────────────────────

    @GetMapping("/instituicao/editar")
    public String exibirEditar(HttpSession session, Model model) {
        if (session.getAttribute("instituicao_id") == null) {
            return "redirect:/instituicao/categoria";
        }
        Long id = (Long) session.getAttribute("instituicao_id");
        instituicaoRepository.findById(id).ifPresent(inst -> model.addAttribute("instituicao", inst));
        return "instituicao/editar";
    }

    @PostMapping("/instituicao/editar")
    public String processarEdicao(
            @RequestParam String nome,
            @RequestParam String cnpj,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String telefone,
            @RequestParam(required = false, defaultValue = "") String endereco,
            @RequestParam(required = false, defaultValue = "") String cep,
            @RequestParam(required = false) String senha,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("instituicao_id") == null) {
            return "redirect:/instituicao/categoria";
        }

        if (nome.isBlank() || cnpj.isBlank() || email.isBlank()) {
            redirectAttributes.addFlashAttribute("erro", "Preencha todos os campos obrigatórios.");
            return "redirect:/instituicao/editar";
        }

        String cnpjNumerico = cnpj.replaceAll("\\D", "");
        if (cnpjNumerico.length() != 14) {
            redirectAttributes.addFlashAttribute("erro", "CNPJ inválido.");
            return "redirect:/instituicao/editar";
        }

        Long id = (Long) session.getAttribute("instituicao_id");
        Optional<Instituicao> opt = instituicaoRepository.findById(id);

        if (opt.isEmpty()) {
            return "redirect:/instituicao/categoria";
        }

        Instituicao inst = opt.get();
        inst.setNome(nome.trim());
        inst.setCnpj(cnpjNumerico);
        inst.setEmail(email.trim());
        inst.setTelefone(telefone.trim());
        inst.setEndereco(endereco.trim());
        inst.setCep(cep.trim());

        if (senha != null && !senha.isBlank()) {
            inst.setSenha(passwordEncoder.encode(senha));
        }

        instituicaoRepository.save(inst);

        session.setAttribute("instituicao_nome", inst.getNome());
        redirectAttributes.addFlashAttribute("sucesso", "Dados atualizados com sucesso!");
        return "redirect:/instituicao/editar";
    }

    // ─── LOGOUT ───────────────────────────────────────────────────────────────

    @GetMapping("/instituicao/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private void preencherSessao(HttpSession session, Instituicao inst) {
        session.setAttribute("instituicao_id", inst.getId());
        session.setAttribute("instituicao_nome", inst.getNome());
        session.setAttribute("instituicao_email", inst.getEmail());
        session.setAttribute("instituicao_categoria", inst.getCategoria().getDescricao());
    }

    @GetMapping("/instituicao/doacoes")
    public String listarDoacoes(HttpSession session, Model model) {
        if (session.getAttribute("instituicao_id") == null)
            return "redirect:/instituicao/categoria";
        model.addAttribute("doacoesSangue", doadorSangueRepository.findAllByOrderByCriadoEmDesc());
        model.addAttribute("doacoesLeite",  doadoraLeiteRepository.findAllByOrderByCriadoEmDesc());
        model.addAttribute("doacoesMedula", doadorMedulaRepository.findAllByOrderByCriadoEmDesc());
        return "instituicao/doacoes";
    }

    @GetMapping("/instituicao/doacoes/medula/{id}")
    public String verDetalhesMedula(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("instituicao_id") == null)
            return "redirect:/instituicao/categoria";
        model.addAttribute("doador", doadorMedulaRepository.findById(id).orElse(null));
        return "instituicao/detalhes_medula";
    }

    @PostMapping("/instituicao/doacoes/medula/{id}/status")
    public String atualizarStatusMedula(@PathVariable Long id,
                                         @RequestParam String status,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        if (session.getAttribute("instituicao_id") == null)
            return "redirect:/instituicao/categoria";
        doadorMedulaRepository.findById(id).ifPresent(d -> {
            try {
                DoadorMedula.StatusMedula novo = DoadorMedula.StatusMedula.valueOf(status);
                d.setStatus(novo);
                if (novo == DoadorMedula.StatusMedula.confirmado) {
                    d.setConfirmadoEm(java.time.LocalDateTime.now());
                } else if (novo == DoadorMedula.StatusMedula.cancelado) {
                    d.setCanceladoEm(java.time.LocalDateTime.now());
                    d.setCanceladoPor("instituicao");
                }
            } catch (Exception ignored) {}
            doadorMedulaRepository.save(d);
        });
        redirectAttributes.addFlashAttribute("sucesso", "Status atualizado com sucesso.");
        return "redirect:/instituicao/doacoes/medula/" + id;
    }

    @GetMapping("/instituicao/doacoes/{id}")
    public String verDetalhesSangue(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("instituicao_id") == null)
            return "redirect:/instituicao/categoria";
        model.addAttribute("doador", doadorSangueRepository.findById(id).orElse(null));
        return "instituicao/detalhes_doacao";
    }

    @PostMapping("/instituicao/doacoes/{id}/status")
    public String atualizarStatusSangue(@PathVariable Long id,
                                         @RequestParam String status,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        if (session.getAttribute("instituicao_id") == null)
            return "redirect:/instituicao/categoria";
        doadorSangueRepository.findById(id).ifPresent(d -> {
            try {
                DoadorSangue.StatusTriagem novo = DoadorSangue.StatusTriagem.valueOf(status);
                d.setStatus(novo);
                if (novo == DoadorSangue.StatusTriagem.confirmado) {
                    d.setConfirmadoEm(java.time.LocalDateTime.now());
                } else if (novo == DoadorSangue.StatusTriagem.cancelado) {
                    d.setCanceladoEm(java.time.LocalDateTime.now());
                    d.setCanceladoPor("instituicao");
                }
            } catch (Exception ignored) {}
            doadorSangueRepository.save(d);
        });
        redirectAttributes.addFlashAttribute("sucesso", "Status atualizado com sucesso.");
        return "redirect:/instituicao/doacoes/" + id;
    }

    @GetMapping("/instituicao/doacoes/leite/{id}")
    public String verDetalhesLeite(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("instituicao_id") == null)
            return "redirect:/instituicao/categoria";
        model.addAttribute("doadora", doadoraLeiteRepository.findById(id).orElse(null));
        return "instituicao/detalhes_leite";
    }

    @PostMapping("/instituicao/doacoes/leite/{id}/status")
    public String atualizarStatusLeite(@PathVariable Long id,
                                        @RequestParam String status,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        if (session.getAttribute("instituicao_id") == null)
            return "redirect:/instituicao/categoria";
        doadoraLeiteRepository.findById(id).ifPresent(d -> {
            try {
                DoadoraLeite.StatusDoadoraLeite novo = DoadoraLeite.StatusDoadoraLeite.valueOf(status);
                d.setStatus(novo);
                if (novo == DoadoraLeite.StatusDoadoraLeite.confirmado) {
                    d.setConfirmadoEm(java.time.LocalDateTime.now());
                } else if (novo == DoadoraLeite.StatusDoadoraLeite.cancelado) {
                    d.setCanceladoEm(java.time.LocalDateTime.now());
                    d.setCanceladoPor("instituicao");
                }
            } catch (Exception ignored) {}
            doadoraLeiteRepository.save(d);
        });
        redirectAttributes.addFlashAttribute("sucesso", "Status atualizado com sucesso.");
        return "redirect:/instituicao/doacoes/leite/" + id;
    }
}