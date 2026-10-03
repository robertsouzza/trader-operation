package com.traderoperation.auditoria.application.port.in;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um caso de uso para ser registrado no diário de auditoria.
 * Sucesso grava {@code acao}; exceção grava {@code acao + "_FALHA"}. Argumentos não são gravados,
 * para nunca vazar senha ou segredo no diário.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditavel {

    String acao();

    String entidade() default "";
}
