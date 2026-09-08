-- ============================================================
--  FLUXO VITAL — 06. Gerenciamento de doações
--
--  Não cria nada — são consultas/exemplos prontos para a
--  instituição (ou você, direto no banco) acompanhar, confirmar
--  e cancelar agendamentos nas 3 tabelas de doação.
--
--  O fluxo real de confirmar/cancelar já roda pela aplicação
--  (DoadorController: /doador/agendamento/{tipo}/{id}/confirmar
--  e .../cancelar), que só permite:
--    confirmar : status atual = 'pendente'
--    cancelar  : status atual = 'pendente' ou 'confirmado'
--  Os UPDATEs abaixo replicam essa mesma regra manualmente.
-- ============================================================

USE fluxovital;

-- ─── PENDENTES POR TIPO ────────────────────────────────────────

-- Sangue
SELECT id, nome, email, telefone, tipo_sanguineo,
       data_agendamento, unidade, status, criado_em
FROM doadores_sangue
WHERE status = 'pendente'
ORDER BY criado_em DESC;

-- Medula
SELECT id, nome, email, telefone,
       data_coleta, unidade, status, criado_em
FROM doadores_medula
WHERE status = 'pendente'
ORDER BY criado_em DESC;

-- Leite
SELECT id, nome, email, telefone,
       data_parto, producao_estimada, status, criado_em
FROM doadora_leite
WHERE status = 'pendente'
ORDER BY criado_em DESC;

-- Todas as pendentes juntas, com o tipo identificado
SELECT 'sangue' AS tipo, id, nome, status, criado_em FROM doadores_sangue WHERE status = 'pendente'
UNION ALL
SELECT 'medula', id, nome, status, criado_em FROM doadores_medula WHERE status = 'pendente'
UNION ALL
SELECT 'leite',  id, nome, status, criado_em FROM doadora_leite   WHERE status = 'pendente'
ORDER BY criado_em DESC;

-- ─── CONFIRMAR (troque a tabela e o ID) ────────────────────────

UPDATE doadores_sangue
SET status = 'confirmado', confirmado_em = NOW()
WHERE id = 1 AND status = 'pendente';

UPDATE doadores_medula
SET status = 'confirmado', confirmado_em = NOW()
WHERE id = 1 AND status = 'pendente';

UPDATE doadora_leite
SET status = 'confirmado', confirmado_em = NOW()
WHERE id = 1 AND status = 'pendente';

-- ─── CANCELAR (troque a tabela e o ID) ─────────────────────────
-- cancelado_por: 'doador' | 'instituicao'

UPDATE doadores_sangue
SET status = 'cancelado', cancelado_em = NOW(), cancelado_por = 'instituicao'
WHERE id = 1 AND status IN ('pendente','confirmado');

UPDATE doadores_medula
SET status = 'cancelado', cancelado_em = NOW(), cancelado_por = 'instituicao'
WHERE id = 1 AND status IN ('pendente','confirmado');

UPDATE doadora_leite
SET status = 'cancelado', cancelado_em = NOW(), cancelado_por = 'instituicao'
WHERE id = 1 AND status IN ('pendente','confirmado');

-- ─── MARCAR COMO REALIZADO (após o comparecimento) ─────────────
-- Não existe endpoint pra isso na aplicação ainda — só via banco.

UPDATE doadores_sangue SET status = 'realizado' WHERE id = 1 AND status = 'confirmado';
UPDATE doadores_medula SET status = 'realizado' WHERE id = 1 AND status = 'confirmado';
UPDATE doadora_leite   SET status = 'realizado' WHERE id = 1 AND status = 'confirmado';

-- ─── HISTÓRICO DE UM DOADOR ESPECÍFICO (troque o ID) ───────────

SELECT 'sangue' AS tipo, id, nome, data_agendamento AS data_evento, status, criado_em
FROM doadores_sangue WHERE doador_id = 1
UNION ALL
SELECT 'medula', id, nome, data_coleta, status, criado_em
FROM doadores_medula WHERE doador_id = 1
UNION ALL
SELECT 'leite', id, nome, data_agendamento, status, criado_em
FROM doadora_leite WHERE doador_id = 1
ORDER BY criado_em DESC;

-- ─── CONTAGEM POR STATUS (visão geral rápida) ──────────────────

SELECT 'sangue' AS tipo, status, COUNT(*) AS total FROM doadores_sangue GROUP BY status
UNION ALL
SELECT 'medula', status, COUNT(*) FROM doadores_medula GROUP BY status
UNION ALL
SELECT 'leite',  status, COUNT(*) FROM doadora_leite   GROUP BY status
ORDER BY tipo, status;
