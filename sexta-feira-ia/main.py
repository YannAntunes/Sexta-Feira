from fastapi import FastAPI
from pydantic import BaseModel
from datetime import date, timedelta
from decimal import Decimal
from typing import Dict, Any
import re
from typing import List
from fastapi.responses import StreamingResponse
import matplotlib.pyplot as plt
import io

app = FastAPI(title="SEXTA-FEIRA IA Service")


# ========= MODELS =========

class MensagemRequest(BaseModel):
    mensagem: str


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



# ========= HELPERS (TRANSAÇÃO) =========

def extrair_valor(mensagem: str) -> Decimal | None:
    padrao = r"\d+[.,]?\d*"
    match = re.search(padrao, mensagem.replace(",", "."))
    if match:
        try:
            return Decimal(match.group())
        except Exception:
            return None
    return None


def determinar_tipo(mensagem: str) -> str:
    m = mensagem.lower()
    if "recebi" in m or "salario" in m or "salário" in m or "ganhei" in m:
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


def interpretar_data(mensagem: str) -> date:
    m_low = mensagem.lower()
    hoje = date.today()

    if "anteontem" in m_low or "day before yesterday" in m_low:
        return hoje - timedelta(days=2)
    if "ontem" in m_low or "yesterday" in m_low:
        return hoje - timedelta(days=1)
    if "hoje" in m_low or "today" in m_low:
        return hoje

    return hoje


def detectar_intencao(mensagem: str) -> str:
    m_low = mensagem.lower()

    if any(x in m_low for x in [
        "apaga", "apagar", "remove", "remover",
        "deleta", "deletar", "exclui", "excluir",
        "undo", "delete"
    ]):
        return "DELETE_TRANSACTION"

    if any(x in m_low for x in [
        "gastei", "paguei", "comprei", "recebi", "ganhei",
        "spent", "paid", "bought", "received", "earned"
    ]):
        return "ADD_TRANSACTION"

    if any(x in m_low for x in [
        "relatório do mês", "relatorio do mes", "relatório mensal", "relatorio mensal",
        "me dá o relatório", "me da o relatorio", "me manda o relatório", "me manda o relatorio",
        "como eu tô esse mês", "como eu to esse mes", "como estou esse mês", "como estou esse mes",
        "monthly report", "month report", "full report", "report this month"
    ]):
        return "ASK_MONTH_REPORT"

    if any(x in m_low for x in [
        "meu orçamento", "meu orcamento",
        "budget status", "how is my budget",
        "como está meu orçamento", "como esta meu orcamento"
    ]):
        return "ASK_BUDGET_STATUS"

    if any(x in m_low for x in ["definir", "set", "criar", "colocar"]) and \
       any(x in m_low for x in ["orçamento", "orcamento", "budget"]):
        return "SET_BUDGET"

    if ("para" in m_low or "to" in m_low) and any(x in m_low for x in [
        "dólar", "dolar", "usd", "euro", "eur", "real", "brl", "convert", "converter", "converte"
    ]):
        return "CONVERT_CURRENCY"

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
    m = re.search(r"(\d+(?:[.,]\d+)?)", text)
    if not m:
        return None
    return float(m.group(1).replace(",", "."))

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


# ========= ENDPOINTS =========

@app.post("/ia/transacoes/interpretar", response_model=TransacaoInterpretada)
def interpretar_transacao(request: MensagemRequest):
    mensagem = request.mensagem

    valor = extrair_valor(mensagem)
    if valor is None:
        valor = Decimal("0.00")

    tipo = determinar_tipo(mensagem)
    categoria = determinar_categoria(mensagem)

    return TransacaoInterpretada(
        valor=valor,
        data=date.today(),
        tipo=tipo,
        categoria=categoria,
        descricao=mensagem
    )


@app.post("/ia/router", response_model=RouterResponse)
def router(request: MensagemRequest):
    mensagem = request.mensagem

    lang = detectar_idioma(mensagem)
    intent = detectar_intencao(mensagem)

    entities: Dict[str, Any] = {}

    entities["data"] = str(interpretar_data(mensagem))

    if intent in ["ADD_TRANSACTION", "DELETE_TRANSACTION"]:
        t = interpretar_transacao(request)
        entities.update({
            "valor": float(t.valor),
            "tipo": t.tipo,
            "categoria": t.categoria,
            "descricao": t.descricao,
            "data": str(t.data)
        })

    if intent == "CONVERT_CURRENCY":
        conv = parse_convert_entities(mensagem)
        entities.update(conv)

    return RouterResponse(intent=intent, entities=entities, lang=lang)

@app.post("/ia/charts/gastos-por-categoria")
def chart_gastos_por_categoria(req: ChartCategoriaRequest):

    categorias = [i.categoria for i in req.items]
    totais = [i.total for i in req.items]

    fig = plt.figure(figsize=(9, 4.5))
    plt.bar(categorias, totais)
    plt.title(req.titulo)
    plt.ylabel("R$")

    plt.xticks(rotation=30, ha="right")
    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format="png", dpi=150)
    plt.close(fig)
    buf.seek(0)

    return StreamingResponse(buf, media_type="image/png")

@app.post("/ia/charts/orcamento-vs-gasto")
def chart_orcamento_vs_gasto(req: ChartBudgetRequest):

    categorias = [i.categoria for i in req.items]
    orcamentos = [i.orcamento for i in req.items]
    gastos = [i.gasto for i in req.items]

    fig = plt.figure(figsize=(10, 4.8))

    x = range(len(categorias))
    width = 0.40

    plt.bar([i - width/2 for i in x], orcamentos, width=width, label="Orçamento")
    plt.bar([i + width/2 for i in x], gastos, width=width, label="Gasto")

    plt.title(req.titulo)
    plt.ylabel("R$")
    plt.xticks(list(x), categorias, rotation=30, ha="right")
    plt.legend()
    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format="png", dpi=150)
    plt.close(fig)
    buf.seek(0)

    return StreamingResponse(buf, media_type="image/png")

@app.post("/ia/charts/serie-linha")
def chart_serie_linha(req: ChartSerieRequest):

    labels = [i.label for i in req.items]
    values = [i.value for i in req.items]

    fig = plt.figure(figsize=(10, 4.8))
    plt.plot(labels, values, marker="o")
    plt.title(req.titulo)
    plt.ylabel(req.y_label)

    # se tiver muitos pontos, não deixa ilegível
    if len(labels) > 15:
        step = max(1, len(labels)//10)
        plt.xticks(range(0, len(labels), step), [labels[i] for i in range(0, len(labels), step)], rotation=30, ha="right")
    else:
        plt.xticks(rotation=30, ha="right")

    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format="png", dpi=150)
    plt.close(fig)
    buf.seek(0)

    return StreamingResponse(buf, media_type="image/png")
