package br.com.yann.sextafeira.core.controller;

import br.com.yann.sextafeira.core.dto.HistoricoMensagemDTO;
import br.com.yann.sextafeira.core.service.MensagemHistoricoService;
import br.com.yann.sextafeira.identity.service.UsuarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mensagens")
public class MensagemHistoricoController {

    private final MensagemHistoricoService mensagemHistoricoService;
    private final UsuarioService usuarioService;

    public MensagemHistoricoController(MensagemHistoricoService mensagemHistoricoService, UsuarioService usuarioService) {
        this.mensagemHistoricoService = mensagemHistoricoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/historico")
    public List<HistoricoMensagemDTO> obterHistorico(@RequestParam(defaultValue = "20") int limite) {
        int cap = Math.min(Math.max(1, limite), 100);
        return mensagemHistoricoService.obterHistoricoRecente(usuarioService.obterUsuarioAtual(), cap);
    }
}
