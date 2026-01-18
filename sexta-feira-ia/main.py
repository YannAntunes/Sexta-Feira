from fastapi import FastAPI
from pydantic import BaseModel
from datetime import date
from decimal import Decimal
import re

app = FastAPI(title="SEXTA-FEIRA IA Service")


class MensagemRequest(BaseModel):
    mensagem: str


class TransacaoInterpretada(BaseModel):
    valor: Decimal
    data: date
    tipo: str          # "DESPESA" ou "RECEITA"
    categoria: str     # "ALIMENTACAO", "TRANSPORTE", etc.
    descricao: str


def extrair_valor(mensagem: str) -> Decimal | None:
    # Procura algo tipo 30, 45.50, 120.99 etc
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

    # ALIMENTAÇÃO
    if ("mercado" in m or "comida" in m or "lanche" in m or
        "alimentacao" in m or "alimentação" in m or
        "bebida" in m or "refri" in m or "restaurante" in m or "ifood" in m):
        return "ALIMENTACAO"

    # TRANSPORTE
    if "uber" in m or "ônibus" in m or "onibus" in m or "gasolina" in m or "metro" in m:
        return "TRANSPORTE"

    # MORADIA
    if "aluguel" in m or "condominio" in m or "condomínio" in m:
        return "MORADIA"

    # LAZER
    if "netflix" in m or "cinema" in m or "show" in m or "lazer" in m or "passeio" in m:
        return "LAZER"

    # SAÚDE
    if "farmacia" in m or "farmácia" in m or "remedio" in m or "remédio" in m or "médico" in m or "medico" in m:
        return "SAUDE"

    # EDUCAÇÃO
    if "faculdade" in m or "curso" in m or "livro" in m or "escola" in m:
        return "EDUCACAO"

    # CONTAS FIXAS
    if "conta de luz" in m or "conta de água" in m or "conta de agua" in m or "energia" in m or "internet" in m:
        return "CONTAS_FIXAS"

    # INVESTIMENTOS – agora com cuidado pra não pegar 'alimentação'
    import re
    if ( "investi" in m or "aporte" in m or "fii" in m or "cripto" in m or
         re.search(r"\bacao\b", m) or
         re.search(r"\bações\b", m) or
         re.search(r"\bacoes\b", m) or
         re.search(r"\betf\b", m) ):
        return "INVESTIMENTOS"

    return "OUTROS"



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
