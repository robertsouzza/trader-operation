package com.traderoperation.planos.application.port.in;

import java.util.Set;
import java.util.UUID;

/**
 * Única porta para consultar os direitos efetivos de um usuário (perfil + plano ativo).
 * Devolve apenas os nomes dos direitos (strings) para não vazar o enum {@code Direito} do
 * domínio de planos para outros módulos. O filtro JWT da autenticação usa esses nomes como
 * {@code GrantedAuthority}.
 */
public interface VerificarDireitoUseCase {

    Set<String> direitosDoUsuario(UUID usuarioId);
}
