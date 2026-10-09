package com.traderoperation.planos.application.service;

import com.traderoperation.planos.domain.Direito;
import com.traderoperation.planos.domain.Plano;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Única fonte das matrizes direito-por-plano e direito-por-perfil (D-15).
 * As chaves de perfil são o nome do enum {@code Perfil} da autenticação (como String,
 * para não acoplar módulos). Qualquer mudança nessa matriz entra aqui — não pode haver
 * {@code EnumSet<Direito>} de constante em nenhum outro ponto da aplicação.
 */
@Component
public class MatrizDeDireitos {

    private final Map<Plano, Set<Direito>> porPlano;
    private final Map<String, Set<Direito>> porPerfil;

    public MatrizDeDireitos() {
        this.porPlano = construirMatrizPorPlano();
        this.porPerfil = construirMatrizPorPerfil();
    }

    public Set<Direito> doPlano(Plano plano) {
        return porPlano.getOrDefault(plano, Set.of());
    }

    public Set<Direito> doPerfil(String perfil) {
        return porPerfil.getOrDefault(perfil, Set.of());
    }

    /** União de perfil + plano, pronta para virar authorities no filtro JWT. */
    public Set<Direito> paraUsuario(String perfil, Plano planoAtual) {
        EnumSet<Direito> todos = EnumSet.noneOf(Direito.class);
        todos.addAll(doPerfil(perfil));
        todos.addAll(doPlano(planoAtual));
        return Set.copyOf(todos);
    }

    private static Map<Plano, Set<Direito>> construirMatrizPorPlano() {
        EnumMap<Plano, Set<Direito>> matriz = new EnumMap<>(Plano.class);
        matriz.put(Plano.FREE, EnumSet.of(
                Direito.VER_OPERACAO_COM_ATRASO,
                Direito.LER_CHAT_OPERACAO));
        matriz.put(Plano.PRO, EnumSet.of(
                Direito.VER_OPERACAO_COM_ATRASO,
                Direito.LER_CHAT_OPERACAO,
                Direito.VER_OPERACAO_TEMPO_REAL,
                Direito.PARTICIPAR_CHAT_OPERACAO,
                Direito.USAR_COPILOTO_MT5,
                Direito.USAR_CHAT_IA));
        matriz.put(Plano.PREMIUM, EnumSet.of(
                Direito.VER_OPERACAO_COM_ATRASO,
                Direito.LER_CHAT_OPERACAO,
                Direito.VER_OPERACAO_TEMPO_REAL,
                Direito.PARTICIPAR_CHAT_OPERACAO,
                Direito.USAR_COPILOTO_MT5,
                Direito.USAR_CHAT_IA,
                Direito.PEDIR_BACKTEST_IA,
                Direito.PUBLICAR_NA_VITRINE));
        Map<Plano, Set<Direito>> imutavel = new EnumMap<>(Plano.class);
        matriz.forEach((p, direitos) -> imutavel.put(p, Set.copyOf(direitos)));
        return Map.copyOf(imutavel);
    }

    private static Map<String, Set<Direito>> construirMatrizPorPerfil() {
        Map<String, Set<Direito>> matriz = new HashMap<>();
        matriz.put("ADMIN", EnumSet.of(Direito.ADMINISTRAR_USUARIOS));
        matriz.put("MASTER", EnumSet.of(
                Direito.PUBLICAR_OPERACAO,
                Direito.APROVAR_ESTRATEGIA,
                Direito.VER_OPERACAO_TEMPO_REAL,
                Direito.USAR_CHAT_IA));
        matriz.put("CLIENTE", EnumSet.noneOf(Direito.class));
        Map<String, Set<Direito>> imutavel = new HashMap<>();
        matriz.forEach((p, direitos) -> imutavel.put(p, Set.copyOf(direitos)));
        return Map.copyOf(imutavel);
    }
}
