package com.fluxo.fluxovital.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doadores_medula")
public class DoadorMedula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doador_id")
    private Doador doador;

    // Identificação
    private String nome;
    private String cpf;
    private String rg;
    private LocalDate nascimento;
    private String sexo;
    private Double peso;
    private Double altura;
    private String etnia;
    private String email;
    private String telefone;

    // Histórico e informações adicionais
    private String historicoMedula;
    private String cadastroRedome;
    private String modalidade;

    // Agendamento
    private String unidade;
    private LocalDate dataColeta;
    private String turno;
    private String comoSoube;

    @Column(length = 1000)
    private String observacoes;

    private Boolean consentimento = false;

    // Interesse e questionário simplificado (sem perguntas de doenças específicas)
    @Column(name = "interesse_doador")
    private Boolean interesseDoador = true;

    @Column(name = "condicao_saude")
    private Boolean condicaoSaude = false;

    @Column(name = "descricao_condicao", length = 500)
    private String descricaoCondicao;

    @Enumerated(EnumType.STRING)
    private StatusMedula status = StatusMedula.pendente;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "cancelado_por")
    private String canceladoPor;

    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    public enum StatusMedula { pendente, confirmado, cancelado, realizado }

    // --- GETTERS E SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Doador getDoador() { return doador; }
    public void setDoador(Doador doador) { this.doador = doador; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }

    public LocalDate getNascimento() { return nascimento; }
    public void setNascimento(LocalDate nascimento) { this.nascimento = nascimento; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }

    public Double getAltura() { return altura; }
    public void setAltura(Double altura) { this.altura = altura; }

    public String getEtnia() { return etnia; }
    public void setEtnia(String etnia) { this.etnia = etnia; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getHistoricoMedula() { return historicoMedula; }
    public void setHistoricoMedula(String v) { this.historicoMedula = v; }

    public String getCadastroRedome() { return cadastroRedome; }
    public void setCadastroRedome(String v) { this.cadastroRedome = v; }

    public String getModalidade() { return modalidade; }
    public void setModalidade(String v) { this.modalidade = v; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public LocalDate getDataColeta() { return dataColeta; }
    public void setDataColeta(LocalDate v) { this.dataColeta = v; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getComoSoube() { return comoSoube; }
    public void setComoSoube(String v) { this.comoSoube = v; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public Boolean getConsentimento() { return consentimento; }
    public void setConsentimento(Boolean v) { this.consentimento = v; }

    public Boolean getInteresseDoador() { return interesseDoador; }
    public void setInteresseDoador(Boolean v) { this.interesseDoador = v; }
    public Boolean getCondicaoSaude() { return condicaoSaude; }
    public void setCondicaoSaude(Boolean v) { this.condicaoSaude = v != null && v; }
    public String getDescricaoCondicao() { return descricaoCondicao; }
    public void setDescricaoCondicao(String v) { this.descricaoCondicao = v; }

    public StatusMedula getStatus() { return status; }
    public void setStatus(StatusMedula status) { this.status = status; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public void setConfirmadoEm(LocalDateTime v) { this.confirmadoEm = v; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(LocalDateTime v) { this.canceladoEm = v; }
    public String getCanceladoPor() { return canceladoPor; }
    public void setCanceladoPor(String v) { this.canceladoPor = v; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
