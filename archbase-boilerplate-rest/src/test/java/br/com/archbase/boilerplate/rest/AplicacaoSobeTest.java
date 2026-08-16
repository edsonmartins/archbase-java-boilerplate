package br.com.archbase.boilerplate.rest;

import br.com.archbase.boilerplate.core.application.dto.ProdutoCreateDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoDTO;
import br.com.archbase.boilerplate.core.application.dto.ProdutoUpdateDTO;
import br.com.archbase.boilerplate.core.application.service.ProdutoService;
import br.com.archbase.boilerplate.core.domain.enums.CategoriaProduto;
import br.com.archbase.security.password.ArchbasePasswordStrengthPolicy;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A aplicação sobe inteira e o CRUD de exemplo funciona de ponta a ponta.
 *
 * <p><b>Por que este teste existe.</b> Este boilerplate é o ponto de partida de projetos novos, e
 * não tinha nenhum {@code @SpringBootTest}: os testes eram unitários, com o repositório mockado, e
 * nenhum subia o contexto nem encostava num banco. O efeito é que o CRUD de exemplo — a primeira
 * coisa que alguém copia ao começar um projeto — estava quebrado em três pontos, todos invisíveis
 * para o build:
 *
 * <ul>
 *   <li>criar produto falhava sempre, com "Identifier of entity ... must be manually assigned
 *       before calling 'persist()'": o {@code @Builder} da entidade repassa o id ao construtor
 *       completo da base, e só o construtor SEM argumentos do archbase gera UUID;</li>
 *   <li>{@code createEntityDate} ficava nulo em todo registro criado, pelo mesmo motivo;</li>
 *   <li>{@code updateEntityDate} ficava nulo em toda alteração — as datas do archbase são
 *       preenchidas por construtor, não por {@code @LastModifiedDate}, então quem atualiza precisa
 *       marcá-las.</li>
 * </ul>
 *
 * <p>Nenhum desses defeitos quebrava a compilação nem os testes unitários; todos apareciam na
 * primeira chamada real à API. Por isso o teste exercita o CRUD, e não apenas verifica que o
 * contexto sobe: um teste que só liga o contexto teria deixado os três passarem.
 *
 * <p><b>Sem banco, ele se pula em vez de falhar.</b> Para rodar:
 *
 * <pre>
 * docker compose up -d postgres
 * mvn test -Dtest=AplicacaoSobeTest
 * </pre>
 *
 * <p>Se o Postgres estiver em outra porta, informe {@code -Dboilerplate.test.postgres.port=...}.
 */
@SpringBootTest(classes = ArchbaseBoilerplateApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AplicacaoSobeTest {

    private static final String HOST = System.getProperty("boilerplate.test.postgres.host",
            System.getenv().getOrDefault("POSTGRES_HOST", "localhost"));
    private static final String PORTA = System.getProperty("boilerplate.test.postgres.port",
            System.getenv().getOrDefault("POSTGRES_PORT", "5432"));
    private static final String BANCO = System.getProperty("boilerplate.test.postgres.db",
            System.getenv().getOrDefault("POSTGRES_DATABASE", "archbase_db"));
    private static final String USUARIO = System.getProperty("boilerplate.test.postgres.user",
            System.getenv().getOrDefault("POSTGRES_USER", "archbase"));
    private static final String SENHA = System.getProperty("boilerplate.test.postgres.password",
            System.getenv().getOrDefault("POSTGRES_PASSWORD", "changeit"));

    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PORTA + "/" + BANCO;

    @BeforeAll
    static void exigirBanco() {
        // Antes de o Spring tentar subir: sem banco, pular com um motivo legível. Deixar o contexto
        // falhar produziria um erro de conexão que esconde a única informação útil aqui.
        assumeTrue(bancoDisponivel(),
                () -> "PostgreSQL indisponível em " + URL + " — teste pulado. "
                        + "Suba com `docker compose up -d postgres`.");
    }

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> URL);
        registry.add("spring.datasource.username", () -> USUARIO);
        registry.add("spring.datasource.password", () -> SENHA);

        // A aplicação recusa-se a subir com o segredo de exemplo, que não é Base64 válido — a
        // validação é proposital. Este é um segredo de teste, sem valor fora daqui.
        registry.add("archbase.security.jwt.secret-key",
                () -> "dGVzdGVCb2lsZXJwbGF0ZUFwbGljYWNhb1NvYmVUZXN0ZTEyMzQ1Njc4OTBhYmNkZWY=");
    }

    @Autowired
    ApplicationContext contexto;
    @Autowired
    ProdutoService produtoService;
    @Autowired
    ArchbasePasswordStrengthPolicy politicaDeSenha;
    @LocalServerPort
    int porta;

    @Test
    @DisplayName("o contexto sobe inteiro, com Flyway e o banco real")
    void contextoSobe() {
        assertThat(contexto).isNotNull();
        assertThat(contexto.getBean(EntityManagerFactory.class)).isNotNull();
    }

    @Test
    @DisplayName("criar preenche id e data de criação, e o produto é recuperável")
    void criarFuncionaEPreencheAuditoria() {
        ProdutoCreateDTO novo = new ProdutoCreateDTO();
        novo.setNome("Produto do teste de subida");
        novo.setSku("TESTE-" + UUID.randomUUID());
        novo.setPreco(new BigDecimal("19.90"));
        novo.setEstoque(7);
        novo.setCategoria(CategoriaProduto.ELETRONICOS);

        ProdutoDTO criado = produtoService.criar(novo);

        // O id é o que faltava: sem ele, save() ia direto para "Identifier of entity ... must be
        // manually assigned before calling 'persist()'" e nenhum produto era criado.
        assertThat(criado.getId()).as("id gerado").isNotBlank();
        assertThat(criado.getCreateEntityDate()).as("data de criação").isNotNull();

        ProdutoDTO lido = produtoService.buscarPorId(criado.getId());
        assertThat(lido.getNome()).isEqualTo("Produto do teste de subida");
        assertThat(lido.getEstoque()).isEqualTo(7);
    }

    @Test
    @DisplayName("atualizar registra a data de alteração")
    void atualizarPreencheDataDeAlteracao() {
        ProdutoCreateDTO novo = new ProdutoCreateDTO();
        novo.setNome("Produto a alterar");
        novo.setSku("TESTE-" + UUID.randomUUID());
        novo.setPreco(new BigDecimal("10.00"));
        novo.setEstoque(1);
        novo.setCategoria(CategoriaProduto.LIVROS);
        ProdutoDTO criado = produtoService.criar(novo);

        ProdutoUpdateDTO alteracao = new ProdutoUpdateDTO();
        alteracao.setNome("Produto alterado");
        alteracao.setPreco(new BigDecimal("12.00"));

        ProdutoDTO alterado = produtoService.atualizar(criado.getId(), alteracao);

        assertThat(alterado.getNome()).isEqualTo("Produto alterado");
        // As datas do archbase não vêm de @LastModifiedDate; quem altera precisa marcá-las. Sem
        // isso, todo registro alterado ficava com a data de alteração vazia.
        assertThat(alterado.getUpdateEntityDate()).as("data de alteração").isNotNull();
    }

    @Test
    @DisplayName("a política de força de senha está ligada e recusa senha fraca")
    void politicaDeSenhaEstaAtiva() {
        // Os defaults do framework vêm todos desligados (min-length=0): a política só existe porque
        // o application.yml a define. Este caso existe para que apagar aquelas chaves apareça como
        // teste vermelho, e não como uma proteção que silenciosamente parou de valer.
        assertThat(politicaDeSenha.isEnabled())
                .as("archbase.security.password.* configurado")
                .isTrue();

        // Curta, sem maiúscula, sem dígito e sem caractere especial — reprovada em quatro regras.
        assertThatThrownBy(() -> politicaDeSenha.validate("senha"))
                .isInstanceOf(ArchbaseValidationException.class);

        // "Password1!" tem 10 caracteres e atende ao resto: quem barra é o mínimo de 12.
        assertThatThrownBy(() -> politicaDeSenha.validate("Password1!"))
                .as("mínimo de 12 caracteres")
                .isInstanceOf(ArchbaseValidationException.class);

        // Atende a todas as regras e passa sem lançar.
        politicaDeSenha.validate("Boilerplate#2026Seguro");
    }

    @Test
    @DisplayName("a recuperação de senha responde igual para e-mail existente e inexistente")
    void naoPermiteEnumerarUsuarios() {
        // Com prevent-user-enumeration desligado, a diferença entre "enviado" e "usuário não
        // encontrado" diz a quem perguntar se aquele e-mail tem conta — de graça, sem autenticação.
        // As duas respostas precisam ser indistinguíveis.
        HttpResponse<String> existente =
                pedirResetDeSenha("admin@archbase.com.br");
        HttpResponse<String> inexistente =
                pedirResetDeSenha("nao-existe-" + UUID.randomUUID() + "@exemplo.com");

        assertThat(inexistente.statusCode())
                .as("mesmo status para e-mail que existe e e-mail que não existe")
                .isEqualTo(existente.statusCode());
        assertThat(inexistente.body())
                .as("mesmo corpo de resposta")
                .isEqualTo(existente.body());
    }

    /**
     * Chama o endpoint de recuperação de senha pelo HTTP real da aplicação.
     *
     * <p>Com o {@code HttpClient} do JDK, e não com {@code TestRestTemplate}: este último saiu do
     * Spring Boot 4 junto da virada para {@code RestClient}. O cliente do JDK não depende de API de
     * framework e não muda de lugar entre versões.
     */
    private HttpResponse<String> pedirResetDeSenha(String email) {
        try (HttpClient cliente = HttpClient.newHttpClient()) {
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + porta
                            + "/api/v1/auth/sendResetPasswordEmail/" + email))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("falha ao chamar o endpoint de reset de senha", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrompido ao chamar o endpoint de reset de senha", e);
        }
    }

    private static boolean bancoDisponivel() {
        try (Connection ignored = DriverManager.getConnection(URL, USUARIO, SENHA)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
