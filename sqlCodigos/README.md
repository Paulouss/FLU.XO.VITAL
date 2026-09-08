# SQL do Fluxo Vital

## Banco NOVO (do zero)

Execute nesta ordem:

1. `00_criar_banco.sql`
2. `01_schema_doador.sql`
3. `02_schema_doacoes_sangue.sql`
4. `03_schema_doacoes_medula.sql`
5. `04_schema_doacoes_leite.sql`
6. `05_schema_instituicao.sql`
7. `06_gerenciamento_doacoes.sql` — opcional, só consultas prontas de confirmar/cancelar/histórico (edite os IDs antes de rodar)
8. `07_legado_campanhas_doacoes.sql` — opcional, tabelas sem entidade Java ainda (guardado pra uma feature futura)
9. `08_agendamento_generico.sql` — opcional, cria a tabela `agendamentos` (genérica, guarda sangue/medula/leite juntos) alimentada por **triggers** nas 3 tabelas de tipo. Não muda nada nelas nem na aplicação — elas continuam recebendo e salvando o agendamento exatamente como hoje; o banco só replica automaticamente cada INSERT/UPDATE pra essa tabela geral.
10. `09_regras_validacao_formulario.sql` — opcional, adiciona regras direto no banco: RG com 12 dígitos, idade entre 16-69, data de agendamento/coleta no futuro, CPF não pode ter nome/nascimento diferentes em outro formulário, última doação não pode ser antes do nascimento, e calcula IMC (+ se ficou abaixo do mínimo) em sangue/medula. Também tem no topo um diagnóstico pra descobrir por que um agendamento não apareceu na tabela `agendamentos`.

## Banco JÁ EXISTENTE (schema antigo, ou deu erro "Data truncated for column 'status'")

Use só:

- `99_migracao_schema_antigo.sql`

Rode o arquivo inteiro de uma vez no MySQL Workbench (ou `mysql -u root -p fluxovital < 99_migracao_schema_antigo.sql` no terminal). Ele funciona em qualquer versão do MySQL 5.7+/8.x — não depende de sintaxe que só existe em versões recentes, então não deve dar erro de sintaxe. No final ele mostra 3 `SHOW COLUMNS` pra você conferir que o `status` ficou correto.

Depois, se quiser instituições/campanhas num banco migrado, rode `05_schema_instituicao.sql` e, opcionalmente, `07_legado_campanhas_doacoes.sql`.

## O que cada arquivo cria

| Arquivo | Tabela(s) | Entidade Java |
|---|---|---|
| 01 | `doador` | `Doador.java` |
| 02 | `doadores_sangue` | `DoadorSangue.java` |
| 03 | `doadores_medula` | `DoadorMedula.java` |
| 04 | `doadora_leite` | `DoadoraLeite.java` |
| 05 | `instituicoes` | `Instituicao.java` |
| 07 | `campanhas`, `doacoes_recebidas` | **nenhuma** — legado/referência |
| 08 | `agendamentos` (genérica: sangue+medula+leite) | **nenhuma** — alimentada por triggers, não por código Java |
| 09 | validações em `doador`, `doadores_sangue`, `doadores_medula`, `doadora_leite` | **nenhuma** — CHECKs e triggers, não muda os models Java |
