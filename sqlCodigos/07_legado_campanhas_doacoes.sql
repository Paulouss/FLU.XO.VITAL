-- ============================================================
--  FLUXO VITAL — 07. Legado: CAMPANHAS e DOACOES_RECEBIDAS
--
--  ⚠️  Estas duas tabelas NÃO têm entidade Java correspondente
--      hoje (não existe model/Campanha.java nem
--      model/DoacaoRecebida.java, nem repository/controller
--      pra elas). A aplicação não lê nem escreve aqui.
--
--  Guardado só como referência, caso você queira implementar
--  essas funcionalidades no futuro (ex: instituição divulgar
--  campanhas, registrar doações já recebidas fisicamente).
--
--  Se for usar: precisa criar Campanha.java / DoacaoRecebida.java
--  com @Entity, os repositories e os endpoints no
--  InstituicaoController antes de essas tabelas fazerem algo.
--
--  Requer 05_schema_instituicao.sql e 01_schema_doador.sql
--  já executados.
-- ============================================================

USE fluxovital;

CREATE TABLE IF NOT EXISTS campanhas (
  id               BIGINT        AUTO_INCREMENT PRIMARY KEY,
  id_instituicao   BIGINT        NOT NULL,
  titulo           VARCHAR(200)  NOT NULL,
  descricao        TEXT          DEFAULT NULL,
  tipo_doacao      ENUM('sangue','medula','leite','todos') NOT NULL DEFAULT 'todos',
  data_inicio      DATE          NOT NULL,
  data_fim         DATE          DEFAULT NULL,
  status           ENUM('ativa','encerrada','suspensa') NOT NULL DEFAULT 'ativa',
  criado_em        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_campanha_instituicao
    FOREIGN KEY (id_instituicao) REFERENCES instituicoes(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS doacoes_recebidas (
  id               BIGINT     AUTO_INCREMENT PRIMARY KEY,
  id_doador        BIGINT     NOT NULL,
  id_instituicao   BIGINT     NOT NULL,
  tipo_doacao      ENUM('sangue','medula','leite') NOT NULL,
  data_doacao      DATE       NOT NULL,
  volume_ml        INT        DEFAULT NULL,
  tipo_sanguineo   VARCHAR(5) DEFAULT NULL,
  status           ENUM('recebida','processada','descartada') NOT NULL DEFAULT 'recebida',
  observacoes      TEXT       DEFAULT NULL,
  criado_em        DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_doacao_doador
    FOREIGN KEY (id_doador) REFERENCES doador(id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_doacao_instituicao
    FOREIGN KEY (id_instituicao) REFERENCES instituicoes(id)
    ON DELETE RESTRICT
);

-- ─── Consultas de referência (só funcionam se as tabelas acima
--     forem realmente usadas por alguma feature futura) ────────

SELECT c.id, i.nome AS instituicao, c.titulo,
       c.tipo_doacao, c.data_inicio, c.data_fim
FROM campanhas c
JOIN instituicoes i ON i.id = c.id_instituicao
WHERE c.status = 'ativa'
ORDER BY c.data_inicio;

SELECT dr.id, d.nome AS doador, dr.tipo_doacao,
       dr.data_doacao, dr.tipo_sanguineo, dr.volume_ml, dr.status
FROM doacoes_recebidas dr
JOIN doador d ON d.id = dr.id_doador
WHERE dr.id_instituicao = 1   -- trocar pelo id desejado
ORDER BY dr.data_doacao DESC;

SELECT i.nome AS instituicao, COUNT(dr.id) AS total_doacoes
FROM doacoes_recebidas dr
JOIN instituicoes i ON i.id = dr.id_instituicao
GROUP BY i.id, i.nome
ORDER BY total_doacoes DESC;
