-- ============================================
-- ARCHBASE BOILERPLATE - MIGRATION V1
-- Criação da tabela de produtos
-- ============================================

CREATE TABLE IF NOT EXISTS produto (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(100),
    version BIGINT DEFAULT 0,
    create_entity_date TIMESTAMP,
    created_by_user VARCHAR(100),
    update_entity_date TIMESTAMP,
    last_modified_by_user VARCHAR(100),
    tenant_id VARCHAR(36),

    -- Campos de negócio
    nome VARCHAR(200) NOT NULL,
    descricao VARCHAR(1000),
    preco DECIMAL(10, 2),
    estoque INTEGER DEFAULT 0,
    categoria VARCHAR(30),
    ativo BOOLEAN DEFAULT TRUE,
    sku VARCHAR(100),
    data_cadastro TIMESTAMP,
    destaque BOOLEAN DEFAULT FALSE,
    url_imagem VARCHAR(500),
    marca VARCHAR(100)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_produto_nome ON produto(nome);
CREATE UNIQUE INDEX IF NOT EXISTS idx_produto_sku ON produto(sku) WHERE sku IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_produto_categoria ON produto(categoria);
CREATE INDEX IF NOT EXISTS idx_produto_ativo ON produto(ativo);
CREATE INDEX IF NOT EXISTS idx_produto_tenant ON produto(tenant_id);

-- Comentários
COMMENT ON TABLE produto IS 'Tabela de produtos do sistema';
COMMENT ON COLUMN produto.id IS 'Identificador único do produto (UUID)';
COMMENT ON COLUMN produto.nome IS 'Nome do produto';
COMMENT ON COLUMN produto.descricao IS 'Descrição detalhada do produto';
COMMENT ON COLUMN produto.preco IS 'Preço do produto';
COMMENT ON COLUMN produto.estoque IS 'Quantidade em estoque';
COMMENT ON COLUMN produto.categoria IS 'Categoria do produto';
COMMENT ON COLUMN produto.ativo IS 'Indica se o produto está ativo';
COMMENT ON COLUMN produto.sku IS 'Stock Keeping Unit - código único de identificação';
COMMENT ON COLUMN produto.data_cadastro IS 'Data de cadastro do produto';
COMMENT ON COLUMN produto.tenant_id IS 'ID do tenant para multi-tenancy';
