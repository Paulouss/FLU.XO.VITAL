-- ============================================================
--  FLUXO VITAL — 08. Agendamento genérico (multi-tipo)
--
--  Objetivo: criar uma tabela `agendamentos` que recebe e
--  guarda TODOS os tipos de agendamento (sangue, medula, leite)
--  num único lugar, de forma genérica — sem tirar nem alterar
--  nada do que já existe.
--
--  ⚠️  NADA do agendamento atual muda:
--    - doadores_sangue, doadores_medula e doadora_leite continuam
--      exatamente como estão, guardando os dados na própria tabela,
--      do jeito que a aplicação (DoadorController) já faz hoje.
--    - Nenhuma entidade Java, repository ou controller precisa
--      mudar. Isso aqui é só banco de dados.
--
--  Como funciona:
--    1. Cria a tabela `agendamentos`, genérica, com uma coluna
--       `tipo_doacao` (sangue | medula | leite) dizendo de qual
--       tabela específica aquele registro veio.
--    2. Cria TRIGGERS nas 3 tabelas (sangue/medula/leite): toda
--       vez que a aplicação faz um INSERT (novo agendamento) ou
--       um UPDATE (confirmar/cancelar), o próprio MySQL replica
--       automaticamente essa informação para `agendamentos`.
--       A aplicação continua escrevendo só nas tabelas de sempre;
--       quem duplica pra tabela genérica é o banco.
--    3. Faz um backfill (INSERT ... SELECT) dos agendamentos que
--       já existiam antes de rodar este script.
--
--  Requer 01, 02, 03 e 04 já executados.
--  Pode ser rodado tanto num banco novo quanto num banco já em uso
--  (o script é idempotente: DROP ... IF EXISTS antes de recriar).
-- ============================================================

USE fluxovital;

-- ────────────────────────────────────────────────────────────
-- 1. Tabela genérica de agendamentos (todos os tipos juntos)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS agendamentos (
  id                BIGINT       AUTO_INCREMENT PRIMARY KEY,

  -- De qual tipo/tabela esse agendamento veio
  tipo_doacao       ENUM('sangue','medula','leite') NOT NULL,
  origem_id         BIGINT       NOT NULL,   -- id na tabela específica (doadores_sangue/doadores_medula/doadora_leite)

  -- Dados comuns aos 3 tipos
  doador_id         BIGINT       DEFAULT NULL,
  nome              VARCHAR(150) DEFAULT NULL,
  email             VARCHAR(150) DEFAULT NULL,
  telefone          VARCHAR(20)  DEFAULT NULL,

  -- Só existe pra sangue, mas fica aqui pra tabela ser mais completa
  tipo_sanguineo    VARCHAR(5)   DEFAULT NULL,

  -- Agendamento em si (nomes genéricos: data_coleta da medula também
  -- vem pra cá como data_agendamento)
  unidade           VARCHAR(100) DEFAULT NULL,
  data_agendamento  DATE         DEFAULT NULL,
  turno             VARCHAR(20)  DEFAULT NULL,

  -- Controle (mesmo fluxo que já existe hoje nas 3 tabelas)
  status            ENUM('pendente','confirmado','cancelado','realizado')
                     NOT NULL DEFAULT 'pendente',
  confirmado_em     DATETIME     DEFAULT NULL,
  cancelado_em      DATETIME     DEFAULT NULL,
  cancelado_por     VARCHAR(20)  DEFAULT NULL,

  criado_em         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em     DATETIME     DEFAULT NULL,

  -- Um agendamento de origem só pode ter uma linha aqui
  UNIQUE KEY uk_agendamento_origem (tipo_doacao, origem_id),
  INDEX idx_ag_status (status),
  INDEX idx_ag_tipo (tipo_doacao),
  INDEX idx_ag_doador (doador_id),

  CONSTRAINT fk_agendamento_doador FOREIGN KEY (doador_id)
    REFERENCES doador(id) ON DELETE SET NULL
);

-- ────────────────────────────────────────────────────────────
-- 2. TRIGGERS — sangue (doadores_sangue)
-- ────────────────────────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_ds_agendamento_insert;
DROP TRIGGER IF EXISTS trg_ds_agendamento_update;

DELIMITER $$

CREATE TRIGGER trg_ds_agendamento_insert
AFTER INSERT ON doadores_sangue
FOR EACH ROW
BEGIN
  INSERT INTO agendamentos
    (tipo_doacao, origem_id, doador_id, nome, email, telefone,
     tipo_sanguineo, unidade, data_agendamento, turno,
     status, confirmado_em, cancelado_em, cancelado_por, criado_em)
  VALUES
    ('sangue', NEW.id, NEW.doador_id, NEW.nome, NEW.email, NEW.telefone,
     NEW.tipo_sanguineo, NEW.unidade, NEW.data_agendamento, NEW.turno,
     NEW.status, NEW.confirmado_em, NEW.cancelado_em, NEW.cancelado_por, NEW.criado_em)
  ON DUPLICATE KEY UPDATE
    doador_id = NEW.doador_id, nome = NEW.nome, email = NEW.email,
    telefone = NEW.telefone, tipo_sanguineo = NEW.tipo_sanguineo,
    unidade = NEW.unidade, data_agendamento = NEW.data_agendamento,
    turno = NEW.turno, status = NEW.status,
    confirmado_em = NEW.confirmado_em, cancelado_em = NEW.cancelado_em,
    cancelado_por = NEW.cancelado_por;
END$$

CREATE TRIGGER trg_ds_agendamento_update
AFTER UPDATE ON doadores_sangue
FOR EACH ROW
BEGIN
  UPDATE agendamentos
  SET doador_id = NEW.doador_id,
      nome = NEW.nome,
      email = NEW.email,
      telefone = NEW.telefone,
      tipo_sanguineo = NEW.tipo_sanguineo,
      unidade = NEW.unidade,
      data_agendamento = NEW.data_agendamento,
      turno = NEW.turno,
      status = NEW.status,
      confirmado_em = NEW.confirmado_em,
      cancelado_em = NEW.cancelado_em,
      cancelado_por = NEW.cancelado_por,
      atualizado_em = NOW()
  WHERE tipo_doacao = 'sangue' AND origem_id = NEW.id;
END$$

DELIMITER ;

-- ────────────────────────────────────────────────────────────
-- 3. TRIGGERS — medula (doadores_medula)
--    (data_coleta da medula vira data_agendamento na tabela genérica)
-- ────────────────────────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_dm_agendamento_insert;
DROP TRIGGER IF EXISTS trg_dm_agendamento_update;

DELIMITER $$

CREATE TRIGGER trg_dm_agendamento_insert
AFTER INSERT ON doadores_medula
FOR EACH ROW
BEGIN
  INSERT INTO agendamentos
    (tipo_doacao, origem_id, doador_id, nome, email, telefone,
     unidade, data_agendamento, turno,
     status, confirmado_em, cancelado_em, cancelado_por, criado_em)
  VALUES
    ('medula', NEW.id, NEW.doador_id, NEW.nome, NEW.email, NEW.telefone,
     NEW.unidade, NEW.data_coleta, NEW.turno,
     NEW.status, NEW.confirmado_em, NEW.cancelado_em, NEW.cancelado_por, NEW.criado_em)
  ON DUPLICATE KEY UPDATE
    doador_id = NEW.doador_id, nome = NEW.nome, email = NEW.email,
    telefone = NEW.telefone, unidade = NEW.unidade,
    data_agendamento = NEW.data_coleta, turno = NEW.turno, status = NEW.status,
    confirmado_em = NEW.confirmado_em, cancelado_em = NEW.cancelado_em,
    cancelado_por = NEW.cancelado_por;
END$$

CREATE TRIGGER trg_dm_agendamento_update
AFTER UPDATE ON doadores_medula
FOR EACH ROW
BEGIN
  UPDATE agendamentos
  SET doador_id = NEW.doador_id,
      nome = NEW.nome,
      email = NEW.email,
      telefone = NEW.telefone,
      unidade = NEW.unidade,
      data_agendamento = NEW.data_coleta,
      turno = NEW.turno,
      status = NEW.status,
      confirmado_em = NEW.confirmado_em,
      cancelado_em = NEW.cancelado_em,
      cancelado_por = NEW.cancelado_por,
      atualizado_em = NOW()
  WHERE tipo_doacao = 'medula' AND origem_id = NEW.id;
END$$

DELIMITER ;

-- ────────────────────────────────────────────────────────────
-- 4. TRIGGERS — leite (doadora_leite)
-- ────────────────────────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_dl_agendamento_insert;
DROP TRIGGER IF EXISTS trg_dl_agendamento_update;

DELIMITER $$

CREATE TRIGGER trg_dl_agendamento_insert
AFTER INSERT ON doadora_leite
FOR EACH ROW
BEGIN
  INSERT INTO agendamentos
    (tipo_doacao, origem_id, doador_id, nome, email, telefone,
     unidade, data_agendamento, turno,
     status, confirmado_em, cancelado_em, cancelado_por, criado_em)
  VALUES
    ('leite', NEW.id, NEW.doador_id, NEW.nome, NEW.email, NEW.telefone,
     NEW.unidade, NEW.data_agendamento, NEW.turno,
     NEW.status, NEW.confirmado_em, NEW.cancelado_em, NEW.cancelado_por, NEW.criado_em)
  ON DUPLICATE KEY UPDATE
    doador_id = NEW.doador_id, nome = NEW.nome, email = NEW.email,
    telefone = NEW.telefone, unidade = NEW.unidade,
    data_agendamento = NEW.data_agendamento, turno = NEW.turno, status = NEW.status,
    confirmado_em = NEW.confirmado_em, cancelado_em = NEW.cancelado_em,
    cancelado_por = NEW.cancelado_por;
END$$

CREATE TRIGGER trg_dl_agendamento_update
AFTER UPDATE ON doadora_leite
FOR EACH ROW
BEGIN
  UPDATE agendamentos
  SET doador_id = NEW.doador_id,
      nome = NEW.nome,
      email = NEW.email,
      telefone = NEW.telefone,
      unidade = NEW.unidade,
      data_agendamento = NEW.data_agendamento,
      turno = NEW.turno,
      status = NEW.status,
      confirmado_em = NEW.confirmado_em,
      cancelado_em = NEW.cancelado_em,
      cancelado_por = NEW.cancelado_por,
      atualizado_em = NOW()
  WHERE tipo_doacao = 'leite' AND origem_id = NEW.id;
END$$

DELIMITER ;

-- ────────────────────────────────────────────────────────────
-- 5. BACKFILL — copia pra `agendamentos` o que já existia antes
--    de rodar este script (os triggers só pegam INSERT/UPDATE
--    que acontecerem DAQUI PRA FRENTE).
-- ────────────────────────────────────────────────────────────
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

INSERT INTO agendamentos
  (tipo_doacao, origem_id, doador_id, nome, email, telefone,
   unidade, data_agendamento, turno,
   status, confirmado_em, cancelado_em, cancelado_por, criado_em)
SELECT 'medula', id, doador_id, nome, email, telefone,
       unidade, data_coleta, turno,
       status, confirmado_em, cancelado_em, cancelado_por, criado_em
FROM doadores_medula
ON DUPLICATE KEY UPDATE
  doador_id = VALUES(doador_id), nome = VALUES(nome), email = VALUES(email),
  telefone = VALUES(telefone), unidade = VALUES(unidade),
  data_agendamento = VALUES(data_agendamento), turno = VALUES(turno),
  status = VALUES(status), confirmado_em = VALUES(confirmado_em),
  cancelado_em = VALUES(cancelado_em), cancelado_por = VALUES(cancelado_por);

INSERT INTO agendamentos
  (tipo_doacao, origem_id, doador_id, nome, email, telefone,
   unidade, data_agendamento, turno,
   status, confirmado_em, cancelado_em, cancelado_por, criado_em)
SELECT 'leite', id, doador_id, nome, email, telefone,
       unidade, data_agendamento, turno,
       status, confirmado_em, cancelado_em, cancelado_por, criado_em
FROM doadora_leite
ON DUPLICATE KEY UPDATE
  doador_id = VALUES(doador_id), nome = VALUES(nome), email = VALUES(email),
  telefone = VALUES(telefone), unidade = VALUES(unidade),
  data_agendamento = VALUES(data_agendamento), turno = VALUES(turno),
  status = VALUES(status), confirmado_em = VALUES(confirmado_em),
  cancelado_em = VALUES(cancelado_em), cancelado_por = VALUES(cancelado_por);

-- ────────────────────────────────────────────────────────────
-- 6. Conferência — todos os agendamentos, de qualquer tipo,
--    numa consulta só
-- ────────────────────────────────────────────────────────────
SELECT tipo_doacao, id, origem_id, nome, unidade, data_agendamento,
       turno, status, criado_em
FROM agendamentos
ORDER BY criado_em DESC;

-- Pronto. A partir de agora:
--  - A aplicação continua salvando normalmente em doadores_sangue,
--    doadores_medula e doadora_leite (nada mudou pra ela).
--  - Cada INSERT/UPDATE nessas 3 tabelas é replicado automaticamente
--    (pelos triggers) para `agendamentos`, que guarda os 3 tipos
--    juntos, de forma genérica, pra você consultar tudo num só lugar.
