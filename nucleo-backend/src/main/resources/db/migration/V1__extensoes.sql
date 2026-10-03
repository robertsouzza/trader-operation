-- Garante a extensão também fora do Docker de dev (o init do compose já a cria).
-- pgvector guarda a memória/conhecimento da IA (D-08).
CREATE EXTENSION IF NOT EXISTS vector;
