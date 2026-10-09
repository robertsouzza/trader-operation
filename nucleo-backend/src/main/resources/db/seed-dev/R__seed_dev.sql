-- ============================================================
-- DADOS SINTÉTICOS — SOMENTE DESENVOLVIMENTO E TESTE.
-- Carregado só nos profiles dev, docker e test. Nunca em produção.
-- Senha de todos: trader123 (hash BCrypt abaixo).
-- ============================================================
INSERT INTO usuarios (id, nome, email, senha_hash, perfil)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'Admin Dev',   'admin@trader.local',
     '$2b$10$q/9.RqGXu/joNgy20xJxYe8tAqbgiopHmy2yyleYP/qI6P/lIF9RO', 'ADMIN'),
    ('00000000-0000-0000-0000-000000000002', 'Master Dev',  'master@trader.local',
     '$2b$10$q/9.RqGXu/joNgy20xJxYe8tAqbgiopHmy2yyleYP/qI6P/lIF9RO', 'MASTER'),
    ('00000000-0000-0000-0000-000000000003', 'Cliente Dev', 'cliente@trader.local',
     '$2b$10$q/9.RqGXu/joNgy20xJxYe8tAqbgiopHmy2yyleYP/qI6P/lIF9RO', 'CLIENTE')
ON CONFLICT (email) DO NOTHING;

-- Cliente Dev nasce com assinatura PRO manual de 30 dias (skill 02).
-- Admin e Master não precisam de assinatura: direitos vêm do perfil.
INSERT INTO assinaturas (id, usuario_id, plano, status, origem, inicio_em, fim_em)
VALUES
    ('00000000-0000-0000-0000-00000000a001',
     '00000000-0000-0000-0000-000000000003',
     'PRO', 'ATIVA', 'MANUAL', now(), now() + interval '30 days')
ON CONFLICT (id) DO NOTHING;
