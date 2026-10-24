import json
import os
import re
import unicodedata
from datetime import date, timedelta
from decimal import Decimal
from typing import Any, Dict, Optional, Tuple, List

# Carrega base de vocabulário
DATA_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data", "vocabulario_financeiro.json")

def carregar_vocabulario() -> Dict[str, Any]:
    try:
        with open(DATA_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return {
            "versao": "1.0.0-fallback",
            "termos_monetarios_coloquiais": {
                "quinhentao": 500, "quinhentão": 500,
                "barao": 1000, "barão": 1000,
                "conto": 1, "contos": 1, "pau": 1, "paus": 1
            },
            "numeros_extenso": {
                "um": 1, "dois": 2, "tres": 3, "três": 3, "quatro": 4, "cinco": 5,
                "seis": 6, "sete": 7, "oito": 8, "nove": 9, "dez": 10, "vinte": 20,
                "trinta": 30, "quarenta": 40, "cinquenta": 50, "sessenta": 60,
                "setenta": 70, "oitenta": 80, "noventa": 90, "cem": 100, "cento": 100,
                "duzentos": 200, "trezentos": 300, "quatrocentos": 400, "quinhentos": 500,
                "mil": 1000
            }
        }

VOCABULARIO = carregar_vocabulario()

def normalizar_texto(texto: str) -> str:
    s = texto.lower().strip()
    return "".join(
        c for c in unicodedata.normalize("NFD", s)
        if unicodedata.category(c) != "Mn"
    )

def _tem_palavra(palavras: List[str], texto: str) -> bool:
    """Verifica se alguma palavra da lista existe como palavra inteira no texto."""
    for p in palavras:
        if re.search(r"\b" + re.escape(p) + r"\b", texto):
            return True
    return False

def converter_extenso_para_numero(texto: str) -> Optional[Decimal]:
    """
    Converte expressões numéricas em português (ex: "dois mil", "cento e cinquenta",
    "quinhentão", "80 conto", "dois mil e quinhentos") para Decimal.
    Evita falsos positivos com artigos indefinidos como 'um dinheiro', 'uma grana'.
    """
    t_low = normalizar_texto(texto)

    # Ignora artigos indefinidos seguidos de substantivos monetários genéricos
    if re.search(r"\b(um|uma)\s+(dinheiro|grana|valor|trocado|pouco|renda|quantia)\b", t_low):
        t_low = re.sub(r"\b(um|uma)\s+(dinheiro|grana|valor|trocado|pouco|renda|quantia)\b", " ", t_low)

    # 1. Gírias monetárias diretas de valor fixo
    if "quinhentao" in t_low:
        return Decimal("500.00")
    if "barao" in t_low or "baroes" in t_low:
        m_bar = re.search(r"\b(um|dois|tres|quatro|cinco|\d+)\s+bar[a-z]+", t_low)
        if m_bar:
            mult_str = m_bar.group(1)
            num_map = VOCABULARIO.get("numeros_extenso", {})
            mult = num_map.get(mult_str, int(mult_str) if mult_str.isdigit() else 1)
            return Decimal(str(mult * 1000))
        return Decimal("1000.00")

    # 2. Número antes de "conto" ou "pau" (ex: "80 conto", "cinquenta pau")
    m_conto = re.search(r"(\d+)\s+(?:conto|contos|pau|paus|reais|real)\b", t_low)
    if m_conto:
        try:
            return Decimal(m_conto.group(1))
        except Exception:
            pass

    # 3. Números por extenso combinados (ex: "dois mil e quinhentos", "dois mil")
    numeros = VOCABULARIO.get("numeros_extenso", {})
    tokens = re.findall(r"\b[a-z]+\b", t_low)

    total = 0
    corrente = 0
    encontrou_numero = False

    for tok in tokens:
        if tok == "e":
            continue
        if tok in numeros:
            encontrou_numero = True
            valor = numeros[tok]
            if valor == 1000:
                total += (corrente if corrente > 0 else 1) * 1000
                corrente = 0
            elif valor >= 100:
                corrente += valor
            else:
                corrente += valor
        elif tok in ("conto", "contos", "pau", "paus", "reais", "real", "bucks", "dollars"):
            if encontrou_numero:
                break
        elif encontrou_numero and tok not in ("de", "do", "da"):
            break

    total += corrente
    if encontrou_numero and total > 0:
        return Decimal(str(total))

    return None

def _converter_token_ou_extenso(token: str) -> Optional[Decimal]:
    """Converte um token numérico (dígitos ou extenso) para Decimal."""
    if not token:
        return None
    token = token.strip().lower()
    t_clean = token.replace(",", ".")
    try:
        return Decimal(t_clean)
    except Exception:
        pass
    val = converter_extenso_para_numero(token)
    if val is not None:
        return val
    nums = VOCABULARIO.get("numeros_extenso", {})
    if token in nums:
        return Decimal(str(nums[token]))
    return None

def extrair_valor_inteligente(mensagem: str) -> Optional[Decimal]:
    """
    Extrai o valor monetário com máxima cobertura de formatos e coloquialismos.
    """
    m_low = normalizar_texto(mensagem)

    # 0. Checa padrão de quantidade * preço unitário: "Vendi 5 ações por 200 reais cada", "Vendi cinco ações por duzentos reais cada"
    m_qtd_preco = re.search(
        r"(?:vendi|venda)?\s*([a-z0-9]+)\s+(?:ações|acoes|cotas|ativos|papeis|papéis)(?:\s+(?:da|de|do)\s+[A-Za-z0-9]+)?\s+(?:por|a)\s+([a-z0-9.,\s]+?)(?:\s+(?:reais|cada)|\s*$)",
        m_low
    )
    if m_qtd_preco:
        q_val = _converter_token_ou_extenso(m_qtd_preco.group(1))
        u_val = _converter_token_ou_extenso(m_qtd_preco.group(2))
        if q_val is not None and u_val is not None and q_val > Decimal("0") and u_val > Decimal("0"):
            return q_val * u_val

    # Trata 'um dinheiro', 'uma grana' para não virar 1.0
    limpa_artigos = re.sub(r"\b(um|uma)\s+(dinheiro|grana|valor|trocado|pouco|renda|quantia)\b", " ", m_low)

    # 1. R$ explícito ou número seguido de moeda
    m_brl = re.search(r"r\$\s*(\d+(?:[.,]\d+)?)", mensagem, flags=re.IGNORECASE)
    if not m_brl:
        m_brl = re.search(r"(\d+(?:[.,]\d+)?)\s*(?:reais|real|conto|contos|pau|paus|bucks|dollars|euros)\b", limpa_artigos, flags=re.IGNORECASE)
    if m_brl:
        try:
            val_str = m_brl.group(1).replace(",", ".")
            return Decimal(val_str)
        except Exception:
            pass

    # 2. Conversão por extenso / gírias
    num_ext = converter_extenso_para_numero(mensagem)
    if num_ext is not None:
        return num_ext

    # 3. Número solto (limpando datas, tickers, horas e anos)
    limpa = re.sub(r"\b\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?\b", " ", mensagem)
    limpa = re.sub(r"\bdia\s+\d{1,2}\b", " ", limpa, flags=re.IGNORECASE)
    limpa = re.sub(r"\b\d{1,2}h(?:\d{2})?\b", " ", limpa, flags=re.IGNORECASE)
    limpa = re.sub(r"\b[A-Za-z]{4}\d{1,2}\b", " ", limpa)
    limpa = re.sub(r"\b(um|uma)\s+(dinheiro|grana|valor|trocado|pouco)\b", " ", limpa, flags=re.IGNORECASE)

    m_geral = re.search(r"\b(\d+(?:[.,]\d+)?)\b", limpa)
    if m_geral:
        try:
            return Decimal(m_geral.group(1).replace(",", "."))
        except Exception:
            return None

    return None

def classificar_categoria_inteligente(mensagem: str) -> Tuple[str, str]:
    """
    Retorna (categoria_enum, item_especifico).
    Garante mapeamento para as categorias Java:
    ALIMENTACAO, TRANSPORTE, MORADIA, LAZER, SAUDE, EDUCACAO, CONTAS_FIXAS, INVESTIMENTOS, OUTROS
    """
    m = normalizar_texto(mensagem)

    # Investimentos
    palavras_invest = ["investi", "investir", "investimento", "investimentos", "aporte", "aportes", "aplicar", "apliquei", "tesouro", "fii", "fiis", "cripto", "btc", "eth", "acao", "acoes", "bolsa", "holding"]
    if _tem_palavra(palavras_invest, m):
        return "INVESTIMENTOS", "investimentos"

    # Alimentação
    palavras_alim = [
        "almoco", "jantar", "comida", "mercado", "supermercado", "lanche",
        "restaurante", "ifood", "cafe", "padaria", "pizza", "bebida", "refeicao",
        "lunch", "dinner", "food", "groceries", "restaurant", "alimentacao"
    ]
    if _tem_palavra(palavras_alim, m):
        for item in ["almoco", "jantar", "mercado", "lanche", "restaurante", "ifood", "pizza", "cafe", "lunch", "dinner"]:
            if item in m:
                return "ALIMENTACAO", item
        return "ALIMENTACAO", "alimentação"

    # Transporte
    palavras_transp = [
        "uber", "99", "taxi", "gasolina", "combustivel", "etanol", "onibus", "metro",
        "pedagio", "passagem", "estacionamento", "bus", "subway"
    ]
    if _tem_palavra(palavras_transp, m) or re.search(r"\bgas\b", m) or re.search(r"\bfuel\b", m):
        for item in ["uber", "99", "gasolina", "onibus", "metro", "estacionamento"]:
            if item in m:
                return "TRANSPORTE", item
        return "TRANSPORTE", "transporte"

    # Moradia & Contas Fixas
    palavras_moradia = ["aluguel", "condominio", "iptu", "rent"]
    if _tem_palavra(palavras_moradia, m):
        return "MORADIA", "moradia"

    palavras_contas = ["energia", "internet", "luz", "agua", "eletricidade"]
    if _tem_palavra(palavras_contas, m) or "conta de luz" in m or "conta de agua" in m:
        return "CONTAS_FIXAS", "contas fixas"

    # Saúde
    palavras_saude = ["farmacia", "remedio", "medico", "dentista", "consulta", "exame", "hospital", "academia", "pharmacy", "doctor"]
    if _tem_palavra(palavras_saude, m):
        return "SAUDE", "saúde"

    # Educação
    palavras_educ = ["faculdade", "curso", "livro", "escola", "mensalidade", "book", "course", "college"]
    if _tem_palavra(palavras_educ, m):
        return "EDUCACAO", "educação"

    # Lazer & Vestuário
    palavras_vest = ["roupa", "roupas", "tenis", "sapato", "camisa", "calca", "shopping", "vestido", "clothes", "shoes"]
    if _tem_palavra(palavras_vest, m):
        return "OUTROS", "vestuário"

    palavras_lazer = ["cinema", "show", "teatro", "viagem", "hotel", "festa", "steam", "netflix", "spotify", "games"]
    if _tem_palavra(palavras_lazer, m):
        return "LAZER", "lazer"

    return "OUTROS", "geral"

def analisar_mensagem(mensagem: str, historico: Optional[List[Dict[str, Any]]] = None) -> Dict[str, Any]:
    """
    Executa o pipeline completo de NLU em linguagem natural (Português e Inglês).
    Produz intenção, entidades extraídas, indicação de ambiguidade e pergunta de esclarecimento.
    """
    m_norm = normalizar_texto(mensagem)

    # 0. Se contiver ticker específico de ativo de carteira (PETR4, MXRF11, BTC, ETH...),
    # delega para os roteadores especializados de carteira e holdings do main.py
    if re.search(r"\b[A-Za-z]{4}\d{1,2}\b", mensagem) or re.search(r"\b(btc|eth|bitcoin|ethereum)\b", m_norm):
        return {
            "intent": "UNKNOWN",
            "entities": {},
            "needs_clarification": False
        }

    # 1. Detecção de Desfazer / Undo
    termos_undo = VOCABULARIO.get("intencoes", {}).get("UNDO_LAST", [])
    if any(u in m_norm for u in termos_undo) or "desfaz" in m_norm or "desfazer" in m_norm or "cancela o ultimo" in m_norm or "undo last" in m_norm:
        return {
            "intent": "DELETE_TRANSACTION",
            "entities": {"action": "undo_last"},
            "needs_clarification": False
        }

    # 2. Detecção de Correção Conversacional (ex: "não, quis dizer 50, não 500")
    termos_corr = VOCABULARIO.get("intencoes", {}).get("CORRECTION", [])
    if any(c in m_norm for c in termos_corr) or m_norm.startswith("nao,") or m_norm.startswith("nao "):
        m_corr = re.search(r"(?:quis dizer|foi|era|errei)\s+(\d+(?:[.,]\d+)?)", m_norm)
        if m_corr:
            novo_valor = Decimal(m_corr.group(1).replace(",", "."))
            return {
                "intent": "CORRECTION",
                "entities": {"novo_valor": float(novo_valor)},
                "needs_clarification": False
            }

    # 3. Detecção de Perguntas / Consultas Financeiras (Prioritário sobre registro de gastos!)
    # Ex: "Quanto eu gastei com alimentação este mês?", "How much did I spend this month"
    if any(q in m_norm for q in [
        "quanto gastei", "quanto eu gastei", "gastei quanto", "quanto foi",
        "how much did i spend", "how much i spent", "how much spent"
    ]):
        cat, item = classificar_categoria_inteligente(mensagem)
        entities: Dict[str, Any] = {"range": "THIS_MONTH"}
        if "hoje" in m_norm or "today" in m_norm:
            entities["range"] = "TODAY"
        elif "ontem" in m_norm or "yesterday" in m_norm:
            entities["range"] = "YESTERDAY"
        elif "semana" in m_norm or "week" in m_norm:
            entities["range"] = "THIS_WEEK"
        elif "mes passado" in m_norm or "last month" in m_norm:
            entities["range"] = "LAST_MONTH"

        if cat != "OUTROS" or item != "geral":
            entities["category"] = cat

        return {
            "intent": "ASK_PERIOD_SUMMARY",
            "entities": entities,
            "needs_clarification": False
        }

    # 4. Detecção de Gráficos e Visualizações
    termos_grafico = VOCABULARIO.get("intencoes", {}).get("ASK_CHARTS", [])
    if any(g in m_norm for g in termos_grafico) or "grafico" in m_norm or "graficos" in m_norm or "show chart" in m_norm:
        if "carteira" in m_norm or "investimento" in m_norm or "acoes" in m_norm or "patrimonio" in m_norm or "portfolio" in m_norm:
            return {
                "intent": "ASK_PORTFOLIO_CHART",
                "entities": {"tipo": "carteira"},
                "needs_clarification": False
            }
        return {
            "intent": "ASK_FINANCES_CHART",
            "entities": {"tipo": "despesas_categoria"},
            "needs_clarification": False
        }

    # 5. Detecção de Visão Geral das Finanças
    termos_financas = VOCABULARIO.get("intencoes", {}).get("ASK_FINANCES_OVERVIEW", [])
    if any(f in m_norm for f in termos_financas) or "minhas financas" in m_norm or "how are my finances" in m_norm:
        return {
            "intent": "ASK_FINANCES_OVERVIEW",
            "entities": {},
            "needs_clarification": False
        }

    # 6. Perguntas sobre Investimentos ("Vendi algumas ações. Quanto recebi?")
    if ("vendi" in m_norm or "venda" in m_norm) and any(w in m_norm for w in ["quanto recebi", "quanto ganhei", "quanto deu"]):
        return {
            "intent": "ASK_PORTFOLIO_REPORT",
            "entities": {"tipo": "rendimentos_venda"},
            "needs_clarification": False
        }

    # 6.1. Venda de Ativos / Ações com quantidade e preço unitário
    # Ex: "Vendi 5 ações por 200 reais cada", "Vendi cinco ações por duzentos reais cada", "Vendi 10 cotas a 50 reais"
    m_venda_qtd = re.search(
        r"(?:vendi|venda)\s+([a-z0-9]+)\s+(?:ações|acoes|cotas|ativos|papeis|papéis)(?:\s+(?:da|de|do)\s+([A-Za-z0-9]+))?\s+(?:por|a)\s+([a-z0-9.,\s]+?)(?:\s+(?:reais|cada)|\s*$)",
        m_norm
    )
    if m_venda_qtd:
        qty_dec = _converter_token_ou_extenso(m_venda_qtd.group(1))
        ticker_match = m_venda_qtd.group(2)
        unit_price = _converter_token_ou_extenso(m_venda_qtd.group(3))
        if qty_dec is not None and unit_price is not None and qty_dec > Decimal("0") and unit_price > Decimal("0"):
            total_val = qty_dec * unit_price

            entities = {
                "valor": float(total_val),
                "qty": float(qty_dec),
                "unit_price": float(unit_price),
                "tipo": "RECEITA",
                "categoria": "INVESTIMENTOS",
                "descricao": mensagem,
                "item_detalhe": "venda de ações"
            }
            if ticker_match:
                entities["ticker"] = ticker_match.upper()

            return {
                "intent": "ADD_TRANSACTION",
                "entities": entities,
                "needs_clarification": False
            }

    # 7. Aportes e Investimentos Genéricos ("Apliquei 300 reais em ações")
    if any(a in m_norm for a in ["apliquei", "aportei", "investi"]):
        valor = extrair_valor_inteligente(mensagem)
        if valor is not None and valor > Decimal("0.00"):
            return {
                "intent": "ADD_TRANSACTION",
                "entities": {
                    "valor": float(valor),
                    "tipo": "DESPESA",
                    "categoria": "INVESTIMENTOS",
                    "descricao": mensagem,
                    "item_detalhe": "investimentos"
                },
                "needs_clarification": False
            }
        else:
            return {
                "intent": "CLARIFICATION_REQUIRED",
                "entities": {"tipo": "INVESTIMENTO", "categoria": "INVESTIMENTOS"},
                "needs_clarification": True,
                "clarification_prompt": "Entendi que você fez uma aplicação financeira, mas não identifiquei o valor. Quanto foi?"
            }

    # 8. Detecção de Despesas e Receitas Coloquiais
    termos_despesa = VOCABULARIO.get("intencoes", {}).get("ADD_TRANSACTION_DESPESA", [])
    termos_receita = VOCABULARIO.get("intencoes", {}).get("ADD_TRANSACTION_RECEITA", [])

    is_despesa = any(d in m_norm for d in termos_despesa)
    is_receita = any(r in m_norm for r in termos_receita)

    # "Saiu 120 da minha conta" / "Caiu um dinheiro na conta"
    if "saiu" in m_norm or "debitou" in m_norm or "descontou" in m_norm or "torrei" in m_norm:
        is_despesa = True
    if "caiu" in m_norm or "pingou" in m_norm or "entrou" in m_norm:
        is_receita = True

    if is_despesa or is_receita:
        valor = extrair_valor_inteligente(mensagem)
        categoria, item_detalhe = classificar_categoria_inteligente(mensagem)
        tipo = "RECEITA" if is_receita and not is_despesa else "DESPESA"

        # Proteção de Ambiguidade: Se não conseguiu identificar o valor numérico com segurança
        if valor is None or valor <= Decimal("0.00"):
            prompt_item = f" com {item_detalhe}" if item_detalhe != "geral" else ""
            if tipo == "RECEITA":
                prompt = "Entendi que entrou um dinheiro na sua conta, mas não identifiquei o valor exato. Quanto foi?"
            else:
                prompt = f"Entendi que você gastou{prompt_item}, mas não identifiquei o valor exato. Quanto foi?"

            return {
                "intent": "CLARIFICATION_REQUIRED",
                "entities": {
                    "tipo": tipo,
                    "categoria": categoria,
                    "item": item_detalhe
                },
                "needs_clarification": True,
                "clarification_prompt": prompt
            }

        return {
            "intent": "ADD_TRANSACTION",
            "entities": {
                "valor": float(valor),
                "tipo": tipo,
                "categoria": categoria,
                "descricao": mensagem,
                "item_detalhe": item_detalhe
            },
            "needs_clarification": False
        }

    return {
        "intent": "UNKNOWN",
        "entities": {},
        "needs_clarification": False
    }
