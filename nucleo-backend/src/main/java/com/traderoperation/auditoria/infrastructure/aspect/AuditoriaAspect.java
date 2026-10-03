package com.traderoperation.auditoria.infrastructure.aspect;

import com.traderoperation.auditoria.application.port.in.Auditavel;
import com.traderoperation.auditoria.application.port.in.RegistrarAuditoriaUseCase;
import com.traderoperation.shared.security.UsuarioAutenticado;
import java.util.Map;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Grava no diário toda chamada a um método anotado com {@link Auditavel}. */
@Aspect
@Component
class AuditoriaAspect {

    private final RegistrarAuditoriaUseCase auditoria;

    AuditoriaAspect(RegistrarAuditoriaUseCase auditoria) {
        this.auditoria = auditoria;
    }

    @Around("@annotation(auditavel)")
    Object auditar(ProceedingJoinPoint jp, Auditavel auditavel) throws Throwable {
        String metodo = jp.getSignature().getDeclaringType().getSimpleName() + "." + jp.getSignature().getName();
        String entidade = auditavel.entidade().isBlank() ? null : auditavel.entidade();
        try {
            Object resultado = jp.proceed();
            auditoria.registrar(usuarioAtual(), auditavel.acao(), entidade, null, Map.of("metodo", metodo));
            return resultado;
        } catch (Throwable erro) {
            auditoria.registrar(usuarioAtual(), auditavel.acao() + "_FALHA", entidade, null,
                    Map.of("metodo", metodo, "erro", erro.getClass().getSimpleName()));
            throw erro;
        }
    }

    private static UUID usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u ? u.id() : null;
    }
}
