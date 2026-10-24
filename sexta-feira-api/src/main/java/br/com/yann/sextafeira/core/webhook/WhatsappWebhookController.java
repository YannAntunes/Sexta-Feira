package br.com.yann.sextafeira.core.webhook;

import br.com.yann.sextafeira.core.dto.ChatResponse;
import br.com.yann.sextafeira.core.integration.whatsapp.WhatsappService;
import br.com.yann.sextafeira.core.integration.whatsapp.dto.WhatsappIncomingMessageDTO;
import br.com.yann.sextafeira.core.integration.whatsapp.dto.WhatsappOutgoingMessageDTO;
import br.com.yann.sextafeira.core.service.AssistenteService;
import br.com.yann.sextafeira.identity.context.UsuarioContextHolder;
import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whatsapp/webhook")
public class WhatsappWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookController.class);

    private final AssistenteService assistenteService;
    private final WhatsappService whatsappService;
    private final UsuarioService usuarioService;

    @Value("${whatsapp.webhook.secret:}")
    private String webhookSecret;

    public WhatsappWebhookController(AssistenteService assistenteService,
                                     WhatsappService whatsappService,
                                     UsuarioService usuarioService) {
        this.assistenteService = assistenteService;
        this.whatsappService = whatsappService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void receberMensagem(
            @RequestHeader(value = "X-Webhook-Token", required = false) String tokenRecebido,
            @RequestBody WhatsappIncomingMessageDTO incoming
    ) {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (!webhookSecret.equals(tokenRecebido)) {
                log.warn("Tentativa de chamada ao webhook sem X-Webhook-Token valido.");
                throw new SecurityException("Token de webhook inválido ou ausente.");
            }
        }

        if (incoming == null || incoming.getFrom() == null || incoming.getFrom().isBlank()) {
            throw new IllegalArgumentException("Remetente (from) da mensagem do WhatsApp é obrigatório.");
        }

        Usuario usuario = usuarioService.buscarOuCriarPorWhatsapp(incoming.getFrom());
        log.info("Mensagem recebida via webhook para usuarioId={}", usuario.getId());

        try {
            UsuarioContextHolder.setUsuario(usuario);
            ChatResponse resposta = assistenteService.processarMensagem(usuario, incoming.getMessage());

            WhatsappOutgoingMessageDTO outgoing =
                    new WhatsappOutgoingMessageDTO(usuario.getWhatsappNumber(), resposta.getResposta());
            whatsappService.enviarMensagem(outgoing);
        } finally {
            UsuarioContextHolder.clear();
        }
    }
}
