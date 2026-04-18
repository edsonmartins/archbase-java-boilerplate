package br.com.archbase.boilerplate.core.domain.enums;

import lombok.Getter;

/**
 * Enumeração de categorias de produto.
 */
@Getter
public enum CategoriaProduto {

    ELETRONICOS("Eletrônicos"),
    MOVEIS("Móveis"),
    ROUPAS("Roupas"),
    VESTUARIO("Vestuário"),
    ALIMENTOS("Alimentos"),
    BEBIDAS("Bebidas"),
    LIMPEZA("Limpeza"),
    HIGIENE("Higiene"),
    ESPORTES("Esportes"),
    LIVROS("Livros"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaProduto(String descricao) {
        this.descricao = descricao;
    }
}
