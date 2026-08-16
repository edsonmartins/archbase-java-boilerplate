package br.com.archbase.boilerplate.rest.infrastructure.config;

import br.com.archbase.security.service.ArchbaseEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Envio de e-mail do fluxo de credenciais — aqui, apenas registrado em log.
 *
 * <p><b>Por que este arquivo existe.</b> O archbase declara o SPI {@link ArchbaseEmailService} e não
 * traz implementação: cada aplicação pluga o seu provedor. Sem um bean desta interface, o stub do
 * framework lança "forneça uma implementação de ArchbaseEmailService", o controller converte isso em
 * HTTP 500, e a recuperação de senha não funciona.
 *
 * <p><b>E o efeito não parava aí.</b> Com {@code prevent-user-enumeration=true}, pedir recuperação
 * para um e-mail que não existe responde 200 — resposta uniforme, que é o ponto da proteção. Só que
 * o e-mail que <i>existe</i> chegava ao envio, o envio explodia, e a resposta virava 500. A diferença
 * entre 500 e 200 dizia exatamente o que a proteção queria esconder: quem tem conta. A proteção
 * estava ligada e, ainda assim, dava para enumerar usuários — porque o vazamento vinha da falha, não
 * da lógica.
 *
 * <p><b>Isto não envia e-mail.</b> É um ponto de extensão com comportamento de desenvolvimento:
 * registra em log o que seria enviado, para o fluxo poder ser exercitado sem servidor SMTP. Numa
 * aplicação de verdade, troque o corpo dos métodos por uma chamada ao seu provedor — SMTP via
 * {@code JavaMailSender}, SES, SendGrid, o que for.
 *
 * <p><b>O token só aparece no log no perfil dev.</b> Ele vale como credencial: quem o lê troca a
 * senha da conta. Em qualquer outro perfil fica registrado apenas que houve um pedido, sem o
 * segredo — log costuma ir para agregador, e agregador costuma ter mais leitores que o banco.
 */
@Service
@Slf4j
public class BoilerplateEmailService implements ArchbaseEmailService {

    private final boolean desenvolvimento;

    public BoilerplateEmailService(Environment environment) {
        this.desenvolvimento = Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    @Override
    public void sendResetPasswordEmail(String email, String resetPasswordToken, String userName, String name) {
        if (desenvolvimento) {
            log.info("[e-mail não configurado] Reset de senha para {} — token: {}", email, resetPasswordToken);
        } else {
            log.info("[e-mail não configurado] Reset de senha solicitado para {}. "
                    + "Implemente o BoilerplateEmailService para que a mensagem seja entregue.", email);
        }
    }

    @Override
    public void sendActivationTokenApiEmail(String email, String token, String userName, String name) {
        if (desenvolvimento) {
            log.info("[e-mail não configurado] Ativação de token de API para {} — token: {}", email, token);
        } else {
            log.info("[e-mail não configurado] Ativação de token de API solicitada para {}. "
                    + "Implemente o BoilerplateEmailService para que a mensagem seja entregue.", email);
        }
    }
}
