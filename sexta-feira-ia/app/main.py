"""
Ponto de entrada legado/secundário do pacote app.
Exporta a aplicação oficial definida em sexta-feira-ia/main.py
para garantir ponto único de verdade e evitar divergências de código.
"""
import sys
from pathlib import Path

# Garante que o diretório raiz do módulo sexta-feira-ia esteja no sys.path
_parent_dir = str(Path(__file__).resolve().parent.parent)
if _parent_dir not in sys.path:
    sys.path.insert(0, _parent_dir)

from main import (  # noqa: F401, E402
    app,
    router,
    interpretar_transacao,
    health,
    chart_gastos_por_categoria,
    chart_orcamento_vs_gasto,
    chart_serie_linha,
    tentar_roteamento_llm,
    is_llm_enabled,
)
