package br.com.archbase.boilerplate.rest.infrastructure.config;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.spi.ArchbaseRoleResolver;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Ponto de partida para as roles de negócio da sua aplicação.
 *
 * <p><b>Por que este arquivo existe vazio de regra.</b> A anotação {@code @RequireRole} do archbase
 * cobre roles que <i>não</i> são o Profile/Group do framework: são do domínio da aplicação —
 * "GERENTE_LOJA", "ENTREGADOR", "AUDITOR" — e moram no modelo dela. O framework não tem como
 * descobri-las, por isso pergunta através deste SPI.
 *
 * <p>Como não existe domínio de roles neste boilerplate, {@link #resolveRoles} devolve o conjunto
 * vazio. A consequência é a correta para um esqueleto: qualquer método anotado com
 * {@code @RequireRole} <b>nega</b> acesso até que alguém implemente a consulta de verdade. É o
 * oposto de liberar por omissão.
 *
 * <p><b>Para usar de fato</b>, troque o corpo por uma consulta ao seu modelo:
 *
 * <pre>{@code
 * @Override
 * public Set<String> resolveRoles(UserEntity user) {
 *     return lojaRepository.findRolesDoUsuario(user.getId());
 * }
 * }</pre>
 *
 * <p><b>Se a sua aplicação não usa {@code @RequireRole}</b>, este bean ainda tem função: sem um
 * {@code ArchbaseRoleResolver} registrado, a configuração
 * {@code archbase.security.require-role.no-resolver-policy=deny} impede a aplicação de subir — o
 * framework recusa manter uma proteção que não teria como funcionar. Apagar este arquivo exige,
 * portanto, devolver aquela chave para {@code permit}.
 */
@Component
public class BoilerplateRoleResolver implements ArchbaseRoleResolver {

    @Override
    public Set<String> resolveRoles(UserEntity user) {
        // Vazio, e não nulo: a comparação precisa resultar em negação, não em erro.
        return Set.of();
    }

    @Override
    public boolean isOwner(UserEntity user) {
        // "Não sei responder" e "é proprietário" não podem dar no mesmo resultado num controle de
        // acesso. Implemente quando usar @RequireRole(ownerOnly = true).
        return false;
    }
}
