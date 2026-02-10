package br.com.yann.sextafeira.core.webhook;

import br.com.yann.sextafeira.identity.domain.Usuario;
import br.com.yann.sextafeira.core.dto.ChatResponse;
import br.com.yann.sextafeira.core.integration.whatsapp.dto.WhatsappIncomingMessageDTO;
import br.com.yann.sextafeira.core.integration.whatsapp.dto.WhatsappOutgoingMessageDTO;
import br.com.yann.sextafeira.core.service.AssistenteService;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import br.com.yann.sextafeira.core.integration.whatsapp.WhatsappService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whatsapp/webhook")
public class WhatsappWebhookController {

    private final AssistenteService assistenteService;
    private final WhatsappService whatsappService;
    private final UsuarioService usuarioService;

    public WhatsappWebhookController(AssistenteService assistenteService,
                                     WhatsappService whatsappService,
                                     UsuarioService usuarioService) {
        this.assistenteService = assistenteService;
        this.whatsappService = whatsappService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void receberMensagem(@RequestBody WhatsappIncomingMessageDTO incoming) {

        System.out.println("Webhook recebido do número: " + incoming.getFrom());

        // 1) buscar ou criar usuário
        Usuario usuario = usuarioService.buscarOuCriarPorWhatsapp(incoming.getFrom());
        System.out.println("Usuário associado (id=" + usuario.getId() + ")");

        String texto = incoming.getMessage();

        // 2) processar mensagem (por enquanto ainda sem usar o usuário)
        ChatResponse resposta = assistenteService.processarMensagem(texto);

        // 3) enviar resposta
        WhatsappOutgoingMessageDTO outgoing =
                new WhatsappOutgoingMessageDTO(usuario.getWhatsappNumber(), resposta.getResposta());

        whatsappService.enviarMensagem(outgoing);
    }
}
