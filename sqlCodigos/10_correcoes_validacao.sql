-- ============================================================
--  FLUXO VITAL — 10. Correções nas regras de validação
--
--  O 09_regras_validacao_formulario.sql tinha 3 problemas que este
--  script corrige (execute-o DEPOIS do 09; requer 01-09 já rodados):
--
--    1. A idade 16-69 estava igual pras 4 tabelas, mas os próprios
--       formulários prometem faixas diferentes por tipo de doação
--       (sangue 16-69, medula 18-55 REDOME, leite 16-45). Os
--       triggers de medula e leite são recriados aqui com a faixa
--       certa.
--    2. O IMC (peso/altura) era só uma coluna calculada, sem
--       bloquear nada. Agora vira CHECK: IMC abaixo de 18.5 impede
--       o INSERT/UPDATE em sangue e medula.
--    3. CPF e e-mail digitados no formulário tinham que bater com
--       os já cadastrados na CONTA do doador logado (doador_id),
--       e isso nunca era verificado — nem CPF nem e-mail.
--
--  Observação: a validação principal agora também acontece em Java
--  (DoadorController + ValidacaoDoacao), então essas regras aqui
--  são uma segunda camada — não a única. Se você rodar isto num
--  banco que já tem cadastros incompatíveis com as novas faixas
--  de idade, os UPDATEs futuros desses registros vão falhar até
--  os dados serem corrigidos.
-- ============================================================

USE fluxovital;

-- ────────────────────────────────────────────────────────────
-- 1. IMC mínimo (18.5) como CHECK de verdade em sangue e medula
-- ────────────────────────────────────────────────────────────
DELIMITER $$

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

-- imc_aceitavel só é NULL quando falta peso/altura (não bloqueia; o
-- Java já obriga os dois campos). Quando existe, tem que ser 1.
CALL fv_add_check_if_missing('doadores_sangue', 'chk_ds_imc_minimo',
  'imc_aceitavel IS NULL OR imc_aceitavel = 1');

CALL fv_add_check_if_missing('doadores_medula', 'chk_dm_imc_minimo',
  'imc_aceitavel IS NULL OR imc_aceitavel = 1');

DROP PROCEDURE IF EXISTS fv_add_check_if_missing;

-- ────────────────────────────────────────────────────────────
-- 2. Faixa de idade correta por tipo de doação + consistência de
--    CPF/e-mail com a conta do doador logado (doador_id)
-- ────────────────────────────────────────────────────────────

-- ─── DOADORES_MEDULA — idade 18 a 55 (REDOME) ─────────────────
DROP TRIGGER IF EXISTS trg_dm_valida_insert;
DROP TRIGGER IF EXISTS trg_dm_valida_update;

DELIMITER $$

CREATE TRIGGER trg_dm_valida_insert
BEFORE INSERT ON doadores_medula
FOR EACH ROW
BEGIN
  DECLARE v_cpf_conta VARCHAR(20);
  DECLARE v_email_conta VARCHAR(255);

  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 18 AND 55 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Doação de medula: idade deve estar entre 18 e 55 anos (REDOME).';
  END IF;

  IF NEW.data_coleta IS NOT NULL AND NEW.data_coleta <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de coleta precisa ser no futuro.';
  END IF;

  IF NEW.doador_id IS NOT NULL THEN
    SELECT cpf, email INTO v_cpf_conta, v_email_conta FROM doador WHERE id = NEW.doador_id;
    IF v_cpf_conta IS NOT NULL AND NEW.cpf IS NOT NULL AND v_cpf_conta <> NEW.cpf THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF informado é diferente do CPF já cadastrado na sua conta.';
    END IF;
    IF v_email_conta IS NOT NULL AND NEW.email IS NOT NULL AND v_email_conta <> NEW.email THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'E-mail informado é diferente do e-mail já cadastrado na sua conta.';
    END IF;
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
  DECLARE v_cpf_conta VARCHAR(20);
  DECLARE v_email_conta VARCHAR(255);

  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 18 AND 55 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Doação de medula: idade deve estar entre 18 e 55 anos (REDOME).';
  END IF;

  IF NEW.data_coleta IS NOT NULL AND NEW.data_coleta <> OLD.data_coleta
     AND NEW.data_coleta <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de coleta precisa ser no futuro.';
  END IF;

  IF NEW.doador_id IS NOT NULL THEN
    SELECT cpf, email INTO v_cpf_conta, v_email_conta FROM doador WHERE id = NEW.doador_id;
    IF v_cpf_conta IS NOT NULL AND NEW.cpf IS NOT NULL AND v_cpf_conta <> NEW.cpf THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF informado é diferente do CPF já cadastrado na sua conta.';
    END IF;
    IF v_email_conta IS NOT NULL AND NEW.email IS NOT NULL AND v_email_conta <> NEW.email THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'E-mail informado é diferente do e-mail já cadastrado na sua conta.';
    END IF;
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

-- ─── DOADORA_LEITE — idade 16 a 45 ─────────────────────────────
DROP TRIGGER IF EXISTS trg_dl_valida_insert;
DROP TRIGGER IF EXISTS trg_dl_valida_update;

DELIMITER $$

CREATE TRIGGER trg_dl_valida_insert
BEFORE INSERT ON doadora_leite
FOR EACH ROW
BEGIN
  DECLARE v_cpf_conta VARCHAR(20);
  DECLARE v_email_conta VARCHAR(255);

  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 45 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Doação de leite: idade deve estar entre 16 e 45 anos.';
  END IF;

  IF NEW.data_agendamento IS NOT NULL AND NEW.data_agendamento <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de agendamento precisa ser no futuro.';
  END IF;

  IF NEW.doador_id IS NOT NULL THEN
    SELECT cpf, email INTO v_cpf_conta, v_email_conta FROM doador WHERE id = NEW.doador_id;
    IF v_cpf_conta IS NOT NULL AND NEW.cpf IS NOT NULL AND v_cpf_conta <> NEW.cpf THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF informado é diferente do CPF já cadastrado na sua conta.';
    END IF;
    IF v_email_conta IS NOT NULL AND NEW.email IS NOT NULL AND v_email_conta <> NEW.email THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'E-mail informado é diferente do e-mail já cadastrado na sua conta.';
    END IF;
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
  DECLARE v_cpf_conta VARCHAR(20);
  DECLARE v_email_conta VARCHAR(255);

  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 45 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Doação de leite: idade deve estar entre 16 e 45 anos.';
  END IF;

  IF NEW.data_agendamento IS NOT NULL AND NEW.data_agendamento <> OLD.data_agendamento
     AND NEW.data_agendamento <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de agendamento precisa ser no futuro.';
  END IF;

  IF NEW.doador_id IS NOT NULL THEN
    SELECT cpf, email INTO v_cpf_conta, v_email_conta FROM doador WHERE id = NEW.doador_id;
    IF v_cpf_conta IS NOT NULL AND NEW.cpf IS NOT NULL AND v_cpf_conta <> NEW.cpf THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF informado é diferente do CPF já cadastrado na sua conta.';
    END IF;
    IF v_email_conta IS NOT NULL AND NEW.email IS NOT NULL AND v_email_conta <> NEW.email THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'E-mail informado é diferente do e-mail já cadastrado na sua conta.';
    END IF;
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

-- ─── DOADORES_SANGUE — mantém 16 a 69, só adiciona a checagem
--      de CPF/e-mail vs. conta do doador logado ─────────────────
DROP TRIGGER IF EXISTS trg_ds_valida_insert;
DROP TRIGGER IF EXISTS trg_ds_valida_update;

DELIMITER $$

CREATE TRIGGER trg_ds_valida_insert
BEFORE INSERT ON doadores_sangue
FOR EACH ROW
BEGIN
  DECLARE v_cpf_conta VARCHAR(20);
  DECLARE v_email_conta VARCHAR(255);

  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Doação de sangue: idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.data_agendamento IS NOT NULL AND NEW.data_agendamento <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de agendamento precisa ser no futuro.';
  END IF;

  IF NEW.doador_id IS NOT NULL THEN
    SELECT cpf, email INTO v_cpf_conta, v_email_conta FROM doador WHERE id = NEW.doador_id;
    IF v_cpf_conta IS NOT NULL AND NEW.cpf IS NOT NULL AND v_cpf_conta <> NEW.cpf THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF informado é diferente do CPF já cadastrado na sua conta.';
    END IF;
    IF v_email_conta IS NOT NULL AND NEW.email IS NOT NULL AND v_email_conta <> NEW.email THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'E-mail informado é diferente do e-mail já cadastrado na sua conta.';
    END IF;
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
  DECLARE v_cpf_conta VARCHAR(20);
  DECLARE v_email_conta VARCHAR(255);

  IF NEW.nascimento IS NOT NULL AND
     TIMESTAMPDIFF(YEAR, NEW.nascimento, CURDATE()) NOT BETWEEN 16 AND 69 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Doação de sangue: idade deve estar entre 16 e 69 anos.';
  END IF;

  IF NEW.data_agendamento IS NOT NULL AND NEW.data_agendamento <> OLD.data_agendamento
     AND NEW.data_agendamento <= CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data de agendamento precisa ser no futuro.';
  END IF;

  IF NEW.doador_id IS NOT NULL THEN
    SELECT cpf, email INTO v_cpf_conta, v_email_conta FROM doador WHERE id = NEW.doador_id;
    IF v_cpf_conta IS NOT NULL AND NEW.cpf IS NOT NULL AND v_cpf_conta <> NEW.cpf THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CPF informado é diferente do CPF já cadastrado na sua conta.';
    END IF;
    IF v_email_conta IS NOT NULL AND NEW.email IS NOT NULL AND v_email_conta <> NEW.email THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'E-mail informado é diferente do e-mail já cadastrado na sua conta.';
    END IF;
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

-- ────────────────────────────────────────────────────────────
-- Pronto. A partir de agora, além do que o 09 já fazia:
--  • Cada tipo de doação valida a idade na faixa certa (sangue
--    16-69, medula 18-55, leite 16-45), igual ao que o
--    formulário promete visualmente.
--  • IMC < 18.5 bloqueia o cadastro em sangue e medula (era só
--    informativo antes).
--  • CPF e e-mail digitados no formulário são comparados com os
--    da conta do doador logado (doador_id) — não só entre os
--    próprios formulários de doação.
-- ────────────────────────────────────────────────────────────
