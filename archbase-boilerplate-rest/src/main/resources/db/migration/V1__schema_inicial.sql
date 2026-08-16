-- Schema inicial: domínio do boilerplate + segurança do Archbase.
--
-- POR QUE ESTA MIGRATION EXISTE
-- O perfil de produção sobe com hibernate.ddl-auto=validate, que é o certo: o Hibernate confere o
-- schema e recusa subir se algo não bate, em vez de alterar o banco sozinho. Só que validate exige
-- que TUDO já exista — inclusive as tabelas de segurança do framework. Sem esta migration, todo
-- projeto criado a partir deste boilerplate falhava no primeiro boot em produção com
--
--     Schema validation: missing table [seguranca]
--
-- e a mesma coisa acontecia a cada versão do Archbase que acrescentasse tabela ou coluna.
--
-- ESTE ARQUIVO É GERADO, NÃO ESCRITO À MÃO
-- Saiu de um pg_dump --schema-only de um banco criado pelo próprio mapeamento das entidades
-- (ddl-auto=create). Escrever à mão significaria divergir do mapeamento no primeiro campo novo.
-- Para regerar depois de mudar entidades: suba um PostgreSQL vazio, rode a aplicação uma vez com
-- ddl-auto=create e refaça o dump.
--
-- AS TABELAS _AUD NÃO ESTÃO AQUI, e isso é proposital: elas só existem quando a trilha de auditoria
-- é ligada (archbase.security.audit.enabled=true). Ligando a trilha, o DDL delas está em
-- deployment/sql/auditoria-seguranca-postgresql.sql, no repositório do framework.

CREATE TABLE produto (
    ativo boolean,
    destaque boolean,
    estoque integer,
    preco numeric(10,2),
    data_cadastro timestamp(6) without time zone,
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    versao bigint NOT NULL,
    categoria character varying(30),
    codigo character varying(40),
    id character varying(40) NOT NULL,
    tenant_id character varying(40) NOT NULL,
    marca character varying(100),
    sku character varying(100),
    nome character varying(200) NOT NULL,
    url_imagem character varying(500),
    descricao character varying(1000),
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255),
    CONSTRAINT produto_categoria_check CHECK (((categoria)::text = ANY ((ARRAY['ELETRONICOS'::character varying, 'MOVEIS'::character varying, 'ROUPAS'::character varying, 'VESTUARIO'::character varying, 'ALIMENTOS'::character varying, 'BEBIDAS'::character varying, 'LIMPEZA'::character varying, 'HIGIENE'::character varying, 'ESPORTES'::character varying, 'LIVROS'::character varying, 'OUTROS'::character varying])::text[])))
);

CREATE TABLE seguranca (
    bo_administrador character varying(1),
    bo_alterar_senha_proximo_login character varying(1),
    bo_conta_desativada character varying(1),
    bo_horario_livre character varying(1),
    bo_mfa_habilitado character varying(1),
    bo_permite_alterar_senha character varying(1),
    bo_permite_multiplicos_logins character varying(1),
    bo_senha_nunca_expira character varying(1),
    conta_bloqueada character varying(1),
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    dt_ultima_troca_senha timestamp(6) without time zone,
    versao bigint NOT NULL,
    access_level character varying(30),
    tp_seguranca character varying(31) NOT NULL,
    cd_seguranca character varying(40),
    horario_acesso_id character varying(40),
    id_seguranca character varying(40) NOT NULL,
    perfil_id character varying(40),
    tenant_id character varying(40) NOT NULL,
    employee_id character varying(60),
    mfa_recovery_codes character varying(2048),
    apelido character varying(255),
    descricao character varying(255) NOT NULL,
    email character varying(255),
    external_id character varying(255),
    mfa_secret character varying(255),
    nome character varying(255) NOT NULL,
    senha character varying(255),
    ultimo_usuario_alterou character varying(255),
    user_name character varying(255),
    usuario_criou character varying(255),
    avatar bytea,
    CONSTRAINT seguranca_access_level_check CHECK (((access_level)::text = ANY ((ARRAY['OPERATOR'::character varying, 'SUPERVISOR'::character varying, 'READER'::character varying, 'NONE'::character varying, 'TENANT_ADMIN'::character varying])::text[]))),
    CONSTRAINT seguranca_bo_administrador_check CHECK (((bo_administrador)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_bo_alterar_senha_proximo_login_check CHECK (((bo_alterar_senha_proximo_login)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_bo_conta_desativada_check CHECK (((bo_conta_desativada)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_bo_horario_livre_check CHECK (((bo_horario_livre)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_bo_mfa_habilitado_check CHECK (((bo_mfa_habilitado)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_bo_permite_alterar_senha_check CHECK (((bo_permite_alterar_senha)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_bo_permite_multiplicos_logins_check CHECK (((bo_permite_multiplicos_logins)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_bo_senha_nunca_expira_check CHECK (((bo_senha_nunca_expira)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_conta_bloqueada_check CHECK (((conta_bloqueada)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_tp_seguranca_check CHECK (((tp_seguranca)::text = ANY ((ARRAY['SEGURANCA_GRUPO'::character varying, 'SEGURANCA_PERFIL'::character varying, 'USUARIO'::character varying])::text[])))
);

CREATE TABLE seguranca_acao (
    bo_ativa character varying(1) NOT NULL,
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    versao bigint NOT NULL,
    minimum_level character varying(30),
    cd_acao character varying(40),
    id_acao character varying(40) NOT NULL,
    id_recurso character varying(40) NOT NULL,
    tenant_id character varying(40) NOT NULL,
    categoria character varying(255),
    descricao character varying(255) NOT NULL,
    nome character varying(255) NOT NULL,
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255),
    versao_acao character varying(255),
    CONSTRAINT seguranca_acao_bo_ativa_check CHECK (((bo_ativa)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_acao_minimum_level_check CHECK (((minimum_level)::text = ANY ((ARRAY['OPERATOR'::character varying, 'SUPERVISOR'::character varying, 'READER'::character varying, 'NONE'::character varying, 'TENANT_ADMIN'::character varying])::text[])))
);

CREATE TABLE seguranca_evento (
    bo_sucesso boolean NOT NULL,
    dh_evento timestamp(6) without time zone NOT NULL,
    id_evento character varying(40) NOT NULL,
    tp_evento character varying(40) NOT NULL,
    origem character varying(100),
    detalhe character varying(500),
    acao character varying(255),
    recurso character varying(255),
    tenant_id character varying(255),
    usuario character varying(255),
    CONSTRAINT seguranca_evento_tp_evento_check CHECK (((tp_evento)::text = ANY ((ARRAY['LOGIN'::character varying, 'LOGIN_FALHOU'::character varying, 'LOGOUT'::character varying, 'ACESSO_NEGADO'::character varying, 'SIMULACAO'::character varying])::text[])))
);

CREATE TABLE seguranca_grupo_usuario (
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    versao bigint NOT NULL,
    cd_usuario_grupo character varying(40),
    id_grupo character varying(40),
    id_usuario character varying(40),
    id_usuario_grupo character varying(40) NOT NULL,
    tenant_id character varying(40) NOT NULL,
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255)
);

CREATE TABLE seguranca_horario_acesso (
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    versao bigint NOT NULL,
    cd_horario_acesso character varying(40),
    id_horario_acesso character varying(40) NOT NULL,
    tenant_id character varying(40) NOT NULL,
    descricao character varying(255),
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255)
);

CREATE TABLE seguranca_intervalo_acesso (
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    dia_da_semana bigint,
    versao bigint NOT NULL,
    cd_intervalo_acesso character varying(40),
    id_horario_acesso character varying(40),
    id_intervalo_acesso character varying(40) NOT NULL,
    tenant_id character varying(40) NOT NULL,
    hora_final character varying(255),
    hora_inicial character varying(255),
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255)
);

CREATE TABLE seguranca_permissao (
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    versao bigint NOT NULL,
    effect character varying(10),
    cd_permissao character varying(40),
    id_acao character varying(40) NOT NULL,
    id_permissao character varying(40) NOT NULL,
    id_seguranca character varying(40) NOT NULL,
    tenant_id character varying(40) NOT NULL,
    company_id character varying(255),
    project_id character varying(255),
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255),
    CONSTRAINT seguranca_permissao_effect_check CHECK (((effect)::text = ANY ((ARRAY['GRANT'::character varying, 'DENY'::character varying])::text[])))
);

CREATE TABLE seguranca_recurso (
    bo_ativo character varying(1) NOT NULL,
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    versao bigint NOT NULL,
    cd_recurso character varying(40),
    id_recurso character varying(40) NOT NULL,
    tenant_id character varying(40) NOT NULL,
    tipo_recurso character varying(50),
    descricao character varying(255) NOT NULL,
    nome character varying(255) NOT NULL,
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255),
    CONSTRAINT seguranca_recurso_bo_ativo_check CHECK (((bo_ativo)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_recurso_tipo_recurso_check CHECK (((tipo_recurso)::text = ANY ((ARRAY['VIEW'::character varying, 'API'::character varying])::text[])))
);

CREATE TABLE seguranca_revisao (
    dh_revisao timestamp(6) without time zone,
    dh_revisao_milis bigint,
    id_revisao bigint NOT NULL,
    origem character varying(100),
    tenant_id character varying(255),
    usuario character varying(255)
);

CREATE SEQUENCE seguranca_revisao_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE seguranca_token_acesso (
    token_expirado character varying(1),
    token_revogado character varying(1),
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    dh_expiracao timestamp(6) without time zone,
    tempo_expiracao bigint,
    versao bigint NOT NULL,
    tp_uso_token character varying(20),
    cd_token character varying(40),
    id_token character varying(40) NOT NULL,
    id_usuario character varying(40),
    tenant_id character varying(40) NOT NULL,
    tp_token character varying(50),
    token character varying(5000),
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255),
    CONSTRAINT seguranca_token_acesso_token_expirado_check CHECK (((token_expirado)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_token_acesso_token_revogado_check CHECK (((token_revogado)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_token_acesso_tp_token_check CHECK (((tp_token)::text = 'BEARER'::text)),
    CONSTRAINT seguranca_token_acesso_tp_uso_token_check CHECK (((tp_uso_token)::text = ANY ((ARRAY['ACCESS'::character varying, 'REFRESH'::character varying])::text[])))
);

CREATE TABLE seguranca_token_api (
    bo_ativado character varying(1) NOT NULL,
    bo_revogado character varying(1) NOT NULL,
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    dh_expiracao timestamp(6) without time zone NOT NULL,
    versao bigint NOT NULL,
    cd_token_api character varying(40),
    id_seguranca character varying(40) NOT NULL,
    id_token_api character varying(40) NOT NULL,
    tenant_id character varying(40) NOT NULL,
    token_hash character varying(64),
    descricao character varying(255) NOT NULL,
    nome character varying(255) NOT NULL,
    token character varying(255),
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255),
    CONSTRAINT seguranca_token_api_bo_ativado_check CHECK (((bo_ativado)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_token_api_bo_revogado_check CHECK (((bo_revogado)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[])))
);

CREATE TABLE seguranca_token_redefinicao_senha (
    token_expirado character varying(1),
    token_revogado character varying(1),
    dh_atualizacao timestamp(6) without time zone,
    dh_criacao timestamp(6) without time zone,
    tempo_expiracao bigint,
    versao bigint NOT NULL,
    token character varying(13),
    cd_token character varying(40),
    id_token character varying(40) NOT NULL,
    id_usuario character varying(40),
    tenant_id character varying(40) NOT NULL,
    ultimo_usuario_alterou character varying(255),
    usuario_criou character varying(255),
    CONSTRAINT seguranca_token_redefinicao_senha_token_expirado_check CHECK (((token_expirado)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[]))),
    CONSTRAINT seguranca_token_redefinicao_senha_token_revogado_check CHECK (((token_revogado)::text = ANY ((ARRAY['N'::character varying, 'S'::character varying, 'N'::character varying])::text[])))
);

ALTER TABLE ONLY produto
    ADD CONSTRAINT produto_pkey PRIMARY KEY (id);

ALTER TABLE ONLY produto
    ADD CONSTRAINT produto_sku_key UNIQUE (sku);

ALTER TABLE ONLY seguranca_acao
    ADD CONSTRAINT seguranca_acao_pkey PRIMARY KEY (id_acao);

ALTER TABLE ONLY seguranca_evento
    ADD CONSTRAINT seguranca_evento_pkey PRIMARY KEY (id_evento);

ALTER TABLE ONLY seguranca
    ADD CONSTRAINT seguranca_external_id_key UNIQUE (external_id);

ALTER TABLE ONLY seguranca_grupo_usuario
    ADD CONSTRAINT seguranca_grupo_usuario_pkey PRIMARY KEY (id_usuario_grupo);

ALTER TABLE ONLY seguranca_horario_acesso
    ADD CONSTRAINT seguranca_horario_acesso_pkey PRIMARY KEY (id_horario_acesso);

ALTER TABLE ONLY seguranca_intervalo_acesso
    ADD CONSTRAINT seguranca_intervalo_acesso_pkey PRIMARY KEY (id_intervalo_acesso);

ALTER TABLE ONLY seguranca_permissao
    ADD CONSTRAINT seguranca_permissao_pkey PRIMARY KEY (id_permissao);

ALTER TABLE ONLY seguranca
    ADD CONSTRAINT seguranca_pkey PRIMARY KEY (id_seguranca);

ALTER TABLE ONLY seguranca_recurso
    ADD CONSTRAINT seguranca_recurso_pkey PRIMARY KEY (id_recurso);

ALTER TABLE ONLY seguranca_recurso
    ADD CONSTRAINT seguranca_recurso_tenant_id_nome_key UNIQUE (tenant_id, nome);

ALTER TABLE ONLY seguranca_revisao
    ADD CONSTRAINT seguranca_revisao_pkey PRIMARY KEY (id_revisao);

ALTER TABLE ONLY seguranca_token_acesso
    ADD CONSTRAINT seguranca_token_acesso_pkey PRIMARY KEY (id_token);

ALTER TABLE ONLY seguranca_token_acesso
    ADD CONSTRAINT seguranca_token_acesso_token_key UNIQUE (token);

ALTER TABLE ONLY seguranca_token_api
    ADD CONSTRAINT seguranca_token_api_pkey PRIMARY KEY (id_token_api);

ALTER TABLE ONLY seguranca_token_redefinicao_senha
    ADD CONSTRAINT seguranca_token_redefinicao_senha_pkey PRIMARY KEY (id_token);

ALTER TABLE ONLY seguranca_acao
    ADD CONSTRAINT uk_seguranca_acao_recurso_nome UNIQUE (tenant_id, id_recurso, nome);

CREATE INDEX idx_produto_ativo ON produto USING btree (ativo);

CREATE INDEX idx_produto_categoria ON produto USING btree (categoria);

CREATE INDEX idx_produto_nome ON produto USING btree (nome);

CREATE INDEX idx_seguranca_evento_dh ON seguranca_evento USING btree (dh_evento);

CREATE INDEX idx_seguranca_evento_tenant ON seguranca_evento USING btree (tenant_id);

CREATE INDEX idx_seguranca_evento_usuario ON seguranca_evento USING btree (usuario);

CREATE INDEX idx_seguranca_revisao_dh ON seguranca_revisao USING btree (dh_revisao);

CREATE INDEX idx_seguranca_revisao_tenant ON seguranca_revisao USING btree (tenant_id);

ALTER TABLE ONLY seguranca
    ADD CONSTRAINT fk4oml8jk4bfkw61k58qxuw0p3b FOREIGN KEY (perfil_id) REFERENCES seguranca(id_seguranca);

ALTER TABLE ONLY seguranca
    ADD CONSTRAINT fk8g6156ocqeh2fftyfph4jd8d FOREIGN KEY (horario_acesso_id) REFERENCES seguranca_horario_acesso(id_horario_acesso);

ALTER TABLE ONLY seguranca_acao
    ADD CONSTRAINT fk8kp4j3o8rqnh7amfhcpf4sxgn FOREIGN KEY (id_recurso) REFERENCES seguranca_recurso(id_recurso);

ALTER TABLE ONLY seguranca_permissao
    ADD CONSTRAINT fk8xjc2k2m41p6hp3iysmkvu9ps FOREIGN KEY (id_seguranca) REFERENCES seguranca(id_seguranca);

ALTER TABLE ONLY seguranca_grupo_usuario
    ADD CONSTRAINT fkab412nqm666nr4dmkfc4ddw39 FOREIGN KEY (id_usuario) REFERENCES seguranca(id_seguranca);

ALTER TABLE ONLY seguranca_grupo_usuario
    ADD CONSTRAINT fkc61phub73r5jbiacopvi6xj0r FOREIGN KEY (id_grupo) REFERENCES seguranca(id_seguranca);

ALTER TABLE ONLY seguranca_intervalo_acesso
    ADD CONSTRAINT fkfctj75tr55ei52nt2p4eaq87g FOREIGN KEY (id_horario_acesso) REFERENCES seguranca_horario_acesso(id_horario_acesso);

ALTER TABLE ONLY seguranca_token_api
    ADD CONSTRAINT fkh020hxe3hytpfmsb5p62hk39d FOREIGN KEY (id_seguranca) REFERENCES seguranca(id_seguranca);

ALTER TABLE ONLY seguranca_token_acesso
    ADD CONSTRAINT fkh9tyy0ny2wxi7s5oy0kir0byw FOREIGN KEY (id_usuario) REFERENCES seguranca(id_seguranca);

ALTER TABLE ONLY seguranca_token_redefinicao_senha
    ADD CONSTRAINT fkkpwqsytnq8nygyv4ckv7mnyjk FOREIGN KEY (id_usuario) REFERENCES seguranca(id_seguranca);

ALTER TABLE ONLY seguranca_permissao
    ADD CONSTRAINT fktr8rki232q9k1fdyav8n2d4qb FOREIGN KEY (id_acao) REFERENCES seguranca_acao(id_acao);
