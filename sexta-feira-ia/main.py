from fastapi import FastAPI
from pydantic import BaseModel, Field
from datetime import date, timedelta
from decimal import Decimal
from typing import Any, Dict, Optional
import re
from typing import List
from fastapi.responses import StreamingResponse
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.figure import Figure
import io
import unicodedata
import traceback
import os
import json
import urllib.request
import urllib.error
import logging
import time
from core.nlu_engine import (
    analisar_mensagem,
    extrair_valor_inteligente,
    classificar_categoria_inteligente,
)

logger = logging.getLogger("sexta_ia_diagnostic")
if not logger.handlers:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")

def mascarar_dados_sensiveis(texto: str) -> str:
    """Mascara CPFs e numeros de cartao preservando formato sem expor segredos."""
    texto = re.sub(r"\b(?:\d[ -]*?){13,19}\b", lambda m: "****-****-****-" + re.sub(r"\D", "", m.group(0))[-4:], texto)
    texto = re.sub(r"\b\d{3}\.?\d{3}\.?\d{3}[-.]?\d{2}\b", "***.***.***-**", texto)
    return texto


app = FastAPI(title="SEXTA-FEIRA IA Service")


# ========= MODELS =========

class MensagemRequest(BaseModel):
    mensagem: str = Field(..., min_length=1, max_length=1000)
    personalidade: Optional[str] = None
    historico: Optional[List[Dict[str, Any]]] = None


class TransacaoInterpretada(BaseModel):
    valor: Decimal
    data: date
    tipo: str          # "DESPESA" ou "RECEITA"
    categoria: str     # "ALIMENTACAO", "TRANSPORTE", etc.
    descricao: str


class RouterResponse(BaseModel):
    intent: str
    entities: Dict[str, Any] = {}
    lang: str = "pt"

class ChartCategoriaItem(BaseModel):
    categoria: str
    total: float

class ChartCategoriaRequest(BaseModel):
    titulo: str = "Gastos por categoria"
    items: List[ChartCategoriaItem]

class ChartBudgetItem(BaseModel):
    categoria: str
    orcamento: float
    gasto: float

class ChartBudgetRequest(BaseModel):
    titulo: str = "Orçamento vs Gasto"
    items: list[ChartBudgetItem]

class ChartSerieItem(BaseModel):
    label: str   # "2026-01-01" ou "01"
    value: float

class ChartSerieRequest(BaseModel):
    titulo: str = "Evolução de gastos"
    items: list[ChartSerieItem]
    y_label: str = "R$"

class IaTransacaoRequest(BaseModel):
    mensagem: str

class IaRouterResponse(BaseModel):
    intent: str
    entities: Dict[str, Any] = {}
    lang: str = "pt"

# ========= HELPERS (TRANSAÇÃO) =========

def _limpar_tokens_nao_monetarios(mensagem: str) -> str:
    limpa = mensagem
    # Remove datas dd/mm(/yyyy) ou dd-mm(-yyyy)
    limpa = re.sub(r"\b\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?\b", " ", limpa)
    # Remove "dia 15"
    limpa = re.sub(r"\bdia\s+\d{1,2}\b", " ", limpa, flags=re.IGNORECASE)
    # Remove "15 de janeiro"
    limpa = re.sub(
        r"\b\d{1,2}\s+de\s+(?:janeiro|fevereiro|março|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)\b",
        " ",
        limpa,
        flags=re.IGNORECASE,
    )
    # Remove "há N dias" / "últimos N dias" / "N days ago" / "last N days"
    limpa = re.sub(r"\b(?:há|ha|últimos|ultimos|last)\s+\d+\s+(?:dias|days)\b", " ", limpa, flags=re.IGNORECASE)
    limpa = re.sub(r"\b\d+\s+days\s+ago\b", " ", limpa, flags=re.IGNORECASE)
    # Remove tickers da B3 (ex: PETR4, MXRF11, B3SA3) para não capturar o dígito final
    limpa = re.sub(r"\b[A-Za-z]{4}\d{1,2}\b", " ", limpa)
    limpa = re.sub(r"\bB3SA3\b", " ", limpa, flags=re.IGNORECASE)
    return limpa


def extrair_valor(mensagem: str) -> Decimal | None:
    # 1) Prioriza valor explícito com R$ ou seguido de reais/real
    m_brl = re.search(r"r\$\s*(\d+(?:[.,]\d+)?)", mensagem, flags=re.IGNORECASE)
    if not m_brl:
        m_brl = re.search(r"(\d+(?:[.,]\d+)?)\s*(?:reais|real)\b", mensagem, flags=re.IGNORECASE)
    if m_brl:
        try:
            return Decimal(m_brl.group(1).replace(",", "."))
        except Exception:
            pass

    # 2) Limpa datas e tickers antes de buscar número geral
    limpa = _limpar_tokens_nao_monetarios(mensagem)
    padrao = r"\d+(?:[.,]\d+)?"
    match = re.search(padrao, limpa.replace(",", "."))
    if match:
        try:
            return Decimal(match.group())
        except Exception:
            return None
    return None


def determinar_tipo(mensagem: str) -> str:
    m = mensagem.lower()
    termos_receita = [
        "recebi", "recebido", "recebimento", "salario", "salário", "ganhei",
        "vendi", "venda", "rendimento", "rendimentos", "dividendo", "dividendos",
        "lucro", "provento", "proventos", "depósito", "deposito", "entrada",
        "pingou", "caiu", "entrou", "estorno", "cashback", "remuneração",
        "remuneracao", "resgate", "resgatei", "received", "earned", "salary",
        "income", "sold", "dividend"
    ]
    if any(t in m for t in termos_receita):
        if ("gastei" in m or "paguei" in m or "comprei" in m or "torrei" in m) and not ("vendi" in m or "resgatei" in m):
            return "DESPESA"
        return "RECEITA"
    return "DESPESA"


def determinar_categoria(mensagem: str) -> str:
    m = mensagem.lower()

    if ("mercado" in m or "comida" in m or "lanche" in m or
        "alimentacao" in m or "alimentação" in m or
        "bebida" in m or "refri" in m or "restaurante" in m or "ifood" in m):
        return "ALIMENTACAO"

    if "uber" in m or "ônibus" in m or "onibus" in m or "gasolina" in m or "metro" in m:
        return "TRANSPORTE"

    if "aluguel" in m or "condominio" in m or "condomínio" in m:
        return "MORADIA"

    if "netflix" in m or "cinema" in m or "show" in m or "lazer" in m or "passeio" in m:
        return "LAZER"

    if ("farmacia" in m or "farmácia" in m or "remedio" in m or "remédio" in m or
        "médico" in m or "medico" in m):
        return "SAUDE"

    if "faculdade" in m or "curso" in m or "livro" in m or "escola" in m:
        return "EDUCACAO"

    if ("conta de luz" in m or "conta de água" in m or "conta de agua" in m or
        "energia" in m or "internet" in m):
        return "CONTAS_FIXAS"

    if ("investi" in m or "aporte" in m or "fii" in m or "cripto" in m or
        re.search(r"\bacao\b", m) or
        re.search(r"\bações\b", m) or
        re.search(r"\bacoes\b", m) or
        re.search(r"\betf\b", m)):
        return "INVESTIMENTOS"

    return "OUTROS"


# ========= HELPERS (ROUTER) =========

def detectar_idioma(mensagem: str) -> str:
    m_low = mensagem.lower()
    if any(p in m_low for p in ["how much", "delete", "remove", "spent", "budget", "today", "yesterday"]):
        return "en"
    return "pt"


MESES = {
    "janeiro": 1, "jan": 1,
    "fevereiro": 2, "fev": 2,
    "março": 3, "marco": 3, "mar": 3,
    "abril": 4, "abr": 4,
    "maio": 5,
    "junho": 6, "jun": 6,
    "julho": 7, "jul": 7,
    "agosto": 8, "ago": 8,
    "setembro": 9, "set": 9,
    "outubro": 10, "out": 10,
    "novembro": 11, "nov": 11,
    "dezembro": 12, "dez": 12,
}

DOW = {
    "segunda": 0, "seg": 0, "monday": 0, "mon": 0,
    "terça": 1, "terca": 1, "ter": 1, "tuesday": 1, "tue": 1,
    "quarta": 2, "qua": 2, "wednesday": 2, "wed": 2,
    "quinta": 3, "qui": 3, "thursday": 3, "thu": 3,
    "sexta": 4, "sex": 4, "friday": 4, "fri": 4,
    "sábado": 5, "sabado": 5, "sab": 5, "saturday": 5, "sat": 5,
    "domingo": 6, "dom": 6, "sunday": 6, "sun": 6,
}

def _ultima_ocorrencia_dia_semana(target_dow: int, ref: date) -> date:
    # volta até o último dia da semana desejado (pode ser hoje)
    delta = (ref.weekday() - target_dow) % 7
    return ref - timedelta(days=delta)

def interpretar_data(mensagem: str) -> date:
    m = mensagem.lower()
    hoje = date.today()

    # há N dias / N days ago
    m1 = re.search(r"há\s+(\d+)\s+dias", m) or re.search(r"(\d+)\s+days\s+ago", m)
    if m1:
        n = int(m1.group(1))
        return hoje - timedelta(days=n)

    # anteontem / ontem / hoje
    if "anteontem" in m or "day before yesterday" in m:
        return hoje - timedelta(days=2)
    if "ontem" in m or "yesterday" in m:
        return hoje - timedelta(days=1)
    if "hoje" in m or "today" in m:
        return hoje

    # dd/mm ou dd-mm (assume ano atual)
    m2 = re.search(r"\b(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2,4}))?\b", m)
    if m2:
        d = int(m2.group(1))
        mo = int(m2.group(2))
        y = int(m2.group(3)) if m2.group(3) else hoje.year
        if y < 100:
            y += 2000
        try:
            return date(y, mo, d)
        except ValueError:
            return hoje

    # "dia 15" (assume mês atual)
    m3 = re.search(r"\bdia\s+(\d{1,2})\b", m)
    if m3:
        d = int(m3.group(1))
        try:
            return date(hoje.year, hoje.month, d)
        except ValueError:
            return hoje

    # "15 de janeiro" / "15 jan"
    m4 = re.search(r"\b(\d{1,2})\s+de\s+([a-zç]+)\b", m) or re.search(r"\b(\d{1,2})\s+([a-zç]{3,9})\b", m)
    if m4:
        d = int(m4.group(1))
        mes_txt = m4.group(2)
        if mes_txt in MESES:
            mo = MESES[mes_txt]
            try:
                return date(hoje.year, mo, d)
            except ValueError:
                return hoje

    # "última sexta" / "last friday"
    m5 = re.search(r"(última|ultima)\s+([a-zç]+)", m) or re.search(r"last\s+([a-z]+)", m)
    if m5:
        dia_txt = m5.group(2) if len(m5.groups()) == 2 else m5.group(1)
        if dia_txt in DOW:
            base = _ultima_ocorrencia_dia_semana(DOW[dia_txt], hoje)
            # se caiu hoje, volta 7 dias (porque "última" normalmente significa anterior)
            if base == hoje:
                base = hoje - timedelta(days=7)
            return base

    # "na terça" / "on tuesday" => última ocorrência daquele dia
    for token in re.findall(r"[a-zà-ÿ]+", m):
        if token in DOW:
            return _ultima_ocorrencia_dia_semana(DOW[token], hoje)

    return hoje



def parse_personality(mensagem: str) -> str:
    m = mensagem.lower()
    if "formal" in m:
        return "FORMAL"
    if "jarvis" in m:
        return "JARVIS"
    if "amigavel" in m or "amigável" in m:
        return "AMIGAVEL"
    return "SEXTA_FEIRA"


def is_llm_enabled() -> bool:
    val = os.getenv("LLM_ENABLED", "false").strip().lower()
    return val in ("true", "1", "yes", "on")


def tentar_roteamento_llm(request: MensagemRequest) -> Optional[RouterResponse]:
    if not is_llm_enabled():
        return None

    api_key = os.getenv("LLM_API_KEY", "").strip()
    base_url = os.getenv("LLM_BASE_URL", "https://api.openai.com/v1").rstrip("/")
    model = os.getenv("LLM_MODEL", "gpt-4o-mini")
    try:
        timeout = float(os.getenv("LLM_TIMEOUT_SECONDS", "3.5"))
    except ValueError:
        timeout = 3.5

    # Se for endpoint remoto e não tiver chave nem for localhost, aborta para fallback imediato
    if not api_key and "localhost" not in base_url and "127.0.0.1" not in base_url:
        return None

    system_prompt = (
        "Você é o roteador de intenções financeiras do assistente Sexta-Feira. "
        "Seu papel é analisar a mensagem do usuário (e o histórico de diálogo, se houver) e retornar "
        "estritamente um objeto JSON com as chaves: 'intent' e 'entities'.\n"
        "Intenções permitidas: ADD_TRANSACTION, DELETE_TRANSACTION, ASK_PERIOD_SUMMARY, ASK_MONTH_SUMMARY, "
        "ASK_MONTH_REPORT, SET_BUDGET, ASK_BUDGET_STATUS, ASK_BUDGET_OVERVIEW, CONVERT_CURRENCY, "
        "ASK_SAVING_TIPS, ASK_PORTFOLIO_REPORT, ASK_PORTFOLIO_CHART, ASK_PORTFOLIO_TOP, "
        "ASK_PORTFOLIO_ASSET_DETAIL, ADD_HOLDING_QTY, ADD_HOLDING_VALUE, SELL_HOLDING_QTY, SET_PERSONALITY, UNKNOWN.\n"
        "Retorne APENAS o JSON puro, sem blocos de markdown e sem explicações."
    )

    messages = [{"role": "system", "content": system_prompt}]
    if request.historico:
        for h in request.historico[-6:]:
            if isinstance(h, dict) and "role" in h and "content" in h:
                messages.append({"role": str(h["role"]), "content": str(h["content"])})
    messages.append({"role": "user", "content": request.mensagem})

    payload = {
        "model": model,
        "messages": messages,
        "temperature": 0.0,
        "response_format": {"type": "json_object"}
    }

    url = f"{base_url}/chat/completions"
    headers = {"Content-Type": "application/json"}
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"

    try:
        req = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers=headers,
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            if resp.status != 200:
                return None
            body = json.loads(resp.read().decode("utf-8"))
            raw_content = body["choices"][0]["message"]["content"]
            parsed = json.loads(raw_content)
            intent = parsed.get("intent", "UNKNOWN")
            entities = parsed.get("entities", {})
            if not isinstance(entities, dict):
                entities = {}
            return RouterResponse(intent=intent, entities=entities, lang="pt")
    except Exception:
        # Fallback determinístico transparente em caso de indisponibilidade, timeout ou erro
        return None

def detectar_intencao(mensagem: str) -> str:
    m_low = mensagem.lower()

    # SET PERSONALITY (alteração de personalidade)
    if any(x in m_low for x in ["modo", "personalidade"]) and any(x in m_low for x in [
        "formal", "amigavel", "amigável", "jarvis", "sexta-feira", "sexta feira", "padrao", "padrão"
    ]):
        return "SET_PERSONALITY"
    if any(x in m_low for x in ["seja mais formal", "seja formal", "ative o jarvis", "ative jarvis", "seja mais amigável", "seja amigavel", "seja amigável"]):
        return "SET_PERSONALITY"

    if any(x in m_low for x in [
        "apaga", "apagar", "remove", "remover",
        "deleta", "deletar", "exclui", "excluir",
        "undo", "delete"
    ]):
        return "DELETE_TRANSACTION"

    # Consultas de gastos/período ANTES de ADD_TRANSACTION para não colidir com "gastei"
    if any(x in m_low for x in [
        "quanto gastei", "quanto eu gastei", "gastei quanto",
        "how much i spent", "how much did i spend", "how much spent"
    ]):
        return "ASK_PERIOD_SUMMARY"

    if any(x in m_low for x in [
        "relatório do mês", "relatorio do mes", "relatório mensal", "relatorio mensal",
        "me dá o relatório", "me da o relatorio", "me manda o relatório", "me manda o relatorio",
        "como eu tô esse mês", "como eu to esse mes", "como estou esse mês", "como estou esse mes",
        "monthly report", "month report", "full report", "report this month"
    ]):
        return "ASK_MONTH_REPORT"

    if any(x in m_low for x in [
        "resumo do mês", "resumo do mes", "resumo mensal", "saldo do mês", "saldo do mes"
    ]):
        return "ASK_MONTH_SUMMARY"

    # SET BUDGET
    if any(x in m_low for x in [
        "definir", "define", "defina",
        "ajustar", "ajusta", "ajuste",
        "colocar", "coloca", "coloque",
        "setar", "set"
    ]) and any(x in m_low for x in ["orçamento", "orcamento", "budget"]):
        return "SET_BUDGET"

    # BUDGET OVERVIEW (resumo geral)
    if any(x in m_low for x in [
        "resumo do orçamento", "resumo dos orçamentos", "orçamento geral", "orcamento geral",
        "como estão meus orçamentos", "como estao meus orcamentos",
        "status dos orçamentos", "status do orçamento",
        "budget overview", "overall budget", "all budgets"
    ]):
        return "ASK_BUDGET_OVERVIEW"

    if any(x in m_low for x in [
        "meu orçamento", "meu orcamento",
        "budget status", "how is my budget",
        "como está meu orçamento", "como esta meu orcamento"
    ]):
        return "ASK_BUDGET_STATUS"

    if ("para" in m_low or "to" in m_low) and any(x in m_low for x in [
        "dólar", "dolar", "usd", "euro", "eur", "real", "brl", "convert", "converter", "converte"
    ]):
        return "CONVERT_CURRENCY"

    # SAVING TIPS
    if any(x in m_low for x in [
        "economizar", "economia", "cortar gastos", "reduzir gastos",
        "onde dá pra economizar", "como economizar", "dicas de economia",
        "saving tips", "how can i save", "cut expenses"
    ]):
        return "ASK_SAVING_TIPS"

    # ===== INVESTIMENTOS (antes de ADD_TRANSACTION para que "comprei 10 ações da PETR4" seja investimento) =====
    if any(x in m_low for x in [
        "grafico da carteira", "gráfico da carteira",
        "grafico carteira", "gráfico carteira",
        "evolucao da carteira", "evolução da carteira",
        "grafico do patrimonio", "gráfico do patrimônio",
        "grafico da minha carteira", "gráfico da minha carteira",
        "grafico portfolio", "gráfico portfólio"
    ]):
        return "ASK_PORTFOLIO_CHART"

    if is_asset_detail_query(mensagem):
        return "ASK_PORTFOLIO_ASSET_DETAIL"

    if is_portfolio_top_query(mensagem):
        return "ASK_PORTFOLIO_TOP"

    if any(x in m_low for x in ["adicionar", "adicione", "comprar", "compre", "comprei", "aporte", "investi", "investir"]) and (
        any(x in m_low for x in ["btc", "eth", "bitcoin", "ethereum", "petr4", "b3sa3", "bbas3", "mxrf11", "fii", "fiis", "acao", "ações", "acoes", "cripto"])
        or parse_ticker(mensagem) is not None
    ):
        if any(x in m_low for x in ["real", "reais", "r$"]):
            return "ADD_HOLDING_VALUE"
        return "ADD_HOLDING_QTY"

    if any(x in m_low for x in ["vendi", "vender", "venda", "sell", "sold"]) and (
        any(x in m_low for x in ["btc", "eth", "bitcoin", "ethereum", "fii", "fiis", "acao", "ações", "acoes", "cripto"])
        or parse_ticker(mensagem) is not None
    ):
        return "SELL_HOLDING_QTY"

    if any(x in m_low for x in [
        "carteira", "investimentos", "portfolio", "portfólio", "relatorio de fiis", "relatório de fiis",
        "relatorio de acoes", "relatório de ações", "relatorio de cripto", "relatório de cripto",
        "como está minha carteira", "como esta minha carteira"
    ]):
        return "ASK_PORTFOLIO_REPORT"

    # ADD TRANSACTION (após verificar se não era pergunta ou compra de ativo da carteira)
    if any(x in m_low for x in [
        "gastei", "paguei", "comprei", "recebi", "ganhei",
        "spent", "paid", "bought", "received", "earned"
    ]):
        return "ADD_TRANSACTION"

    # PERIOD REPORT (gastos por período sem verbo de gasto direto)
    if any(x in m_low for x in [
        "gastos de", "últimos", "last", "essa semana", "esta semana", "semana passada",
        "hoje", "ontem", "this week", "yesterday", "today"
    ]):
        if "relatório" not in m_low and "relatorio" not in m_low:
            return "ASK_PERIOD_SUMMARY"

    return "UNKNOWN"


# ========= HELPERS (CONVERSÃO) =========

CURRENCY_MAP = {
    "brl": "BRL", "usd": "USD", "eur": "EUR", "gbp": "GBP", "jpy": "JPY",
    "real": "BRL", "reais": "BRL",
    "dolar": "USD", "dólar": "USD", "dolares": "USD", "dólares": "USD",
    "euro": "EUR", "euros": "EUR",
    "libra": "GBP", "libras": "GBP",
    "iene": "JPY", "ienes": "JPY",
    "dollar": "USD", "dollars": "USD",
    "pound": "GBP", "pounds": "GBP",
    "yen": "JPY"
}

def parse_amount(text: str) -> float | None:
    val = extrair_valor(text)
    if val is None:
        return None
    return float(val)

def parse_currencies(text: str) -> list[str]:
    tokens = re.findall(r"[A-Za-zÀ-ÿ]+", text.lower())
    found = []
    for t in tokens:
        if t in CURRENCY_MAP:
            iso = CURRENCY_MAP[t]
            if iso not in found:
                found.append(iso)
    return found

def parse_convert_entities(msg: str) -> dict:
    amount = parse_amount(msg)
    found = parse_currencies(msg)

    from_cur = None
    to_cur = None

    if len(found) >= 2:
        from_cur, to_cur = found[0], found[1]
    elif len(found) == 1:
        from_cur = found[0]
        to_cur = "BRL"

    return {
        "amount": amount,
        "from": from_cur,
        "to": to_cur
    }

# ========= HELPERS (TIMEFRAME) =========


def parse_last_n_days(m: str) -> int | None:
    m_low = m.lower()
    match = re.search(r"últimos\s+(\d+)\s+dias", m_low) or re.search(r"last\s+(\d+)\s+days", m_low)
    if match:
        return int(match.group(1))
    return None

def detectar_timeframe(mensagem: str) -> dict:
    m = mensagem.lower()
    n = parse_last_n_days(mensagem)
    if n:
        return {"range": "LAST_N_DAYS", "days": n}

    if "ontem" in m or "yesterday" in m:
        return {"range": "YESTERDAY"}
    if "hoje" in m or "today" in m:
        return {"range": "TODAY"}
    if "essa semana" in m or "esta semana" in m or "this week" in m:
        return {"range": "THIS_WEEK"}
    if "semana passada" in m or "last week" in m:
        return {"range": "LAST_WEEK"}
    if "esse mês" in m or "este mês" in m or "this month" in m:
        return {"range": "THIS_MONTH"}
    if "mês passado" in m or "last month" in m:
        return {"range": "LAST_MONTH"}

    return {"range": "UNSPECIFIED"}

# ========= HELPERS (CATEGORIA) =========


def parse_budget_entities(msg: str) -> dict:
    m = msg.lower()

    amount = parse_amount(msg)
    category = determinar_categoria(msg)

    if amount is None:
        return {}

    return {
        "amount": amount,
        "category": category
    }

def parse_budget_category(msg: str) -> str | None:
    m = msg.lower()

    if any(x in m for x in ["alimentacao", "alimentação", "comida", "mercado", "ifood", "restaurante", "lanche"]):
        return "ALIMENTACAO"
    if any(x in m for x in ["uber", "transporte", "gasolina", "onibus", "ônibus", "metro"]):
        return "TRANSPORTE"
    if any(x in m for x in ["aluguel", "condominio", "condomínio", "moradia"]):
        return "MORADIA"
    if any(x in m for x in ["netflix", "cinema", "lazer", "show"]):
        return "LAZER"
    if any(x in m for x in ["farmacia", "farmácia", "remedio", "remédio", "saude", "saúde", "medico", "médico"]):
        return "SAUDE"
    if any(x in m for x in ["curso", "faculdade", "educacao", "educação", "escola", "livro"]):
        return "EDUCACAO"

    return None

# ========= HELPERS (INVESTIMENTO) =========

def parse_ticker(msg: str) -> str | None:
    up = msg.upper()

    # 🔒 NÃO tratar "TOP10", "TOP 10" etc como ticker
    if re.search(r"\bTOP\s*\d{1,2}\b", up):
        return None

    # ticker bolsa BR (PETR4, MXRF11, B3SA3 etc)
    m = re.search(r"\b[A-Z]{2,6}\d{1,2}\b", up)
    if m:
        return m.group(0)

    # cripto simples
    m2 = re.search(r"\b(BTC|ETH|SOL|XRP|ADA)\b", up)
    return m2.group(1) if m2 else None

def infer_classe_ativo(ticker: str, msg: str) -> str:
    t = ticker.upper()
    m = msg.lower()
    if "fii" in m or "fiis" in m or t.endswith("11"):
        return "FII"
    if t.isalpha():  # BTC, ETH
        return "CRIPTO"
    return "ACAO"

def normalize(s: str) -> str:
    s = s.lower().strip()
    return "".join(
        c for c in unicodedata.normalize("NFD", s)
        if unicodedata.category(c) != "Mn"
    )

def is_portfolio_query(msg: str) -> bool:
    m = normalize(msg)
    portfolio_words = [
        "carteira", "portfolio", "portfolio de investimentos", "investimentos",
        "relatorio da carteira", "relatorio de carteira", "relatorio carteira",
        "relatorio de acoes","relatorio de ações","relatorio de ação", "relatorio de acao", "relatorio de fii", "relatorio de fiis",
        "relatorio de cripto", "relatorio de criptos", "minha carteira"
    ]
    return any(w in m for w in portfolio_words)

def is_portfolio_graph_query(msg: str) -> bool:
    m = normalize(msg)
    keys = ["grafico", "evolucao", "linha", "serie"]
    return "carteira" in m and any(k in m for k in keys)

def _safe_upper(s: Optional[str]) -> Optional[str]:
    if s is None:
        return None
    s = str(s).strip()
    return s.upper() if s else None

def is_asset_detail_query(msg: str) -> bool:
    m = normalize(msg)

    # palavras-chave (variações)
    keys = [
        "performance", "desempenho", "detalhe", "detalhar", "detalhes",
        "como foi", "como anda", "como ta", "como está", "evolucao", "evolução",
        "resultado", "rendendo", "rendeu", "rentabilidade", "variacao", "variação",
        "o que aconteceu", "porque caiu", "por que caiu", "porque subiu", "por que subiu"
    ]

    # precisa ter ticker (PETR4, MXRF11, BTC, ETH etc.)
    return any(k in m for k in keys) and (parse_ticker(msg) is not None)

def is_portfolio_top_query(msg: str) -> bool:
    m = normalize(msg)

    # pega "top10", "top 10", "top:10"
    if re.search(r"\btop\s*:?\s*\d{1,2}\b", m) or re.search(r"\btop\d{1,2}\b", m):
        return True

    # ranking/ melhores / piores, mesmo sem falar "carteira"
    keys = [
        "top ativos", "melhores ativos", "piores ativos",
        "ranking", "ranking do mes", "ranking do mês",
        "top performance", "top desempenho",
        "melhor ativo", "pior ativo"
    ]
    return any(k in m for k in keys)



def parse_top_n(msg: str) -> int | None:
    m = msg.lower()

    # "top 10", "top10", "top: 10", "top-10"
    m1 = (
        re.search(r"\btop\s*[:\-]?\s*(\d{1,2})\b", m) or
        re.search(r"\btop(\d{1,2})\b", m)
    )
    if not m1:
        return None

    n = int(m1.group(1))
    return max(1, min(n, 20))

def infer_classe_from_text(msg: str) -> str | None:
    m = normalize(msg)
    if "fii" in m or "fiis" in m:
        return "FII"
    if "cripto" in m or "bitcoin" in m or "btc" in m or "ethereum" in m or "eth" in m:
        return "CRIPTO"
    if "acao" in m or "acoes" in m or "ações" in m:
        return "ACAO"
    return None


# ========= ENDPOINTS =========

@app.get("/")
def health():
    return {"status": "ok", "service": "sexta-feira-ia"}

@app.post("/ia/transacoes/interpretar", response_model=TransacaoInterpretada)
def interpretar_transacao(request: MensagemRequest):
    mensagem = request.mensagem

    valor = extrair_valor_inteligente(mensagem)
    if valor is None:
        valor = extrair_valor(mensagem)
    if valor is None:
        valor = Decimal("0.00")

    tipo = determinar_tipo(mensagem)
    categoria_smart, item = classificar_categoria_inteligente(mensagem)
    if categoria_smart != "OUTROS" or item != "geral":
        categoria = categoria_smart
    else:
        categoria = determinar_categoria(mensagem)

    return TransacaoInterpretada(
        valor=valor,
        data=interpretar_data(mensagem),
        tipo=tipo,
        categoria=categoria,
        descricao=mensagem
    )


@app.post("/ia/router", response_model=RouterResponse)
def router(request: MensagemRequest):
    t_inicio = time.perf_counter()
    resp = _processar_router(request)
    duracao_ms = (time.perf_counter() - t_inicio) * 1000
    msg_redigida = mascarar_dados_sensiveis(request.mensagem)
    logger.info(
        f"[NLU-DIAGNOSTIC] Input='{msg_redigida}' | Intent='{resp.intent}' | Lang='{resp.lang}' | Duration={duracao_ms:.2f}ms | Entities={resp.entities}"
    )
    return resp

def _processar_router(request: MensagemRequest) -> RouterResponse:
    # 1. Roteamento Semantico via LLM (quando habilitada, configurada e responsiva)
    resultado_llm = tentar_roteamento_llm(request)
    if resultado_llm is not None:
        return resultado_llm

    # 2. Pipeline de NLU em Linguagem Natural Avancada (Portugues Coloquial & Ingles)
    mensagem = request.mensagem
    lang = detectar_idioma(mensagem)

    nlu_resultado = analisar_mensagem(mensagem, request.historico)
    if nlu_resultado.get("intent") != "UNKNOWN":
        nlu_intent = nlu_resultado["intent"]
        nlu_entities = dict(nlu_resultado.get("entities", {}))
        if "data" not in nlu_entities:
            nlu_entities["data"] = str(interpretar_data(mensagem))
        if nlu_resultado.get("needs_clarification") and "clarification_prompt" in nlu_resultado:
            nlu_entities["clarification_prompt"] = nlu_resultado["clarification_prompt"]
        return RouterResponse(intent=nlu_intent, entities=nlu_entities, lang=lang)

    # 3. Fallback Deterministico por Expressoes Regulares e Palavras-Chave
    intent = detectar_intencao(mensagem)

    m = mensagem  # texto original
    mm = normalize(m)

    entities: Dict[str, Any] = {}
    entities["data"] = str(interpretar_data(m))

    # ========= PRIORIDADE ABSOLUTA: GRÁFICO =========
    if is_portfolio_graph_query(mensagem):
        entities = {}
        entities["data"] = str(interpretar_data(mensagem))
        entities.update(detectar_timeframe(mensagem))

        ticker = parse_ticker(mensagem)
        if ticker:
            entities["ticker"] = ticker
            entities["classe"] = infer_classe_ativo(ticker, mensagem)

        return RouterResponse(
            intent="ASK_PORTFOLIO_GRAPH",
            entities=entities,
            lang=lang
        )

    # ===== PRIORIDADE 2: RANKING / DETALHE / RELATÓRIO =====
    if is_portfolio_top_query(m) or is_portfolio_query(m) or is_asset_detail_query(m):
        if is_portfolio_top_query(m):
            intent = "ASK_PORTFOLIO_TOP"
        elif is_asset_detail_query(m):
            intent = "ASK_PORTFOLIO_ASSET_DETAIL"
        else:
            intent = "ASK_PORTFOLIO_REPORT"

        entities.update(detectar_timeframe(m))

        # top_n só pro ranking
        if intent == "ASK_PORTFOLIO_TOP":
            top_n = parse_top_n(m)
            if top_n:
                entities["top_n"] = top_n

        # ticker/classe (se houver)
        ticker = parse_ticker(m)
        if ticker:
            entities["ticker"] = ticker
            entities["classe"] = infer_classe_ativo(ticker, m)

        return RouterResponse(intent=intent, entities=entities, lang=lang)

    # ===== AJUSTE EXTRA DE TIMEFRAME (sem RETURN quebrado) =====
    if "do mes" in mm:
        entities["range"] = "THIS_MONTH"
    elif "do ultimo mes" in mm or "do ultimo mês" in mm:
        entities["range"] = "LAST_MONTH"
    elif "da semana" in mm:
        entities["range"] = "THIS_WEEK"
    else:
        entities.update(detectar_timeframe(m))

    # ===== TRANSACOES =====
    if intent in ["ADD_TRANSACTION", "DELETE_TRANSACTION"]:
        t = interpretar_transacao(request)
        entities.update({
            "valor": float(t.valor),
            "tipo": t.tipo,
            "categoria": t.categoria,
            "descricao": t.descricao,
            "data": str(t.data)
        })
        return RouterResponse(intent=intent, entities=entities, lang=lang)

    # ===== ORÇAMENTO (SET_BUDGET / ASK_BUDGET_STATUS) =====
    if intent == "SET_BUDGET":
        entities.update(parse_budget_entities(m))
        return RouterResponse(intent=intent, entities=entities, lang=lang)

    if intent == "ASK_BUDGET_STATUS":
        cat = parse_budget_category(m)
        if cat:
            entities["category"] = cat
        return RouterResponse(intent=intent, entities=entities, lang=lang)

    # ===== CONVERSAO =====
    if intent == "CONVERT_CURRENCY":
        conv = parse_convert_entities(m)
        entities.update(conv)
        return RouterResponse(intent=intent, entities=entities, lang=lang)

    # ===== CARTEIRA (movimentos) =====
    if intent in ["ADD_HOLDING_QTY", "SELL_HOLDING_QTY", "ADD_HOLDING_VALUE", "ASK_PORTFOLIO_REPORT"]:
        ticker = parse_ticker(m)
        if ticker:
            entities["ticker"] = ticker
            entities["classe"] = infer_classe_ativo(ticker, m)

        amt = parse_amount(m)
        if amt is not None:
            if intent == "ADD_HOLDING_VALUE":
                entities["value_brl"] = amt
            else:
                entities["qty"] = amt

        if intent == "ASK_PORTFOLIO_REPORT":
            entities.update(detectar_timeframe(m))

        return RouterResponse(intent=intent, entities=entities, lang=lang)

    # ===== RESUMO POR PERIODO =====
    if intent == "ASK_PERIOD_SUMMARY":
        entities.update(detectar_timeframe(m))
        return RouterResponse(intent=intent, entities=entities, lang=lang)

    # ===== PERSONALIDADE =====
    if intent == "SET_PERSONALITY":
        entities["personality"] = parse_personality(m)
        return RouterResponse(intent=intent, entities=entities, lang=lang)

    return RouterResponse(intent=intent, entities=entities, lang=lang)

@app.post("/ia/charts/gastos-por-categoria")
def chart_gastos_por_categoria(req: ChartCategoriaRequest):

    categorias = [i.categoria for i in req.items]
    totais = [i.total for i in req.items]

    fig = Figure(figsize=(9, 4.5))
    ax = fig.subplots()
    ax.bar(categorias, totais)
    ax.set_title(req.titulo)
    ax.set_ylabel("R$")
    ax.tick_params(axis="x", rotation=30)
    for label in ax.get_xticklabels():
        label.set_horizontalalignment("right")
    fig.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=150)
    buf.seek(0)

    return StreamingResponse(buf, media_type="image/png")

@app.post("/ia/charts/orcamento-vs-gasto")
def chart_orcamento_vs_gasto(req: ChartBudgetRequest):

    categorias = [i.categoria for i in req.items]
    orcamentos = [i.orcamento for i in req.items]
    gastos = [i.gasto for i in req.items]

    fig = Figure(figsize=(10, 4.8))
    ax = fig.subplots()

    x = list(range(len(categorias)))
    width = 0.40

    ax.bar([i - width/2 for i in x], orcamentos, width=width, label="Orçamento")
    ax.bar([i + width/2 for i in x], gastos, width=width, label="Gasto")

    ax.set_title(req.titulo)
    ax.set_ylabel("R$")
    ax.set_xticks(x)
    ax.set_xticklabels(categorias, rotation=30, ha="right")
    if categorias:
        ax.legend()
    fig.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=150)
    buf.seek(0)

    return StreamingResponse(buf, media_type="image/png")

@app.post("/ia/charts/serie-linha")
def chart_serie_linha(req: ChartSerieRequest):

    labels = [i.label for i in req.items]
    values = [i.value for i in req.items]

    fig = Figure(figsize=(10, 4.8))
    ax = fig.subplots()
    ax.plot(labels, values, marker="o")
    ax.set_title(req.titulo)
    ax.set_ylabel(req.y_label)

    # se tiver muitos pontos, não deixa ilegível
    if len(labels) > 15:
        step = max(1, len(labels)//10)
        indices = list(range(0, len(labels), step))
        ax.set_xticks(indices)
        ax.set_xticklabels([labels[i] for i in indices], rotation=30, ha="right")
    else:
        ax.tick_params(axis="x", rotation=30)
        for label in ax.get_xticklabels():
            label.set_horizontalalignment("right")

    fig.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=150)
    buf.seek(0)

    return StreamingResponse(buf, media_type="image/png")
