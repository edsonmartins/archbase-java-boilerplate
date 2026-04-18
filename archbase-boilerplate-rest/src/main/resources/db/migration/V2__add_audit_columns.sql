-- ============================================
-- ARCHBASE BOILERPLATE - MIGRATION V2
-- Adiciona colunas de auditoria extras se necessário
-- ============================================

-- Esta migration garante que as colunas de auditoria existam
-- Útil para novos projetos que podem ter esquemas diferentes

-- Verifica e adiciona colunas se não existirem (PostgreSQL)
DO $$
BEGIN
    -- Verifica coluna create_entity_date
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'produto' AND column_name = 'create_entity_date'
    ) THEN
        ALTER TABLE produto ADD COLUMN create_entity_date TIMESTAMP;
    END IF;

    -- Verifica coluna update_entity_date
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'produto' AND column_name = 'update_entity_date'
    ) THEN
        ALTER TABLE produto ADD COLUMN update_entity_date TIMESTAMP;
    END IF;
END $$;

-- Adiciona índice para buscas por data de criação
CREATE INDEX IF NOT EXISTS idx_produto_created ON produto(create_entity_date DESC);

-- Adiciona índice para buscas por data de atualização
CREATE INDEX IF NOT EXISTS idx_produto_updated ON produto(update_entity_date DESC);
