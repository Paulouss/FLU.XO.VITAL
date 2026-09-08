-- ============================================================
--  FLUXO VITAL — 05. Tabela INSTITUICOES
--  Bate com model/Instituicao.java
-- ============================================================

USE fluxovital;

CREATE TABLE IF NOT EXISTS instituicoes (
  id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
  nome          VARCHAR(150)  NOT NULL,
  cnpj          CHAR(14)      NOT NULL UNIQUE,
  email         VARCHAR(100)  NOT NULL UNIQUE,
  senha         VARCHAR(255)  NOT NULL,
  telefone      VARCHAR(20)   DEFAULT NULL,
  categoria     ENUM(
                  'BANCOS_DE_DOACAO',
                  'HOSPITAIS',
                  'ONGS',
                  'OUTROS'
                )             NOT NULL,
  endereco      VARCHAR(200)  DEFAULT NULL,
  cidade        VARCHAR(100)  NOT NULL,
  estado        CHAR(2)       NOT NULL,
  cep           VARCHAR(10)   DEFAULT NULL,
  data_cadastro DATE          NOT NULL DEFAULT (CURRENT_DATE)
);
