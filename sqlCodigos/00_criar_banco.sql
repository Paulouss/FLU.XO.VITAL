-- ============================================================
--  FLUXO VITAL — 00. Criação do banco
--
--  Execute este arquivo primeiro, uma única vez.
--  Ordem completa de execução (banco novo, do zero):
--    00_criar_banco.sql
--    01_schema_doador.sql
--    02_schema_doacoes_sangue.sql
--    03_schema_doacoes_medula.sql
--    04_schema_doacoes_leite.sql
--    05_schema_instituicao.sql
--    06_gerenciamento_doacoes.sql   (opcional — só consultas prontas)
--    07_legado_campanhas_doacoes.sql (opcional — referência futura)
--    08_agendamento_generico.sql    (opcional — tabela `agendamentos`
--                                     genérica, alimentada por triggers,
--                                     sem tirar nada das 3 tabelas de tipo)
--    09_regras_validacao_formulario.sql (opcional — RG, idade, data
--                                     futura, CPF consistente e IMC
--                                     mínimo, validados no banco)
--
--  Se você já tem um banco criado com o schema antigo, NÃO rode
--  os arquivos 01-05 — use 99_migracao_schema_antigo.sql.
-- ============================================================

CREATE DATABASE IF NOT EXISTS fluxovital
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
