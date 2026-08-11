package br.com.archbase.boilerplate.rest.seed;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Cria o primeiro administrador, para que a aplicação recém-instalada tenha por onde entrar.
 *
 * <p><b>O problema que resolve.</b> Todas as telas exigem autenticação, e o cadastro de usuários é
 * uma delas. Num banco novo isso é um impasse: não há como criar o primeiro usuário sem estar
 * autenticado, e não há como se autenticar sem o primeiro usuário. Quem clonava o boilerplate
 * precisava descobrir isso sozinho e inserir uma linha na mão, com o hash bcrypt já calculado.
 *
 * <p><b>Só age em banco sem nenhum usuário.</b> A verificação não é "existe um admin?", e sim
 * "existe alguém?" — assim ele nunca recria uma conta que alguém apagou de propósito, nem
 * ressuscita um administrador removido. Depois do primeiro usuário existir, este runner não faz mais
 * nada, para sempre.
 *
 * <p><b>A senha muda conforme o ambiente</b>, e isso é deliberado:
 *
 * <ul>
 *   <li>Com {@code archbase.boilerplate.seed.admin.password} definida, usa o que foi configurado.
 *   <li>Sem ela, em <b>desenvolvimento</b>, usa {@code admin} — combinando com a dica que a tela de
 *       login mostra, para quem está começando não precisar procurar.
 *   <li>Sem ela, <b>fora de desenvolvimento</b>, gera uma senha aleatória e a escreve no log
 *       <b>uma única vez</b>. Um padrão conhecido em produção é uma porta aberta que ninguém lembra
 *       de fechar; uma senha aleatória obriga a olhar o log e trocá-la.
 * </ul>
 *
 * <p>Para não criar nada: {@code archbase.boilerplate.seed.admin.enabled=false}.
 */
@Component
@Order(50) // antes do DataSeedLoader: sem usuário, o resto do seed não tem contexto de autoria
@RequiredArgsConstructor
@Slf4j
public class AdminSeedLoader implements CommandLineRunner {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${archbase.boilerplate.seed.admin.enabled:true}")
    private boolean habilitado;

    @Value("${archbase.boilerplate.seed.admin.email:admin@archbase.com.br}")
    private String email;

    /** Vazio = decide pelo ambiente (ver javadoc da classe). */
    @Value("${archbase.boilerplate.seed.admin.password:}")
    private String senhaConfigurada;

    @Value("${spring.profiles.active:default}")
    private String perfisAtivos;

    @Value("${archbase.app.tenant.default.id:}")
    private String tenantPadrao;

    @Override
    public void run(String... args) {
        if (!habilitado) {
            return;
        }
        // "Existe alguém?", e não "existe um admin?": ver o javadoc.
        if (userRepository.count() > 0) {
            return;
        }

        boolean desenvolvimento = perfisAtivos.contains("dev") || perfisAtivos.contains("h2");
        String senha = senhaConfigurada;
        boolean senhaGerada = false;

        if (senha == null || senha.isBlank()) {
            if (desenvolvimento) {
                senha = "admin";
            } else {
                senha = gerarSenha();
                senhaGerada = true;
            }
        }

        userRepository.save(UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Administrador")
                .description("Administrador criado na primeira subida")
                .email(email)
                .userName(email)
                .password(passwordEncoder.encode(senha))
                .isAdministrator(true)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .tenantId(tenantPadrao.isBlank() ? null : tenantPadrao)
                .build());

        if (senhaGerada) {
            // Única vez que esta senha aparece em algum lugar. Depois daqui, só o hash existe.
            log.warn("""

                    ┌──────────────────────────────────────────────────────────────────────┐
                    │  ADMINISTRADOR CRIADO — ANOTE A SENHA, ELA NÃO SERÁ MOSTRADA DE NOVO │
                    ├──────────────────────────────────────────────────────────────────────┤
                    │  e-mail: {}
                    │  senha : {}
                    ├──────────────────────────────────────────────────────────────────────┤
                    │  Gerada aleatoriamente porque                                         │
                    │  archbase.boilerplate.seed.admin.password não foi definida fora de    │
                    │  desenvolvimento. TROQUE após o primeiro acesso.                      │
                    └──────────────────────────────────────────────────────────────────────┘
                    """, email, senha);
        } else if (desenvolvimento) {
            log.info("[seed] Administrador criado para desenvolvimento: {} / {}. "
                    + "Em outros perfis a senha é aleatória — defina "
                    + "archbase.boilerplate.seed.admin.password para escolhê-la.", email, senha);
        } else {
            log.info("[seed] Administrador {} criado com a senha configurada em "
                    + "archbase.boilerplate.seed.admin.password.", email);
        }
    }

    /** 18 bytes em Base64 URL-safe: forte o bastante e sem caracteres que atrapalhem copiar do log. */
    private String gerarSenha() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
