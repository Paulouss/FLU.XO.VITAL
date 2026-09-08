-- ============================================================
--  FLUXO VITAL — 09. Regras de validação do formulário (no banco)
--
--  Implementa, direto no MySQL, as regras que hoje só existem no
--  JavaScript do formulário (ou nem existem ainda):
--
--    1. RG só com 12 números.
--    2. Nascimento: idade tem que estar entre 16 e 69 anos.
--    3. Data de agendamento/coleta tem que ser no futuro (não pode
--       marcar pra hoje nem pra uma data que já passou).
--    4. Mesmo CPF não pode ter nome/data de nascimento diferentes
--       em outro formulário (doador, sangue, medula ou leite).
--    5. Última doação não pode ser antes de ter nascido.
--    6. Calcula o IMC (peso ÷ altura²) em sangue e medula e marca
--       se o doador ficou abaixo do IMC mínimo (18.5).
--
--  Requer 01, 02, 03, 04 já executados. Idempotente — pode rodar
--  de novo sem dar erro de "coluna/constraint já existe".
--
--  ⚠️  Os CHECKs (itens 1 e 5) exigem MySQL 8.0.16+. Rode
--      `SELECT VERSION();` pra conferir. Numa versão mais antiga
--      (ou MariaDB < 10.2), me avise que eu troco por triggers.
-- ============================================================

USE fluxovital;

-- ────────────────────────────────────────────────────────────
-- 0. DIAGNÓSTICO — por que o agendamento de sangue não aparece
--    na tabela `agendamentos`?
--
--    A causa mais comum: o registro em doadores_sangue foi criado
--    ANTES de você rodar o 08_agendamento_generico.sql. Os triggers
--    só disparam pra INSERT/UPDATE que acontecem DEPOIS de criados
--    — quem já existia antes não entra sozinho, precisa do backfill.
--
--    Rode as 3 consultas abaixo pra confirmar:
-- ────────────────────────────────────────────────────────────

-- 0a) Os triggers de sangue existem mesmo no banco que a aplicação usa?
SHOW TRIGGERS FROM fluxovital WHERE `Table` = 'doadores_sangue';

-- 0b) Comparar quantidade: se doadores_sangue tiver mais linhas do
--     que agendamentos (tipo='sangue'), é sinal de que faltou sincronizar.
SELECT
  (SELECT COUNT(*) FROM doadores_sangue) AS total_doadores_sangue,
  (SELECT COUNT(*) FROM agendamentos WHERE tipo_doacao = 'sangue') AS total_agendamentos_sangue;

-- 0c) CORREÇÃO — re-roda o backfill (é seguro rodar de novo,
--     ON DUPLICATE KEY UPDATE evita duplicar):
INSERT INTO agendamentos
  (tipo_doacao, origem_id, doador_id, nome, email, telefone,
   tipo_sanguineo, unidade, data_agendamento, turno,
   status, confirmado_em, cancelado_em, cancelado_por, criado_em)
SELECT 'sangue', id, doador_id, nome, email, telefone,
       tipo_sanguineo, unidade, data_agendamento, turno,
       status, confirmado_em, cancelado_em, cancelado_por, criado_em
FROM doadores_sangue
ON DUPLICATE KEY UPDATE
  doador_id = VALUES(doador_id), nome = VALUES(nome), email = VALUES(email),
  telefone = VALUES(telefone), tipo_sanguineo = VALUES(tipo_sanguineo),
  unidade = VALUES(unidade), data_agendamento = VALUES(data_agendamento),
  turno = VALUES(turno), status = VALUES(status),
  confirmado_em = VALUES(confirmado_em), cancelado_em = VALUES(cancelado_em),
  cancelado_por = VALUES(cancelado_por);

-- Se depois disso o total ainda não bater, o problema não é o
-- trigger — é a aplicação estar conectada num banco/schema
-- diferente do que você rodou o 08. Confira a mesma URL/porta/nome
-- de banco em application.properties (spring.datasource.url).

-- ────────────────────────────────────────────────────────────
-- 1. Rotinas auxiliares (idempotência — mesmo padrão do
--    99_migracao_schema_antigo.sql, funciona em qualquer versão)
-- ────────────────────────────────────────────────────────────
DELIMITER $$

DROP PROCEDURE IF EXISTS fv_add_column_if_missing $$
CREATE PROCEDURE fv_add_column_if_missing(
  IN p_tabela VARCHAR(64), IN p_coluna VARCHAR(64), IN p_definicao VARCHAR(500)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_tabela AND COLUMN_NAME = p_coluna
  ) THEN
    SET @fv_ddl = CONCAT('ALTER TABLE `', p_tabela, '` ADD COLUMN `', p_coluna, '` ', p_definicao);
    PREPARE fv_stmt FROM @fv_ddl; EXECUTE fv_stmt; DEALLOCATE PREPARE fv_stmt;
  END IF;
END $$

DROP PROCEDURE IF EXISTS fv_add_check_if_missing $$
CREATE PROCEDURE fv_add_check_if_missing(
  IN p_tabela VARCHAR(64), IN p_nome VARCHAR(64), IN p_expressao VARCHAR(500)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = p_tabela
      AND CONSTRAINT_NAME = p_nome AND CONSTRAINT_TYPE = 'CHECK'
  ) THEN
    SET @fv_ddl = CONCAT('ALTER TABLE `', p_tabela, '` ADD CONSTRAINT `', p_nome, '` CHECK (', p_expressao, ')');
    PREPARE fv_stmt FROM @fv_ddl; EXECUTE fv_stmt; DEALLOCATE PREPARE fv_stmt;
  END IF;
END $$

DELIMITER ;

-- ────────────────────────────────────────────────────────────
-- 2. IMC — sangue e medula (peso em kg, altura em cm no formulário)
--    IMC mínimo considerado apto: 18.5 (abaixo disso = abaixo do peso)
--    Se quiser mudar o mínimo, troque os dois ">= 18.5" abaixo e
--    rode de novo (ele não recria a coluna, então nesse caso
--    troque por ALTER TABLE ... MODIFY COLUMN manualmente).
-- ────────────────────────────────────────────────────────────
CALL fv_add_column_if_missing('doadores_sangue', 'imc',
  'DECIMAL(5,2) GENERATED ALWAYS AS (
     CASE WHEN peso IS NOT NULL AND altura IS NOT NULL AND altura > 0
          THEN ROUND(peso / POWER(altura/100, 2), 2)
     END
   ) STORED');

CALL fv_add_column_if_missing('doadores_sangue', 'imc_aceitavel',
  'TINYINT(1) GENERATED ALWAYS AS (
     CASE WHEN peso IS NULL OR altura IS NULL OR altura <= 0 THEN NULL
          WHEN ROUND(peso / POWER(altura/100, 2), 2) >= 18.5 THEN 1
          ELSE 0
     END
   ) STORED');

CALL fv_add_column_if_missing('doadores_medula', 'imc',
  'DECIMAL(5,2) GENERATED ALWAYS AS (
     CASE WHEN peso IS NOT NULL AND altura IS NOT NULL AND altura > 0
          THEN ROUND(peso / POWER(altura/100, 2), 2)
     END
   ) STORED');

CALL fv_add_column_if_missing('doadores_medula', 'imc_aceitavel',
  'TINYINT(1) GENERATED ALWAYS AS (
     CASE WHEN peso IS NULL OR altura IS NULL OR altura <= 0 THEN NULL
          WHEN ROUND(peso / POWER(altura/100, 2), 2) >= 18.5 THEN 1
          ELSE 0
     END
   ) STORED');

-- doadora_leite não entra aqui — a tabela não tem peso/altura.

-- ────────────────────────────────────────────────────────────
-- 3. RG — só 12 números (doador, sangue, medula, leite)
-- ────────────────────────────────────────────────────────────
CALL fv_add_check_if_missing('doador',          'chk_doador_rg',          "rg IS NULL OR rg REGEXP '^[0-9]{12}$'");
CALL fv_add_check_if_missing('doadores_sangue', 'chk_ds_rg',              "rg IS NULL OR rg REGEXP '^[0-9]{12}$'");
CALL fv_add_check_if_missing('doadores_medula', 'chk_dm_rg',              "rg IS NULL OR rg REGEXP '^[0-9]{12}$'");
CALL fv_add_check_if_missing('doadora_leite',   'chk_dl_rg',              "rg IS NULL OR rg REGEXP '^[0-9]{12}$'");

-- ────────────────────────────────────────────────────────────
-- 4. Última doação não pode ser antes de ter nascido (só sangue,
--    é a única tabela com ultima_doacao)
-- ────────────────────────────────────────────────────────────
CALL fv_add_check_if_missing('doadores_sangue', 'chk_ds_ultima_doacao_pos_nascimento',
  'ultima_doacao IS NULL OR nascimento IS NULL OR ultima_doacao >= nascimento');

-- ────────────────────────────────────────────────────────────
-- 5. Limpeza das rotinas auxiliares (não precisamos mais delas)
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS fv_add_column_if_missing;
DROP PROCEDURE IF EXISTS fv_add_check_if_missing;

-- ────────────────────────────────────────────────────────────
-- 6. TRIGGERS — idade (16-69), data de agendamento/coleta no
--    futuro e CPF consistente entre os 4 formulários.
--    (Usam CURDATE(), por isso não dá pra ser CHECK — CHECK do
--    MySQL não aceita função que muda com o tempo.)
-- ────────────────────────────────────────────────────────────

-- ─── DOADOR ─────────────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_doador_valida_insert;
DROP TRIGGER IF EXISTS trg_doador_valida_update;

DELIMITER $$

CREATE TRIGGER trg_doador_valida_insert
BEFORE INSERT ON doador
FOR EACH ROW
BEGIN
  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.cpf IS NOT NULL AND EXISTS (
    SELECT 1 FROM (
      SELECT nome, nascimento FROM doadores_sangue WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_medula WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadora_leite WHERE cpf = NEW.cpf
    ) existentes
    WHERE existentes.nome <> NEW.nome
       OR (existentes.nascimento IS NOT NULL AND NEW.nascimento IS NOT NULL AND existentes.nascimento <> NEW.nascimento)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF já cadastrado com nome ou data de nascimento diferente em outro formulário.';
  END IF;
END$$

CREATE TRIGGER trg_doador_valida_update
BEFORE UPDATE ON doador
FOR EACH ROW
BEGIN
  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.cpf IS NOT NULL AND EXISTS (
    SELECT 1 FROM (
      SELECT nome, nascimento FROM doadores_sangue WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_medula WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadora_leite WHERE cpf = NEW.cpf
    ) existentes
    WHERE existentes.nome <> NEW.nome
       OR (existentes.nascimento IS NOT NULL AND NEW.nascimento IS NOT NULL AND existentes.nascimento <> NEW.nascimento)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF já cadastrado com nome ou data de nascimento diferente em outro formulário.';
  END IF;
END$$

DELIMITER ;

-- ─── DOADORES_SANGUE ────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_ds_valida_insert;
DROP TRIGGER IF EXISTS trg_ds_valida_update;

DELIMITER $$

CREATE TRIGGER trg_ds_valida_insert
BEFORE INSERT ON doadores_sangue
FOR EACH ROW
BEGIN
  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.data_agendamento IS NOT NULL AND NEW.data_agendamento <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de agendamento precisa ser no futuro.';
  END IF;

  IF NEW.cpf IS NOT NULL AND EXISTS (
    SELECT 1 FROM (
      SELECT nome, nascimento FROM doador WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_medula WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadora_leite WHERE cpf = NEW.cpf
    ) existentes
    WHERE existentes.nome <> NEW.nome
       OR (existentes.nascimento IS NOT NULL AND NEW.nascimento IS NOT NULL AND existentes.nascimento <> NEW.nascimento)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF já cadastrado com nome ou data de nascimento diferente em outro formulário.';
  END IF;
END$$

CREATE TRIGGER trg_ds_valida_update
BEFORE UPDATE ON doadores_sangue
FOR EACH ROW
BEGIN
  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.data_agendamento IS NOT NULL AND NEW.data_agendamento <> OLD.data_agendamento
     AND NEW.data_agendamento <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de agendamento precisa ser no futuro.';
  END IF;

  IF NEW.cpf IS NOT NULL AND EXISTS (
    SELECT 1 FROM (
      SELECT nome, nascimento FROM doador WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_medula WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadora_leite WHERE cpf = NEW.cpf
    ) existentes
    WHERE existentes.nome <> NEW.nome
       OR (existentes.nascimento IS NOT NULL AND NEW.nascimento IS NOT NULL AND existentes.nascimento <> NEW.nascimento)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF já cadastrado com nome ou data de nascimento diferente em outro formulário.';
  END IF;
END$$

DELIMITER ;

-- ─── DOADORES_MEDULA ────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_dm_valida_insert;
DROP TRIGGER IF EXISTS trg_dm_valida_update;

DELIMITER $$

CREATE TRIGGER trg_dm_valida_insert
BEFORE INSERT ON doadores_medula
FOR EACH ROW
BEGIN
  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.data_coleta IS NOT NULL AND NEW.data_coleta <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de coleta precisa ser no futuro.';
  END IF;

  IF NEW.cpf IS NOT NULL AND EXISTS (
    SELECT 1 FROM (
      SELECT nome, nascimento FROM doador WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_sangue WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadora_leite WHERE cpf = NEW.cpf
    ) existentes
    WHERE existentes.nome <> NEW.nome
       OR (existentes.nascimento IS NOT NULL AND NEW.nascimento IS NOT NULL AND existentes.nascimento <> NEW.nascimento)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF já cadastrado com nome ou data de nascimento diferente em outro formulário.';
  END IF;
END$$

CREATE TRIGGER trg_dm_valida_update
BEFORE UPDATE ON doadores_medula
FOR EACH ROW
BEGIN
  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.data_coleta IS NOT NULL AND NEW.data_coleta <> OLD.data_coleta
     AND NEW.data_coleta <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de coleta precisa ser no futuro.';
  END IF;

  IF NEW.cpf IS NOT NULL AND EXISTS (
    SELECT 1 FROM (
      SELECT nome, nascimento FROM doador WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_sangue WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadora_leite WHERE cpf = NEW.cpf
    ) existentes
    WHERE existentes.nome <> NEW.nome
       OR (existentes.nascimento IS NOT NULL AND NEW.nascimento IS NOT NULL AND existentes.nascimento <> NEW.nascimento)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF já cadastrado com nome ou data de nascimento diferente em outro formulário.';
  END IF;
END$$

DELIMITER ;

-- ─── DOADORA_LEITE ──────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_dl_valida_insert;
DROP TRIGGER IF EXISTS trg_dl_valida_update;

DELIMITER $$

CREATE TRIGGER trg_dl_valida_insert
BEFORE INSERT ON doadora_leite
FOR EACH ROW
BEGIN
  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.data_agendamento IS NOT NULL AND NEW.data_agendamento <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de agendamento precisa ser no futuro.';
  END IF;

  IF NEW.cpf IS NOT NULL AND EXISTS (
    SELECT 1 FROM (
      SELECT nome, nascimento FROM doador WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_sangue WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_medula WHERE cpf = NEW.cpf
    ) existentes
    WHERE existentes.nome <> NEW.nome
       OR (existentes.nascimento IS NOT NULL AND NEW.nascimento IS NOT NULL AND existentes.nascimento <> NEW.nascimento)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF já cadastrado com nome ou data de nascimento diferente em outro formulário.';
  END IF;
END$$

CREATE TRIGGER trg_dl_valida_update
BEFORE UPDATE ON doadora_leite
FOR EACH ROW
BEGIN
  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.data_agendamento IS NOT NULL AND NEW.data_agendamento <> OLD.data_agendamento
     AND NEW.data_agendamento <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de agendamento precisa ser no futuro.';
  END IF;

  IF NEW.cpf IS NOT NULL AND EXISTS (
    SELECT 1 FROM (
      SELECT nome, nascimento FROM doador WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_sangue WHERE cpf = NEW.cpf
      UNION ALL SELECT nome, nascimento FROM doadores_medula WHERE cpf = NEW.cpf
    ) existentes
    WHERE existentes.nome <> NEW.nome
       OR (existentes.nascimento IS NOT NULL AND NEW.nascimento IS NOT NULL AND existentes.nascimento <> NEW.nascimento)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF já cadastrado com nome ou data de nascimento diferente em outro formulário.';
  END IF;
END$$

DELIMITER ;

-- ────────────────────────────────────────────────────────────
-- 7. Conferência final
-- ────────────────────────────────────────────────────────────
SHOW CREATE TABLE doadores_sangue\G
SHOW CREATE TABLE doadores_medula\G

SELECT id, nome, peso, altura, imc, imc_aceitavel FROM doadores_sangue ORDER BY id DESC LIMIT 10;
SELECT id, nome, peso, altura, imc, imc_aceitavel FROM doadores_medula ORDER BY id DESC LIMIT 10;

-- Pronto. A partir de agora, direto no banco:
--  • RG só passa com 12 dígitos numéricos.
--  • Nascimento fora de 16-69 anos é rejeitado.
--  • data_agendamento / data_coleta no passado ou hoje é rejeitado.
--  • Mesmo CPF com nome/nascimento divergente em outro formulário é rejeitado.
--  • ultima_doacao antes do nascimento é rejeitado (sangue).
--  • imc e imc_aceitavel são calculados sozinhos em sangue/medula.
