-- ============================================================
--  FLUXO VITAL — 01. Tabela DOADOR (conta de login do doador)
--  Bate com model/Doador.java
-- ============================================================

USE fluxovital;

CREATE TABLE IF NOT EXISTS doador (
  id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
  nome              VARCHAR(150) NOT NULL,
  email             VARCHAR(150) NOT NULL UNIQUE,
  telefone          VARCHAR(20)  DEFAULT NULL,
  senha             VARCHAR(255) NOT NULL,

  cpf               VARCHAR(11)  DEFAULT NULL,
  rg                VARCHAR(20)  DEFAULT NULL,
  nascimento        DATE         DEFAULT NULL,
  sexo              VARCHAR(20)  DEFAULT NULL,
  peso              DOUBLE       DEFAULT NULL,

  tipo_sanguineo    VARCHAR(5)   DEFAULT NULL,
  historico_doacao  VARCHAR(20)  DEFAULT NULL,
  observacoes       VARCHAR(500) DEFAULT NULL,

  -- Condições de saúde (legado do cadastro inicial)
  vacina            TINYINT(1)   DEFAULT NULL,
  tatuagem          TINYINT(1)   DEFAULT NULL,
  cronica           TINYINT(1)   DEFAULT NULL,
  medicamento       TINYINT(1)   DEFAULT NULL,
  infeccao          TINYINT(1)   DEFAULT NULL,
  cirurgia          TINYINT(1)   DEFAULT NULL,

  unidade           VARCHAR(100) DEFAULT NULL,
  data_agendamento  DATE         DEFAULT NULL,
  turno             VARCHAR(20)  DEFAULT NULL,

  tipo_doador       VARCHAR(50)  DEFAULT NULL,   -- 'sangue' | 'medula' | 'leite'
  status            ENUM('ativo','inativo') NOT NULL DEFAULT 'ativo',
  criado_em         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
