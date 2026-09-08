package com.fluxo.fluxovital.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doador")
public class Doador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true)
    private String email;

    private String telefone;
    private String senha;

    private String cpf;
    private String rg;

    private LocalDate nascimento;
    private String sexo;
    private Double peso;

    private String tipoSanguineo;
    private String historicoDoacao;

    @Column(length = 500)
    private String observacoes;

    // Condições de saúde
    private Boolean vacina;
    private Boolean tatuagem;
    private Boolean cronica;
    private Boolean medicamento;
    private Boolean infeccao;
    private Boolean cirurgia;

    // Agendamento
    private String unidade;
    private LocalDate dataAgendamento;
    private String turno;

    // Campo que faltava no Controller
    private String tipoDoador;

    @Enumerated(EnumType.STRING)
    private StatusDoador status = StatusDoador.ativo;

    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    public enum StatusDoador { ativo, inativo }

    // --- GETTERS E SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

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

    public String getTipoSanguineo() { return tipoSanguineo; }
    public void setTipoSanguineo(String tipoSanguineo) { this.tipoSanguineo = tipoSanguineo; }

    public String getHistoricoDoacao() { return historicoDoacao; }
    public void setHistoricoDoacao(String historicoDoacao) { this.historicoDoacao = historicoDoacao; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public Boolean getVacina() { return vacina; }
    public void setVacina(Boolean vacina) { this.vacina = vacina; }

    public Boolean getTatuagem() { return tatuagem; }
    public void setTatuagem(Boolean tatuagem) { this.tatuagem = tatuagem; }

    public Boolean getCronica() { return cronica; }
    public void setCronica(Boolean cronica) { this.cronica = cronica; }

    public Boolean getMedicamento() { return medicamento; }
    public void setMedicamento(Boolean medicamento) { this.medicamento = medicamento; }

    public Boolean getInfeccao() { return infeccao; }
    public void setInfeccao(Boolean infeccao) { this.infeccao = infeccao; }

    public Boolean getCirurgia() { return cirurgia; }
    public void setCirurgia(Boolean cirurgia) { this.cirurgia = cirurgia; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public LocalDate getDataAgendamento() { return dataAgendamento; }
    public void setDataAgendamento(LocalDate dataAgendamento) { this.dataAgendamento = dataAgendamento; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getTipoDoador() { return tipoDoador; }
    public void setTipoDoador(String tipoDoador) { this.tipoDoador = tipoDoador; }

    public StatusDoador getStatus() { return status; }
    public void setStatus(StatusDoador status) { this.status = status; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}