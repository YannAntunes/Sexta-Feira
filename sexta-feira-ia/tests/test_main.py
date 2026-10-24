import unittest
import sys
from pathlib import Path
from decimal import Decimal

# Garante que o diretório raiz do serviço IA está no sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import main
from main import (
    MensagemRequest,
    ChartCategoriaRequest,
    ChartCategoriaItem,
    ChartBudgetRequest,
    ChartBudgetItem,
    ChartSerieRequest,
    ChartSerieItem,
)


class SextaFeiraIaSmokeTest(unittest.TestCase):

    def test_health_endpoint(self):
        res = main.health()
        self.assertEqual(res["status"], "ok")
        self.assertEqual(res["service"], "sexta-feira-ia")

    def test_interpretar_transacao_despesa(self):
        req = MensagemRequest(mensagem="gastei 45,90 no mercado ontem")
        res = main.interpretar_transacao(req)
        self.assertEqual(res.valor, Decimal("45.90"))
        self.assertEqual(res.tipo, "DESPESA")
        self.assertEqual(res.categoria, "ALIMENTACAO")

    def test_interpretar_transacao_receita(self):
        req = MensagemRequest(mensagem="recebi 3500 de salario hoje")
        res = main.interpretar_transacao(req)
        self.assertEqual(res.valor, Decimal("3500"))
        self.assertEqual(res.tipo, "RECEITA")

    def test_router_convert_currency(self):
        req = MensagemRequest(mensagem="converter 100 usd para brl")
        res = main.router(req)
        self.assertEqual(res.intent, "CONVERT_CURRENCY")
        self.assertEqual(res.entities["amount"], 100.0)
        self.assertEqual(res.entities["from"], "USD")
        self.assertEqual(res.entities["to"], "BRL")

    def test_router_quanto_gastei_nao_deve_criar_transacao_bug01(self):
        for pergunta in [
            "quanto gastei essa semana",
            "quanto eu gastei hoje",
            "gastei quanto ontem?",
            "how much i spent this week",
        ]:
            res = main.router(MensagemRequest(mensagem=pergunta))
            self.assertEqual(
                res.intent,
                "ASK_PERIOD_SUMMARY",
                f"Falha para '{pergunta}': retornou {res.intent}",
            )

    def test_extrair_valor_ignora_dia_e_ticker_fin01(self):
        res1 = main.interpretar_transacao(
            MensagemRequest(mensagem="dia 15 gastei 45,90 no mercado")
        )
        self.assertEqual(res1.valor, Decimal("45.90"))
        self.assertEqual(res1.data.day, 15)

        res2 = main.interpretar_transacao(
            MensagemRequest(mensagem="em 10/05 paguei R$ 120,00 de internet")
        )
        self.assertEqual(res2.valor, Decimal("120.00"))
        self.assertEqual(res2.data.day, 10)
        self.assertEqual(res2.data.month, 5)

    def test_router_set_budget_retorna_entidades(self):
        res = main.router(
            MensagemRequest(mensagem="definir orçamento de alimentação 1200 reais")
        )
        self.assertEqual(res.intent, "SET_BUDGET")
        self.assertEqual(res.entities.get("amount"), 1200.0)
        self.assertEqual(res.entities.get("category"), "ALIMENTACAO")

    def test_router_compra_acao_com_digito_no_ticker(self):
        res = main.router(MensagemRequest(mensagem="comprei 10 ações da PETR4"))
        self.assertEqual(res.intent, "ADD_HOLDING_QTY")
        self.assertEqual(res.entities.get("ticker"), "PETR4")
        self.assertEqual(res.entities.get("qty"), 10.0)

    def test_router_set_personality(self):
        for frase, esperado in [
            ("mude para o modo formal", "FORMAL"),
            ("ative o jarvis", "JARVIS"),
            ("seja mais amigável", "AMIGAVEL"),
            ("volte para o modo sexta-feira", "SEXTA_FEIRA"),
        ]:
            res = main.router(MensagemRequest(mensagem=frase))
            self.assertEqual(res.intent, "SET_PERSONALITY")
            self.assertEqual(res.entities.get("personality"), esperado)

    def test_llm_fallback_padrao_desativado(self):
        self.assertFalse(main.is_llm_enabled())
        # Mesmo com mensagem complexa, funciona via fallback determinístico
        res = main.router(MensagemRequest(mensagem="gastei 50 no almoço"))
        self.assertEqual(res.intent, "ADD_TRANSACTION")

    def test_charts_generation_returns_png(self):
        r1 = main.chart_gastos_por_categoria(
            ChartCategoriaRequest(
                titulo="Gastos",
                items=[ChartCategoriaItem(categoria="ALIMENTACAO", total=120.5)],
            )
        )
        self.assertEqual(r1.media_type, "image/png")

        r2 = main.chart_orcamento_vs_gasto(
            ChartBudgetRequest(
                titulo="Budget",
                items=[ChartBudgetItem(categoria="ALIMENTACAO", orcamento=500.0, gasto=250.0)],
            )
        )
        self.assertEqual(r2.media_type, "image/png")

        r3 = main.chart_serie_linha(
            ChartSerieRequest(
                titulo="Serie",
                items=[ChartSerieItem(label="2026-10-01", value=100.0)],
            )
        )
        self.assertEqual(r3.media_type, "image/png")


if __name__ == "__main__":
    unittest.main()
