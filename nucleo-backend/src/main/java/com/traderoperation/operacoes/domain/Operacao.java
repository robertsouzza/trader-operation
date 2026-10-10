package com.traderoperation.operacoes.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Operação publicada pelo master (D-02). Imutável — toda transição de estado
 * cria uma nova instância; o repositório grava a nova.
 */
public record Operacao(
        UUID id,
        String ativo,
        Direcao direcao,
        BigDecimal entrada,
        BigDecimal stop,
        List<BigDecimal> alvos,
        String estrategia,
        StatusOperacao status,
        ResultadoOperacao resultado,
        String observacao,
        UUID masterId,
        Instant publicadaEm,
        Instant encerradaEm) {

    public Operacao {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ativo, "ativo");
        Objects.requireNonNull(direcao, "direcao");
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(stop, "stop");
        Objects.requireNonNull(alvos, "alvos");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(masterId, "masterId");
        Objects.requireNonNull(publicadaEm, "publicadaEm");
        if (ativo.isBlank()) {
            throw new IllegalArgumentException("ativo não pode ser vazio.");
        }
        if (alvos.isEmpty()) {
            throw new IllegalArgumentException("É preciso informar ao menos um alvo.");
        }
        if (entrada.signum() <= 0 || stop.signum() <= 0) {
            throw new IllegalArgumentException("Entrada e stop precisam ser positivos.");
        }
        if (alvos.stream().anyMatch(a -> a.signum() <= 0)) {
            throw new IllegalArgumentException("Alvos precisam ser positivos.");
        }
        validarCoerencia(direcao, entrada, stop, alvos);
        alvos = List.copyOf(alvos);
    }

    public static Operacao publicar(
            UUID id,
            String ativo,
            Direcao direcao,
            BigDecimal entrada,
            BigDecimal stop,
            List<BigDecimal> alvos,
            String estrategia,
            UUID masterId,
            Instant quando) {
        return new Operacao(
                id, ativo, direcao, entrada, stop, alvos, estrategia,
                StatusOperacao.PUBLICADA, null, null,
                masterId, quando, null);
    }

    public Operacao encerrar(ResultadoOperacao resultadoFinal, String observacaoFinal, Instant quando) {
        if (status == StatusOperacao.ENCERRADA) {
            throw new IllegalStateException("Operação já está encerrada.");
        }
        Objects.requireNonNull(resultadoFinal, "resultadoFinal");
        Objects.requireNonNull(quando, "quando");
        return new Operacao(
                id, ativo, direcao, entrada, stop, alvos, estrategia,
                StatusOperacao.ENCERRADA, resultadoFinal, observacaoFinal,
                masterId, publicadaEm, quando);
    }

    private static void validarCoerencia(
            Direcao direcao, BigDecimal entrada, BigDecimal stop, List<BigDecimal> alvos) {
        if (direcao == Direcao.COMPRA) {
            if (stop.compareTo(entrada) >= 0) {
                throw new IllegalArgumentException("Em COMPRA, stop precisa ser menor que a entrada.");
            }
            if (alvos.stream().anyMatch(a -> a.compareTo(entrada) <= 0)) {
                throw new IllegalArgumentException("Em COMPRA, cada alvo precisa ser maior que a entrada.");
            }
        } else {
            if (stop.compareTo(entrada) <= 0) {
                throw new IllegalArgumentException("Em VENDA, stop precisa ser maior que a entrada.");
            }
            if (alvos.stream().anyMatch(a -> a.compareTo(entrada) >= 0)) {
                throw new IllegalArgumentException("Em VENDA, cada alvo precisa ser menor que a entrada.");
            }
        }
    }
}
