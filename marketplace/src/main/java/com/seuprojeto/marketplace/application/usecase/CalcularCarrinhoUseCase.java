package com.seuprojeto.marketplace.application.usecase;

import com.seuprojeto.marketplace.application.dto.SelecaoCarrinho;
import com.seuprojeto.marketplace.domain.model.CategoriaProduto;
import com.seuprojeto.marketplace.domain.model.ItemCarrinho;
import com.seuprojeto.marketplace.domain.model.Produto;
import com.seuprojeto.marketplace.domain.model.ResumoCarrinho;
import com.seuprojeto.marketplace.domain.repository.ProdutoRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class CalcularCarrinhoUseCase {

    private final ProdutoRepositorio produtoRepositorio;

    public CalcularCarrinhoUseCase(ProdutoRepositorio produtoRepositorio) {
        this.produtoRepositorio = produtoRepositorio;
    }

    public ResumoCarrinho executar(List<SelecaoCarrinho> selecaoCarrinhos) {
        // 1. Montar itens do carrinho
        List<ItemCarrinho> itens = new ArrayList<>();
        for (SelecaoCarrinho selecao : selecaoCarrinhos) {
            if (selecao.getQuantidade() == null || selecao.getQuantidade() <= 0) continue;
            Produto produto = produtoRepositorio.findById(selecao.getIdProduto())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + selecao.getIdProduto()));
            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // 2. Calcular subtotal e quantidade total de itens
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItens = 0;
        for (ItemCarrinho item : itens) {
            BigDecimal valorItem = item.getProduto().getPreco()
                    .multiply(BigDecimal.valueOf(item.getQuantidade()));
            subtotal = subtotal.add(valorItem);
            totalItens += item.getQuantidade();
        }

        // 3. Desconto por quantidade total de itens
        int percentualQuantidade;
        if (totalItens >= 4) {
            percentualQuantidade = 10;
        } else if (totalItens == 3) {
            percentualQuantidade = 7;
        } else if (totalItens == 2) {
            percentualQuantidade = 5;
        } else {
            percentualQuantidade = 0;
        }

        // 4. Desconto por categoria (por item, não por categoria única)
        int percentualCategoria = 0;
        for (ItemCarrinho item : itens) {
            int descontoPorItem = descontoCategoria(item.getProduto().getCategoriaProduto());
            percentualCategoria += descontoPorItem * item.getQuantidade();
        }

        // 5. Somar descontos e aplicar limite máximo de 25%
        int percentualTotal = percentualQuantidade + percentualCategoria;
        if (percentualTotal > 25) {
            percentualTotal = 25;
        }

        // 6. Calcular valor do desconto e total final
        BigDecimal valorDesconto = subtotal
                .multiply(BigDecimal.valueOf(percentualTotal))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new ResumoCarrinho(subtotal, valorDesconto);
    }

    private int descontoCategoria(CategoriaProduto categoria) {
        return switch (categoria) {
            case CAPINHA -> 3;
            case CARREGADOR -> 5;
            case FONE -> 3;
            case PELICULA -> 2;
            case SUPORTE -> 2;
        };
    }
}