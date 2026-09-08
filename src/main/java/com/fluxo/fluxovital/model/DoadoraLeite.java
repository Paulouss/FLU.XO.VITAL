package com.fluxo.fluxovital.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doadora_leite")
public class DoadoraLeite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doador_id")
    private Doador doador;

    private String nome;
    private String cpf;
    private String rg;
    private LocalDate nascimento;
    private String email;
    private String telefone;
    private String endereco;

    @Column(name = "data_parto")
    private LocalDate dataParto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_parto")
    private TipoParto tipoParto;

    @Enumerated(EnumType.STRING)
    @Column(name = "idade_gestacional")
    private IdadeGestacional idadeGestacional;

    @Column(name = "num_filhos_amamentando")
    private String numFilhosAmamentando;

    @Enumerated(EnumType.STRING)
    @Column(name = "amamentacao_exclusiva")
    private AmamentacaoExclusiva amamentacaoExclusiva;

    @Column(name = "producao_estimada")
    private String producaoEstimada;

    @Enumerated(EnumType.STRING)
    @Column(name = "historico_doacao")
    private HistoricoDoacaoLeite historicoDoacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_coleta")
    private ModalidadeColeta modalidadeColeta;

    private String observacoes;

    // String em vez de Enum para evitar problemas de mapeamento
    private String unidade;

    @Column(name = "data_agendamento")
    private LocalDate dataAgendamento;

    @Enumerated(EnumType.STRING)
    private TurnoLeite turno;

    @Column(name = "como_soube")
    private String comoSoube;

    private Boolean consentimento = false;

    // Interesse e questionário simplificado (sem perguntas de doenças específicas)
    @Column(name = "interesse_doador")
    private Boolean interesseDoador = true;

    @Column(name = "condicao_saude")
    private Boolean condicaoSaude = false;

    @Column(name = "descricao_condicao", length = 500)
    private String descricaoCondicao;

    @Enumerated(EnumType.STRING)
    private StatusDoadoraLeite status = StatusDoadoraLeite.pendente;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "cancelado_por")
    private String canceladoPor;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        if (this.status == null) this.status = StatusDoadoraLeite.pendente;
    }

    public DoadoraLeite() {}

    public enum TipoParto            { normal, cesariana }
    public enum IdadeGestacional     { prematuro_extremo, prematuro_moderado, prematuro_tardio, termo }
    public enum AmamentacaoExclusiva { sim, parcial, nao }
    public enum HistoricoDoacaoLeite { nunca, sim, regular }
    public enum ModalidadeColeta     { unidade, domicilio, ambos }
    public enum TurnoLeite           { manha, tarde }
    public enum StatusDoadoraLeite   { pendente, confirmado, cancelado, realizado }

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
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String v) { this.telefone = v; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String v) { this.endereco = v; }
    public LocalDate getDataParto() { return dataParto; }
    public void setDataParto(LocalDate v) { this.dataParto = v; }
    public TipoParto getTipoParto() { return tipoParto; }
    public void setTipoParto(TipoParto v) { this.tipoParto = v; }
    public IdadeGestacional getIdadeGestacional() { return idadeGestacional; }
    public void setIdadeGestacional(IdadeGestacional v) { this.idadeGestacional = v; }
    public String getNumFilhosAmamentando() { return numFilhosAmamentando; }
    public void setNumFilhosAmamentando(String v) { this.numFilhosAmamentando = v; }
    public AmamentacaoExclusiva getAmamentacaoExclusiva() { return amamentacaoExclusiva; }
    public void setAmamentacaoExclusiva(AmamentacaoExclusiva v) { this.amamentacaoExclusiva = v; }
    public String getProducaoEstimada() { return producaoEstimada; }
    public void setProducaoEstimada(String v) { this.producaoEstimada = v; }
    public HistoricoDoacaoLeite getHistoricoDoacao() { return historicoDoacao; }
    public void setHistoricoDoacao(HistoricoDoacaoLeite v) { this.historicoDoacao = v; }
    public ModalidadeColeta getModalidadeColeta() { return modalidadeColeta; }
    public void setModalidadeColeta(ModalidadeColeta v) { this.modalidadeColeta = v; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String v) { this.observacoes = v; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String v) { this.unidade = v; }
    public LocalDate getDataAgendamento() { return dataAgendamento; }
    public void setDataAgendamento(LocalDate v) { this.dataAgendamento = v; }
    public TurnoLeite getTurno() { return turno; }
    public void setTurno(TurnoLeite v) { this.turno = v; }
    public String getComoSoube() { return comoSoube; }
    public void setComoSoube(String v) { this.comoSoube = v; }
    public Boolean getConsentimento() { return consentimento; }
    public void setConsentimento(Boolean v) { this.consentimento = v != null && v; }
    public Boolean getInteresseDoador() { return interesseDoador; }
    public void setInteresseDoador(Boolean v) { this.interesseDoador = v; }
    public Boolean getCondicaoSaude() { return condicaoSaude; }
    public void setCondicaoSaude(Boolean v) { this.condicaoSaude = v != null && v; }
    public String getDescricaoCondicao() { return descricaoCondicao; }
    public void setDescricaoCondicao(String v) { this.descricaoCondicao = v; }
    public StatusDoadoraLeite getStatus() { return status; }
    public void setStatus(StatusDoadoraLeite v) { this.status = v; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public void setConfirmadoEm(LocalDateTime v) { this.confirmadoEm = v; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(LocalDateTime v) { this.canceladoEm = v; }
    public String getCanceladoPor() { return canceladoPor; }
    public void setCanceladoPor(String v) { this.canceladoPor = v; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime v) { this.criadoEm = v; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime v) { this.atualizadoEm = v; }
}
