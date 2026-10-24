import unittest
import sys
import os
from decimal import Decimal
from pathlib import Path

# Garante path para importar app/main e core
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from core.nlu_engine import (
    analisar_mensagem,
    extrair_valor_inteligente,
    converter_extenso_para_numero,
    classificar_categoria_inteligente,
)
from main import router, MensagemRequest


class TestNluAvancado(unittest.TestCase):

    # 1. Frases obrigatórias de exemplo da Fase 2
    def test_hoje_torrei_500_reais(self):
        res = analisar_mensagem("Hoje torrei 500 reais.")
        self.assertEqual(res["intent"], "ADD_TRANSACTION")
        self.assertEqual(res["entities"]["valor"], 500.0)
        self.assertEqual(res["entities"]["tipo"], "DESPESA")
        self.assertFalse(res["needs_clarification"])

    def test_gastei_quinhentao_com_roupa(self):
        res = analisar_mensagem("Gastei quinhentão com roupa.")
        self.assertEqual(res["intent"], "ADD_TRANSACTION")
        self.assertEqual(res["entities"]["valor"], 500.0)
        self.assertEqual(res["entities"]["tipo"], "DESPESA")
        self.assertEqual(res["entities"]["categoria"], "OUTROS")
        self.assertFalse(res["needs_clarification"])

    def test_paguei_80_conto_no_almoco(self):
        res = analisar_mensagem("Paguei 80 conto no almoço.")
        self.assertEqual(res["intent"], "ADD_TRANSACTION")
        self.assertEqual(res["entities"]["valor"], 80.0)
        self.assertEqual(res["entities"]["tipo"], "DESPESA")
        self.assertEqual(res["entities"]["categoria"], "ALIMENTACAO")
        self.assertFalse(res["needs_clarification"])

    def test_saiu_120_da_minha_conta(self):
        res = analisar_mensagem("Saiu 120 da minha conta.")
        self.assertEqual(res["intent"], "ADD_TRANSACTION")
        self.assertEqual(res["entities"]["valor"], 120.0)
        self.assertEqual(res["entities"]["tipo"], "DESPESA")
        self.assertFalse(res["needs_clarification"])

    def test_recebi_dois_mil_do_trabalho(self):
        res = analisar_mensagem("Recebi dois mil do trabalho.")
        self.assertEqual(res["intent"], "ADD_TRANSACTION")
        self.assertEqual(res["entities"]["valor"], 2000.0)
        self.assertEqual(res["entities"]["tipo"], "RECEITA")
        self.assertFalse(res["needs_clarification"])

    def test_caiu_um_dinheiro_na_conta_pede_esclarecimento(self):
        res = analisar_mensagem("Caiu um dinheiro na conta.")
        self.assertEqual(res["intent"], "CLARIFICATION_REQUIRED")
        self.assertTrue(res["needs_clarification"])
        self.assertIn("clarification_prompt", res)
        self.assertIn("não identifiquei o valor exato", res["clarification_prompt"].lower())

    def test_gastei_com_roupa_pede_esclarecimento(self):
        res = analisar_mensagem("Gastei com roupa.")
        self.assertEqual(res["intent"], "CLARIFICATION_REQUIRED")
        self.assertTrue(res["needs_clarification"])
        self.assertIn("clarification_prompt", res)
        self.assertIn("não identifiquei o valor exato", res["clarification_prompt"].lower())

    def test_quanto_eu_gastei_com_alimentacao_este_mes(self):
        res = analisar_mensagem("Quanto eu gastei com alimentação este mês?")
        self.assertEqual(res["intent"], "ASK_PERIOD_SUMMARY")
        self.assertEqual(res["entities"]["range"], "THIS_MONTH")
        self.assertEqual(res["entities"]["category"], "ALIMENTACAO")
        self.assertFalse(res["needs_clarification"])

    def test_me_mostra_o_grafico_das_minhas_despesas(self):
        res = analisar_mensagem("Me mostra o gráfico das minhas despesas.")
        self.assertEqual(res["intent"], "ASK_FINANCES_CHART")
        self.assertEqual(res["entities"]["tipo"], "despesas_categoria")
        self.assertFalse(res["needs_clarification"])

    def test_apliquei_300_reais_em_acoes(self):
        res = analisar_mensagem("Apliquei 300 reais em ações.")
        self.assertEqual(res["intent"], "ADD_TRANSACTION")
        self.assertEqual(res["entities"]["valor"], 300.0)
        self.assertEqual(res["entities"]["categoria"], "INVESTIMENTOS")
        self.assertFalse(res["needs_clarification"])

    def test_vendi_algumas_acoes_quanto_recebi(self):
        res = analisar_mensagem("Vendi algumas ações. Quanto recebi?")
        self.assertEqual(res["intent"], "ASK_PORTFOLIO_REPORT")
        self.assertFalse(res["needs_clarification"])

    def test_vendi_5_acoes_por_200_reais_cada(self):
        res = analisar_mensagem("Vendi 5 ações por 200 reais cada.")
        self.assertEqual(res["intent"], "ADD_TRANSACTION")
        self.assertEqual(res["entities"]["valor"], 1000.0)
        self.assertEqual(res["entities"]["qty"], 5.0)
        self.assertEqual(res["entities"]["unit_price"], 200.0)
        self.assertEqual(res["entities"]["tipo"], "RECEITA")
        self.assertEqual(res["entities"]["categoria"], "INVESTIMENTOS")
        self.assertFalse(res["needs_clarification"])

    def test_vendi_cinco_acoes_por_duzentos_reais_cada(self):
        res = analisar_mensagem("vendi cinco acoes por duzentos reais cada")
        self.assertEqual(res["intent"], "ADD_TRANSACTION")
        self.assertEqual(res["entities"]["valor"], 1000.0)
        self.assertEqual(res["entities"]["qty"], 5.0)
        self.assertEqual(res["entities"]["unit_price"], 200.0)
        self.assertEqual(res["entities"]["tipo"], "RECEITA")
        self.assertEqual(res["entities"]["categoria"], "INVESTIMENTOS")
        self.assertFalse(res["needs_clarification"])

    def test_transcricao_fala_sem_pontuacao(self):
        # Fala espontanea sem pontuacao ou acentos gerada por Speech-to-Text
        res1 = analisar_mensagem("hoje torrei 500 reais")
        self.assertEqual(res1["intent"], "ADD_TRANSACTION")
        self.assertEqual(res1["entities"]["valor"], 500.0)
        self.assertEqual(res1["entities"]["tipo"], "DESPESA")

        res2 = analisar_mensagem("nao eu quis dizer 50 nao 500")
        self.assertEqual(res2["intent"], "CORRECTION")
        self.assertEqual(res2["entities"]["novo_valor"], 50.0)

    def test_correcao_conversacional_nao_quis_dizer(self):
        res = analisar_mensagem("Não, eu quis dizer 50, não 500.")
        self.assertEqual(res["intent"], "CORRECTION")
        self.assertEqual(res["entities"]["novo_valor"], 50.0)
        self.assertFalse(res["needs_clarification"])

    def test_desfaz_o_ultimo_lancamento(self):
        res = analisar_mensagem("Desfaz o último lançamento.")
        self.assertEqual(res["intent"], "DELETE_TRANSACTION")
        self.assertEqual(res["entities"]["action"], "undo_last")
        self.assertFalse(res["needs_clarification"])

    # 2. Gírias e termos monetários brasileiros
    def test_girias_monetarias_brasileiras(self):
        self.assertEqual(converter_extenso_para_numero("quinhentão"), Decimal("500.00"))
        self.assertEqual(converter_extenso_para_numero("um barão"), Decimal("1000.00"))
        self.assertEqual(converter_extenso_para_numero("dois barões"), Decimal("2000.00"))
        self.assertEqual(converter_extenso_para_numero("80 conto"), Decimal("80"))
        self.assertEqual(converter_extenso_para_numero("50 pau"), Decimal("50"))

    # 3. Números compostos por extenso
    def test_numeros_extenso_compostos(self):
        self.assertEqual(converter_extenso_para_numero("dois mil"), Decimal("2000"))
        self.assertEqual(converter_extenso_para_numero("dois mil e quinhentos"), Decimal("2500"))
        self.assertEqual(converter_extenso_para_numero("cento e cinquenta"), Decimal("150"))

    # 4. Comandos em Inglês
    def test_comandos_em_ingles(self):
        res1 = analisar_mensagem("I spent 50 on lunch")
        self.assertEqual(res1["intent"], "ADD_TRANSACTION")
        self.assertEqual(res1["entities"]["valor"], 50.0)
        self.assertEqual(res1["entities"]["categoria"], "ALIMENTACAO")

        res2 = analisar_mensagem("How much did I spend this month")
        self.assertEqual(res2["intent"], "ASK_PERIOD_SUMMARY")
        self.assertEqual(res2["entities"]["range"], "THIS_MONTH")

        res3 = analisar_mensagem("show charts")
        self.assertEqual(res3["intent"], "ASK_FINANCES_CHART")

    # 5. Roteamento integrado via router FastAPI
    def test_router_fastapi_integracao_completa(self):
        req1 = MensagemRequest(mensagem="Hoje torrei 500 reais.")
        resp1 = router(req1)
        self.assertEqual(resp1.intent, "ADD_TRANSACTION")
        self.assertEqual(resp1.entities["valor"], 500.0)
        self.assertEqual(resp1.lang, "pt")

        req2 = MensagemRequest(mensagem="Caiu um dinheiro na conta.")
        resp2 = router(req2)
        self.assertEqual(resp2.intent, "CLARIFICATION_REQUIRED")
        self.assertIn("clarification_prompt", resp2.entities)

        req3 = MensagemRequest(mensagem="How much did I spend this month")
        resp3 = router(req3)
        self.assertEqual(resp3.intent, "ASK_PERIOD_SUMMARY")
        self.assertEqual(resp3.lang, "en")


if __name__ == "__main__":
    unittest.main()
