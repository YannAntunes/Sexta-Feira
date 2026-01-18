package br.com.yann.sextafeira.controller;

import br.com.yann.sextafeira.dto.ChatResponse;
import br.com.yann.sextafeira.dto.WhatsappIncomingMessageDTO;
import br.com.yann.sextafeira.dto.WhatsappOutgoingMessageDTO;
import br.com.yann.sextafeira.service.AssistenteService;
import br.com.yann.sextafeira.service.WhatsappService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whatsapp/webhook")
public class WhatsappWebhookController {

    private final AssistenteService assistenteService;
    private final WhatsappService whatsappService;

    public WhatsappWebhookController(AssistenteService assistenteService,
                                     WhatsappService whatsappService) {
        this.assistenteService = assistenteService;
        this.whatsappService = whatsappService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void receberMensagem(@RequestBody WhatsappIncomingMessageDTO incoming) {

        // Só pra ter certeza que está chegando até aqui:
        System.out.println("Webhook recebido do número: " + incoming.getFrom());
        System.out.println("Mensagem recebida: " + incoming.getMessage());

        String numeroUsuario = incoming.getFrom();
        String texto = incoming.getMessage();

        // passa pro cérebro da SEXTA-FEIRA
        ChatResponse resposta = assistenteService.processarMensagem(texto);

        WhatsappOutgoingMessageDTO outgoing =
                new WhatsappOutgoingMessageDTO(numeroUsuario, resposta.getResposta());

        // AQUI ele chama o service que você mostrou
        whatsappService.enviarMensagem(outgoing);
    }
}
