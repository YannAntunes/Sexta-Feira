package br.com.yann.sextafeira;

import br.com.yann.sextafeira.core.controller.PingController;
import br.com.yann.sextafeira.core.dto.ChatResponse;
import br.com.yann.sextafeira.core.service.AssistenteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SextaFeiraApiApplicationTests {

	@Autowired
	private PingController pingController;

	@Autowired
	private AssistenteService assistenteService;

	@Test
	void contextLoads() {
		assertEquals("SEXTA-FEIRA online", pingController.ping());
	}

	@Test
	void deveTratarMensagemVaziaOuFalhaNoServicoPythonSemDerrubarBackend() {
		ChatResponse vazio = assistenteService.processarMensagem("   ");
		assertNotNull(vazio);
		assertNotNull(vazio.getResposta());

		// Mesmo se o serviço Python não estiver rodando na porta 8000 durante o teste unitário/integração,
		// o AssistenteService não deve lançar exceção não tratada.
		ChatResponse resposta = assistenteService.processarMensagem("teste de resiliencia");
		assertNotNull(resposta);
		assertNotNull(resposta.getResposta());
	}

}
