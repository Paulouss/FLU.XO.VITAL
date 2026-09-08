package com.fluxo.fluxovital.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doadores_sangue")
public class DoadorSangue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vínculo com o doador logado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doador_id")
    private Doador doador;

    // Identificação
    private String nome;
    private String cpf;
    private String rg;
    private LocalDate nascimento;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    private Double peso;
    private Double altura;
    private String email;
    private String telefone;
    private String endereco;

    @Column(name = "tipo_sanguineo")
    private String tipoSanguineo;

    @Column(name = "ultima_doacao")
    private LocalDate ultimaDoacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "historico_doacao")
    private HistoricoDoacao historicoDoacao;

    @Column(length = 600)
    private String observacoes;

    // Interesse e questionário simplificado (sem perguntas de doenças específicas)
    @Column(name = "interesse_doador")
    private Boolean interesseDoador = true;

    @Column(name = "condicao_saude")
    private Boolean condicaoSaude = false;

    @Column(name = "descricao_condicao", length = 500)
    private String descricaoCondicao;

    // Agendamento
    private String unidade;

    @Column(name = "data_agendamento")
    private LocalDate dataAgendamento;

    @Enumerated(EnumType.STRING)
    private Turno turno;

    @Column(name = "como_soube")
    private String comoSoube;

    private Boolean consentimento = false;

    // Controle interno — status do agendamento (não é mais uma triagem médica)
    @Enumerated(EnumType.STRING)
    private StatusTriagem status = StatusTriagem.pendente;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "cancelado_por")
    private String canceladoPor; // "doador" ou "instituicao"

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        if (this.status == null) this.status = StatusTriagem.pendente;
    }

    public DoadorSangue() {}

    // ─── Enums internos ───────────────────────────────────────────────────────
    public enum Sexo            { masculino, feminino }
    public enum HistoricoDoacao { nunca, sim, regular }
    public enum Turno           { manha, tarde }
    public enum StatusTriagem   { pendente, confirmado, cancelado, realizado }

    // ─── Getters / Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Doador getDoador() { return doador; }
    public void setDoador(Doador d) { this.doador = d; }
    public String getNome() { return nome; }
    public void setNome(String v) { this.nome = v; }
    public String getCpf() { return cpf; }
    public void setCpf(String v) { this.cpf = v; }
    public String getRg() { return rg; }
    public void setRg(String v) { this.rg = v; }
    public LocalDate getNascimento() { return nascimento; }
    public void setNascimento(LocalDate v) { this.nascimento = v; }
    public Sexo getSexo() { return sexo; }
    public void setSexo(Sexo v) { this.sexo = v; }
    public Double getPeso() { return peso; }
    public void setPeso(Double v) { this.peso = v; }
    public Double getAltura() { return altura; }
    public void setAltura(Double v) { this.altura = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String v) { this.telefone = v; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String v) { this.endereco = v; }
    public String getTipoSanguineo() { return tipoSanguineo; }
    public void setTipoSanguineo(String v) { this.tipoSanguineo = v; }
    public LocalDate getUltimaDoacao() { return ultimaDoacao; }
    public void setUltimaDoacao(LocalDate v) { this.ultimaDoacao = v; }
    public HistoricoDoacao getHistoricoDoacao() { return historicoDoacao; }
    public void setHistoricoDoacao(HistoricoDoacao v) { this.historicoDoacao = v; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String v) { this.observacoes = v; }
    public Boolean getInteresseDoador() { return interesseDoador; }
    public void setInteresseDoador(Boolean v) { this.interesseDoador = v; }
    public Boolean getCondicaoSaude() { return condicaoSaude; }
    public void setCondicaoSaude(Boolean v) { this.condicaoSaude = v != null && v; }
    public String getDescricaoCondicao() { return descricaoCondicao; }
    public void setDescricaoCondicao(String v) { this.descricaoCondicao = v; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String v) { this.unidade = v; }
    public LocalDate getDataAgendamento() { return dataAgendamento; }
    public void setDataAgendamento(LocalDate v) { this.dataAgendamento = v; }
    public Turno getTurno() { return turno; }
    public void setTurno(Turno v) { this.turno = v; }
    public String getComoSoube() { return comoSoube; }
    public void setComoSoube(String v) { this.comoSoube = v; }
    public Boolean getConsentimento() { return consentimento; }
    public void setConsentimento(Boolean v) { this.consentimento = v != null && v; }
    public StatusTriagem getStatus() { return status; }
    public void setStatus(StatusTriagem v) { this.status = v; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public void setConfirmadoEm(LocalDateTime v) { this.confirmadoEm = v; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(LocalDateTime v) { this.canceladoEm = v; }
    public String getCanceladoPor() { return canceladoPor; }
    public void setCanceladoPor(String v) { this.canceladoPor = v; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime v) { this.criadoEm = v; }
}
