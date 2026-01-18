package br.com.yann.sextafeira.controller;

import br.com.yann.sextafeira.dto.ChatRequest;
import br.com.yann.sextafeira.dto.ChatResponse;
import br.com.yann.sextafeira.service.AssistenteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final AssistenteService assistenteService;

    public ChatController(AssistenteService assistenteService) {
        this.assistenteService = assistenteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ChatResponse conversar(@RequestBody ChatRequest request) {
        return assistenteService.processarMensagem(request.getMensagem());
    }
}
