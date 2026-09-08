-- ============================================================
--  FLUXO VITAL — 03. Formulários de doação de MEDULA ÓSSEA
--  Bate com model/DoadorMedula.java (@Table doadores_medula)
--  Requer 01_schema_doador.sql já executado.
-- ============================================================

USE fluxovital;

CREATE TABLE IF NOT EXISTS doadores_medula (
  id                    BIGINT         AUTO_INCREMENT PRIMARY KEY,
  doador_id             BIGINT         DEFAULT NULL,

  -- Identificação
  nome                  VARCHAR(150)   NOT NULL,
  cpf                   VARCHAR(11)    DEFAULT NULL,
  rg                    VARCHAR(20)    DEFAULT NULL,
  nascimento            DATE           DEFAULT NULL,
  sexo                  VARCHAR(20)    DEFAULT NULL,
  peso                  DOUBLE         DEFAULT NULL,
  altura                DOUBLE         DEFAULT NULL,
  etnia                 VARCHAR(30)    DEFAULT NULL,
  email                 VARCHAR(150)   DEFAULT NULL,
  telefone              VARCHAR(20)    DEFAULT NULL,

  -- Histórico e informações adicionais
  historico_medula      VARCHAR(10)    DEFAULT NULL,  -- 'sim' | 'nao'
  cadastro_redome       VARCHAR(10)    DEFAULT NULL,  -- 'sim' | 'nao' | 'nao_sei'
  modalidade            VARCHAR(20)    DEFAULT NULL,   -- periferico | puncao | qualquer | medico

  -- Interesse e questionário simplificado
  interesse_doador      TINYINT(1)     NOT NULL DEFAULT 1,
  condicao_saude        TINYINT(1)     NOT NULL DEFAULT 0,
  descricao_condicao    VARCHAR(500)   DEFAULT NULL,

  -- Agendamento (coleta de tipagem HLA)
  unidade               VARCHAR(100)   DEFAULT NULL,
  data_coleta           DATE           DEFAULT NULL,
  turno                 VARCHAR(20)    DEFAULT NULL,
  como_soube            VARCHAR(80)    DEFAULT NULL,
  observacoes           VARCHAR(1000)  DEFAULT NULL,
  consentimento         TINYINT(1)     NOT NULL DEFAULT 0,

  -- Controle do agendamento
  status                ENUM('pendente','confirmado','cancelado','realizado')
                         NOT NULL DEFAULT 'pendente',
  confirmado_em         DATETIME       DEFAULT NULL,
  cancelado_em          DATETIME       DEFAULT NULL,
  cancelado_por         VARCHAR(20)    DEFAULT NULL,
  criado_em             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

  INDEX idx_dm_cpf (cpf),
  INDEX idx_dm_rg (rg),
  CONSTRAINT fk_dm_doador FOREIGN KEY (doador_id)
    REFERENCES doador(id) ON DELETE SET NULL
);
