package com.fluxo.fluxovital.model;

public enum CategoriaInstituicao {

    BANCOS_DE_DOACAO("Bancos de Doação"),
    HOSPITAIS("Hospitais"),
    ONGS("ONGs"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaInstituicao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static CategoriaInstituicao fromDescricao(String descricao) {
        if (descricao == null) throw new IllegalArgumentException("Categoria nula");
        String valor = descricao.trim();
        for (CategoriaInstituicao c : values()) {
            // aceita "Bancos de Doação", "BANCOS_DE_DOACAO", ou qualquer variação
            if (c.descricao.equalsIgnoreCase(valor) || c.name().equalsIgnoreCase(valor)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Categoria inválida: " + descricao);
    }
}
