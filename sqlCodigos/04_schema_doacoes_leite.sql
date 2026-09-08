-- ============================================================
--  FLUXO VITAL — 04. Formulários de doação de LEITE
--  Bate com model/DoadoraLeite.java (@Table doadora_leite)
--  Requer 01_schema_doador.sql já executado.
-- ============================================================

USE fluxovital;

CREATE TABLE IF NOT EXISTS doadora_leite (
  id                     BIGINT         AUTO_INCREMENT PRIMARY KEY,
  doador_id              BIGINT         DEFAULT NULL,

  -- Identificação
  nome                   VARCHAR(150)   NOT NULL,
  cpf                    VARCHAR(11)    DEFAULT NULL,
  rg                     VARCHAR(20)    DEFAULT NULL,
  nascimento             DATE           DEFAULT NULL,
  email                  VARCHAR(150)   DEFAULT NULL,
  telefone               VARCHAR(20)    DEFAULT NULL,
  endereco               VARCHAR(250)   DEFAULT NULL,

  -- Amamentação
  data_parto             DATE           DEFAULT NULL,
  tipo_parto             ENUM('normal','cesariana') DEFAULT NULL,
  idade_gestacional      ENUM('prematuro_extremo','prematuro_moderado','prematuro_tardio','termo') DEFAULT NULL,
  num_filhos_amamentando VARCHAR(5)     DEFAULT NULL,
  amamentacao_exclusiva  ENUM('sim','parcial','nao') DEFAULT NULL,
  producao_estimada      VARCHAR(20)    DEFAULT NULL,

  historico_doacao       ENUM('nunca','sim','regular') DEFAULT NULL,
  modalidade_coleta      ENUM('unidade','domicilio','ambos') DEFAULT NULL,
  observacoes            TEXT           DEFAULT NULL,

  -- Interesse e questionário simplificado
  interesse_doador       TINYINT(1)     NOT NULL DEFAULT 1,
  condicao_saude         TINYINT(1)     NOT NULL DEFAULT 0,
  descricao_condicao     VARCHAR(500)   DEFAULT NULL,

  -- Agendamento
  unidade                VARCHAR(100)   DEFAULT NULL,
  data_agendamento       DATE           DEFAULT NULL,
  turno                  ENUM('manha','tarde') DEFAULT NULL,
  como_soube             VARCHAR(80)    DEFAULT NULL,
  consentimento          TINYINT(1)     NOT NULL DEFAULT 0,

  -- Controle do agendamento
  status                 ENUM('pendente','confirmado','cancelado','realizado')
                          NOT NULL DEFAULT 'pendente',
  confirmado_em          DATETIME       DEFAULT NULL,
  cancelado_em           DATETIME       DEFAULT NULL,
  cancelado_por          VARCHAR(20)    DEFAULT NULL,
  criado_em              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em          DATETIME       DEFAULT NULL,

  INDEX idx_dl_cpf (cpf),
  INDEX idx_dl_rg (rg),
  CONSTRAINT fk_dl_doador FOREIGN KEY (doador_id)
    REFERENCES doador(id) ON DELETE SET NULL
);
