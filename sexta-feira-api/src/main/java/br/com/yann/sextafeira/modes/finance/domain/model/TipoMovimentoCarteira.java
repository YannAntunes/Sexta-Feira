package br.com.yann.sextafeira.modes.finance.domain.model;

public enum TipoMovimentoCarteira {
    COMPRA,        // compra por quantidade
    VENDA,         // venda por quantidade
    APORTE_BRL     // aporte em reais (converte para quantidade via cotação)
}
