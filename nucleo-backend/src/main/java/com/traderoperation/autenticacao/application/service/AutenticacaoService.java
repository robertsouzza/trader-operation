package com.traderoperation.autenticacao.application.service;

import com.traderoperation.auditoria.application.port.in.RegistrarAuditoriaUseCase;
import com.traderoperation.autenticacao.application.port.in.DadosUsuario;
import com.traderoperation.autenticacao.application.port.in.LoginUseCase;
import com.traderoperation.autenticacao.application.port.in.RenovarSessaoUseCase;
import com.traderoperation.autenticacao.application.port.in.SessaoEmitida;
import com.traderoperation.autenticacao.application.port.in.UsuarioAtualQuery;
import com.traderoperation.autenticacao.application.port.out.SenhaPort;
import com.traderoperation.autenticacao.application.port.out.TokenPort;
import com.traderoperation.autenticacao.application.port.out.UsuarioRepositoryPort;
import com.traderoperation.autenticacao.domain.Usuario;
import com.traderoperation.shared.error.NaoAutorizadoException;
import com.traderoperation.shared.error.RecursoNaoEncontradoException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AutenticacaoService implements LoginUseCase, RenovarSessaoUseCase, UsuarioAtualQuery {

    static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos.";
    static final String SESSAO_EXPIRADA = "Sessão expirada. Faça login novamente.";
    /** Hash BCrypt descartável, conferido quando o e-mail não existe, para o tempo de resposta não revelar isso. */
    private static final String HASH_FICTICIO = "$2b$10$q/9.RqGXu/joNgy20xJxYe8tAqbgiopHmy2yyleYP/qI6P/lIF9RO";

    private final UsuarioRepositoryPort usuarios;
    private final SenhaPort senhas;
    private final TokenPort tokens;
    private final RegistrarAuditoriaUseCase auditoria;

    public AutenticacaoService(UsuarioRepositoryPort usuarios, SenhaPort senhas, TokenPort tokens,
                               RegistrarAuditoriaUseCase auditoria) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
        this.auditoria = auditoria;
    }

    @Override
    public SessaoEmitida login(String email, String senha) {
        String emailNormalizado = normalizarOuNulo(email);
        Optional<Usuario> encontrado = Optional.ofNullable(emailNormalizado).flatMap(usuarios::buscarPorEmail);

        boolean senhaConfere = senhas.confere(senha, encontrado.map(Usuario::senhaHash).orElse(HASH_FICTICIO));
        if (encontrado.isEmpty() || !encontrado.get().ativo() || !senhaConfere) {
            // Mesma mensagem para e-mail inexistente e senha errada: não revela quais e-mails existem.
            auditoria.registrar(encontrado.map(Usuario::id).orElse(null), "LOGIN_FALHA", "usuario", null,
                    Map.of("email", String.valueOf(emailNormalizado)));
            throw new NaoAutorizadoException(CREDENCIAIS_INVALIDAS);
        }

        Usuario usuario = encontrado.get();
        auditoria.registrar(usuario.id(), "LOGIN", "usuario", usuario.id().toString(), Map.of());
        return new SessaoEmitida(dados(usuario), tokens.gerarAccess(usuario), tokens.gerarRefresh(usuario));
    }

    @Override
    public SessaoEmitida renovar(String refreshToken) {
        Usuario usuario = Optional.ofNullable(refreshToken)
                .flatMap(tokens::validarRefresh)
                .flatMap(usuarios::buscarPorId)
                .filter(Usuario::ativo)
                .orElseThrow(() -> new NaoAutorizadoException(SESSAO_EXPIRADA));
        return new SessaoEmitida(dados(usuario), tokens.gerarAccess(usuario), null);
    }

    @Override
    public DadosUsuario buscar(UUID id) {
        return usuarios.buscarPorId(id)
                .map(AutenticacaoService::dados)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private static DadosUsuario dados(Usuario u) {
        return new DadosUsuario(u.id(), u.nome(), u.email(), u.perfil().name());
    }

    private static String normalizarOuNulo(String email) {
        try {
            return Usuario.normalizarEmail(email);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
