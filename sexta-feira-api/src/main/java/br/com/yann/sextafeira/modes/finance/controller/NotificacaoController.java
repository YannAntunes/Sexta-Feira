package br.com.yann.sextafeira.modes.finance.controller;

import br.com.yann.sextafeira.modes.finance.domain.model.NotificacaoUsuario;
import br.com.yann.sextafeira.modes.finance.service.NotificacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public List<NotificacaoUsuario> listar(@RequestParam(required = false, defaultValue = "false") boolean apenasNaoLidas) {
        if (apenasNaoLidas) {
            return notificacaoService.listarNaoLidas();
        }
        return notificacaoService.listarTodas();
    }

    @GetMapping("/nao-lidas/count")
    public Map<String, Long> contarNaoLidas() {
        return Map.of("count", notificacaoService.contarNaoLidas());
    }

    @PatchMapping("/{id}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarComoLida(@PathVariable Long id) {
        notificacaoService.marcarComoLida(id);
    }

    @PostMapping("/marcar-todas-lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasComoLidas() {
        notificacaoService.marcarTodasComoLidas();
    }
}
