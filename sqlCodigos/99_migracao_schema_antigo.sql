-- ============================================================
--  FLUXO VITAL — 99. Migração do schema antigo para o novo
--
--  Use este script SE VOCÊ JÁ TEM o banco `fluxovital` criado
--  (com tabelas de um script anterior) e não quer perder os
--  dados que já estão lá.
--
--  Se está criando um banco NOVO do zero, NÃO use este arquivo —
--  use 00 a 05 na ordem (veja o README.md).
--
--  Este script funciona em QUALQUER versão do MySQL 5.7+ e 8.x.
--  Ele não depende de "ADD COLUMN IF NOT EXISTS" (que só existe
--  a partir do MySQL 8.0.29 e foi o que deu erro de sintaxe da
--  vez passada) — em vez disso, usa duas pequenas rotinas
--  (stored procedures) que checam se a coluna/índice já existe
--  antes de tentar criar. Rode o arquivo inteiro de uma vez.
--
--  Faça backup antes:
--    mysqldump -u root -p fluxovital > backup_fluxovital.sql
-- ============================================================

USE fluxovital;

-- ────────────────────────────────────────────────────────────
-- 0. Rotinas auxiliares (funcionam em qualquer versão do MySQL)
-- ────────────────────────────────────────────────────────────
DELIMITER $$

DROP PROCEDURE IF EXISTS fv_add_column_if_missing $$
CREATE PROCEDURE fv_add_column_if_missing(
  IN p_tabela VARCHAR(64),
  IN p_coluna VARCHAR(64),
  IN p_definicao VARCHAR(255)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_tabela
      AND COLUMN_NAME = p_coluna
  ) THEN
    SET @fv_ddl = CONCAT('ALTER TABLE `', p_tabela, '` ADD COLUMN `', p_coluna, '` ', p_definicao);
    PREPARE fv_stmt FROM @fv_ddl;
    EXECUTE fv_stmt;
    DEALLOCATE PREPARE fv_stmt;
  END IF;
END $$

DROP PROCEDURE IF EXISTS fv_add_index_if_missing $$
CREATE PROCEDURE fv_add_index_if_missing(
  IN p_tabela VARCHAR(64),
  IN p_indice VARCHAR(64),
  IN p_colunas VARCHAR(255)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_tabela
      AND INDEX_NAME = p_indice
  ) THEN
    SET @fv_ddl = CONCAT('ALTER TABLE `', p_tabela, '` ADD INDEX `', p_indice, '` (', p_colunas, ')');
    PREPARE fv_stmt FROM @fv_ddl;
    EXECUTE fv_stmt;
    DEALLOCATE PREPARE fv_stmt;
  END IF;
END $$

DELIMITER ;

-- ────────────────────────────────────────────────────────────
-- 1. TABELA doador — colunas que o cadastro completo usa
-- ────────────────────────────────────────────────────────────
CALL fv_add_column_if_missing('doador', 'cpf',              'VARCHAR(11)  DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'rg',               'VARCHAR(20)  DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'nascimento',       'DATE         DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'sexo',             'VARCHAR(20)  DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'peso',             'DOUBLE       DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'tipo_sanguineo',   'VARCHAR(5)   DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'historico_doacao', 'VARCHAR(20)  DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'observacoes',      'VARCHAR(500) DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'vacina',           'TINYINT(1)   DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'tatuagem',         'TINYINT(1)   DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'cronica',          'TINYINT(1)   DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'medicamento',      'TINYINT(1)   DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'infeccao',         'TINYINT(1)   DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'cirurgia',         'TINYINT(1)   DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'unidade',          'VARCHAR(100) DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'data_agendamento', 'DATE         DEFAULT NULL');
CALL fv_add_column_if_missing('doador', 'turno',            'VARCHAR(20)  DEFAULT NULL');

-- ────────────────────────────────────────────────────────────
-- 2. TABELA doadores_sangue
-- ────────────────────────────────────────────────────────────
CALL fv_add_column_if_missing('doadores_sangue', 'interesse_doador',   'TINYINT(1)   NOT NULL DEFAULT 1');
CALL fv_add_column_if_missing('doadores_sangue', 'condicao_saude',     'TINYINT(1)   NOT NULL DEFAULT 0');
CALL fv_add_column_if_missing('doadores_sangue', 'descricao_condicao', 'VARCHAR(500) DEFAULT NULL');
CALL fv_add_column_if_missing('doadores_sangue', 'confirmado_em',      'DATETIME     DEFAULT NULL');
CALL fv_add_column_if_missing('doadores_sangue', 'cancelado_em',       'DATETIME     DEFAULT NULL');
CALL fv_add_column_if_missing('doadores_sangue', 'cancelado_por',      'VARCHAR(20)  DEFAULT NULL');

-- Mapeia os valores antigos de status para os novos ANTES de trocar o ENUM
-- (evita truncar/zerar linhas que já tinham 'aprovado' ou 'inabilitado')
UPDATE doadores_sangue SET status = 'confirmado' WHERE status = 'aprovado';
UPDATE doadores_sangue SET status = 'cancelado'  WHERE status = 'inabilitado';

-- Troca o ENUM para os valores usados pelo Java
-- (pendente | confirmado | cancelado | realizado)
-- ⚠️  Este é o passo que resolve o erro "Data truncated for column 'status'"
--     ao clicar em Confirmar/Cancelar.
ALTER TABLE doadores_sangue
  MODIFY COLUMN status ENUM('pendente','confirmado','cancelado','realizado')
  NOT NULL DEFAULT 'pendente';

-- ────────────────────────────────────────────────────────────
-- 3. TABELA doadores_medula — cria do zero se ainda não existir
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS doadores_medula (
  id                    BIGINT         AUTO_INCREMENT PRIMARY KEY,
  doador_id             BIGINT         DEFAULT NULL,

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

  historico_medula      VARCHAR(10)    DEFAULT NULL,
  cadastro_redome       VARCHAR(10)    DEFAULT NULL,
  modalidade            VARCHAR(20)    DEFAULT NULL,

  interesse_doador      TINYINT(1)     NOT NULL DEFAULT 1,
  condicao_saude        TINYINT(1)     NOT NULL DEFAULT 0,
  descricao_condicao    VARCHAR(500)   DEFAULT NULL,

  unidade               VARCHAR(100)   DEFAULT NULL,
  data_coleta           DATE           DEFAULT NULL,
  turno                 VARCHAR(20)    DEFAULT NULL,
  como_soube            VARCHAR(80)    DEFAULT NULL,
  observacoes           VARCHAR(1000)  DEFAULT NULL,
  consentimento         TINYINT(1)     NOT NULL DEFAULT 0,

  status                ENUM('pendente','confirmado','cancelado','realizado')
                         NOT NULL DEFAULT 'pendente',
  confirmado_em         DATETIME       DEFAULT NULL,
  cancelado_em          DATETIME       DEFAULT NULL,
  cancelado_por         VARCHAR(20)    DEFAULT NULL,
  criado_em             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_dm_doador FOREIGN KEY (doador_id)
    REFERENCES doador(id) ON DELETE SET NULL
);

-- Se a tabela já existia de uma tentativa anterior com ENUM antigo,
-- garante que fica com os valores certos também.
ALTER TABLE doadores_medula
  MODIFY COLUMN status ENUM('pendente','confirmado','cancelado','realizado')
  NOT NULL DEFAULT 'pendente';

-- ────────────────────────────────────────────────────────────
-- 4. TABELA doadora_leite
-- ────────────────────────────────────────────────────────────
CALL fv_add_column_if_missing('doadora_leite', 'interesse_doador',   'TINYINT(1)   NOT NULL DEFAULT 1');
CALL fv_add_column_if_missing('doadora_leite', 'condicao_saude',     'TINYINT(1)   NOT NULL DEFAULT 0');
CALL fv_add_column_if_missing('doadora_leite', 'descricao_condicao', 'VARCHAR(500) DEFAULT NULL');
CALL fv_add_column_if_missing('doadora_leite', 'confirmado_em',      'DATETIME     DEFAULT NULL');
CALL fv_add_column_if_missing('doadora_leite', 'cancelado_em',       'DATETIME     DEFAULT NULL');
CALL fv_add_column_if_missing('doadora_leite', 'cancelado_por',      'VARCHAR(20)  DEFAULT NULL');

-- Mapeia valores antigos de status, se existirem, antes de trocar o ENUM
UPDATE doadora_leite SET status = 'confirmado' WHERE status = 'aprovada';
UPDATE doadora_leite SET status = 'cancelado'  WHERE status = 'inabilitada';
UPDATE doadora_leite SET status = 'pendente'   WHERE status = 'em_avaliacao';

ALTER TABLE doadora_leite
  MODIFY COLUMN status ENUM('pendente','confirmado','cancelado','realizado')
  NOT NULL DEFAULT 'pendente';

-- ────────────────────────────────────────────────────────────
-- 5. Índices auxiliares para busca de CPF/RG duplicado
--    (a API /api/cpf/verificar e /api/rg/verificar ficam mais rápidas)
-- ────────────────────────────────────────────────────────────
CALL fv_add_index_if_missing('doadores_sangue', 'idx_ds_cpf', 'cpf');
CALL fv_add_index_if_missing('doadores_sangue', 'idx_ds_rg',  'rg');
CALL fv_add_index_if_missing('doadores_medula', 'idx_dm_cpf', 'cpf');
CALL fv_add_index_if_missing('doadores_medula', 'idx_dm_rg',  'rg');
CALL fv_add_index_if_missing('doadora_leite',   'idx_dl_cpf', 'cpf');
CALL fv_add_index_if_missing('doadora_leite',   'idx_dl_rg',  'rg');

-- ────────────────────────────────────────────────────────────
-- 6. Limpeza — remove as rotinas auxiliares (não precisa mais delas)
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS fv_add_column_if_missing;
DROP PROCEDURE IF EXISTS fv_add_index_if_missing;

-- ────────────────────────────────────────────────────────────
-- 7. Conferência final — confira se o status de cada tabela
--    já aceita 'confirmado' e 'cancelado'
-- ────────────────────────────────────────────────────────────
SHOW COLUMNS FROM doadores_sangue LIKE 'status';
SHOW COLUMNS FROM doadora_leite   LIKE 'status';
SHOW COLUMNS FROM doadores_medula LIKE 'status';

-- Pronto. Depois de rodar este script, reinicie a aplicação.
-- (spring.jpa.hibernate.ddl-auto=update cuida de qualquer coluna
-- nova que ainda faltar, mas o ENUM de status e a tabela de
-- medula precisavam desse ajuste manual.)
--
-- Se você também quer as tabelas instituicoes / campanhas /
-- doacoes_recebidas num banco migrado, rode depois:
--   05_schema_instituicao.sql
--   07_legado_campanhas_doacoes.sql (opcional)
