-- ============================================================
--  FLUXO VITAL — 02. Formulários de doação de SANGUE
--  Bate com model/DoadorSangue.java (@Table doadores_sangue)
--  Requer 01_schema_doador.sql já executado.
-- ============================================================

USE fluxovital;

CREATE TABLE IF NOT EXISTS doadores_sangue (
  id                  BIGINT         AUTO_INCREMENT PRIMARY KEY,
  doador_id           BIGINT         DEFAULT NULL,

  -- Identificação
  nome                VARCHAR(150)   NOT NULL,
  cpf                 VARCHAR(11)    DEFAULT NULL,
  rg                  VARCHAR(20)    DEFAULT NULL,
  nascimento          DATE           DEFAULT NULL,
  sexo                ENUM('masculino','feminino') DEFAULT NULL,
  peso                DOUBLE         DEFAULT NULL,
  altura              DOUBLE         DEFAULT NULL,
  email               VARCHAR(150)   DEFAULT NULL,
  telefone            VARCHAR(20)    DEFAULT NULL,
  endereco            VARCHAR(250)   DEFAULT NULL,
  tipo_sanguineo      VARCHAR(5)     DEFAULT NULL,
  ultima_doacao       DATE           DEFAULT NULL,

  historico_doacao    ENUM('nunca','sim','regular') DEFAULT NULL,
  observacoes         VARCHAR(600)   DEFAULT NULL,

  -- Interesse e questionário simplificado
  interesse_doador    TINYINT(1)     NOT NULL DEFAULT 1,
  condicao_saude      TINYINT(1)     NOT NULL DEFAULT 0,
  descricao_condicao  VARCHAR(500)   DEFAULT NULL,

  -- Agendamento
  unidade             VARCHAR(100)   DEFAULT NULL,
  data_agendamento    DATE           DEFAULT NULL,
  turno               ENUM('manha','tarde') DEFAULT NULL,
  como_soube          VARCHAR(80)    DEFAULT NULL,
  consentimento       TINYINT(1)     NOT NULL DEFAULT 0,

  -- Controle do agendamento (doador/instituição confirmam ou cancelam)
  status              ENUM('pendente','confirmado','cancelado','realizado')
                       NOT NULL DEFAULT 'pendente',
  confirmado_em       DATETIME       DEFAULT NULL,
  cancelado_em        DATETIME       DEFAULT NULL,
  cancelado_por       VARCHAR(20)    DEFAULT NULL,  -- 'doador' | 'instituicao'
  criado_em           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

  INDEX idx_ds_cpf (cpf),
  INDEX idx_ds_rg (rg),
  CONSTRAINT fk_ds_doador FOREIGN KEY (doador_id)
    REFERENCES doador(id) ON DELETE SET NULL
);
