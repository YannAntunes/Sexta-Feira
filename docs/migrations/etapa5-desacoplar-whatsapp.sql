-- ===================================================================
-- MIGRAÇÃO DE DESACOPLAMENTO DO WHATSAPP E NOVA IDENTIDADE (FASE 2)
-- Arquivo: docs/migrations/etapa5-desacoplar-whatsapp.sql
-- ===================================================================

USE sexta_feira_db;

-- 1. Alteração da tabela de usuários para identidade independente
ALTER TABLE usuarios 
  ADD COLUMN IF NOT EXISTS email VARCHAR(150) NULL UNIQUE AFTER id,
  ADD COLUMN IF NOT EXISTS nome VARCHAR(100) NULL AFTER email,
  ADD COLUMN IF NOT EXISTS senha_hash VARCHAR(255) NULL AFTER nome,
  ADD COLUMN IF NOT EXISTS idioma_preferido VARCHAR(10) NOT NULL DEFAULT 'pt-BR' AFTER personalidade,
  ADD COLUMN IF NOT EXISTS atualizado_em DATETIME NULL AFTER criado_em,
  MODIFY COLUMN whatsapp_number VARCHAR(50) NULL;

-- 2. Backfill de usuários existentes:
-- Garante que nenhum usuário fique sem e-mail ou nome, preservando chaves estrangeiras
UPDATE usuarios 
SET 
  email = COALESCE(email, CONCAT('usuario_', id, '@sextafeira.local')),
  nome = COALESCE(nome, CONCAT('Usuário ', id)),
  idioma_preferido = COALESCE(idioma_preferido, 'pt-BR')
WHERE email IS NULL;

-- 3. Criação da tabela de notificações internas do usuário
CREATE TABLE IF NOT EXISTS notificacoes_usuario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  titulo VARCHAR(120) NOT NULL,
  mensagem VARCHAR(1000) NOT NULL,
  tipo VARCHAR(50) NOT NULL,
  lida BOOLEAN NOT NULL DEFAULT FALSE,
  criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notificacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
  INDEX idx_notificacao_usuario_lida (usuario_id, lida, criado_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Opcional: Remoção definitiva da coluna legada whatsapp_number
-- Executar apenas após validação de que nenhum cliente legado utiliza o campo:
-- ALTER TABLE usuarios DROP INDEX IF EXISTS whatsapp_number;
-- ALTER TABLE usuarios DROP COLUMN IF EXISTS whatsapp_number;
