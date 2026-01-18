package br.com.yann.sextafeira.service;

import br.com.yann.sextafeira.dto.WhatsappOutgoingMessageDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsappService {

    private final RestTemplate restTemplate;

    @Value("${whatsapp.api.url:https://exemplo-do-provedor.com/send}")
    private String whatsappApiUrl;

    @Value("${whatsapp.api.token:TOKEN_AQUI}")
    private String whatsappApiToken;

    public WhatsappService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void enviarMensagem(WhatsappOutgoingMessageDTO mensagem) {
        System.out.println("Simulando envio para WhatsApp:");
        System.out.println("Para: " + mensagem.getTo());
        System.out.println("Mensagem: " + mensagem.getMessage());
    }
}

