import React, { useEffect, useState } from 'react';
import { ResumoFinanceiro, StatusOrcamento } from '../../types';
import { api } from '../../services/api';
import { X, RefreshCw, Wallet, ArrowDownRight, ArrowUpRight, CheckCircle2 } from 'lucide-react';

interface HudSidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const HudSidebar: React.FC<HudSidebarProps> = ({ isOpen, onClose }) => {
  const [resumo, setResumo] = useState<ResumoFinanceiro | null>(null);
  const [orcamentos, setOrcamentos] = useState<StatusOrcamento[]>([]);
  const [loading, setLoading] = useState(false);

  const carregarDados = async () => {
    setLoading(true);
    try {
      const [resumoData, orcamentosData] = await Promise.all([
        api.finance.getMonthSummary().catch(() => null),
        api.finance.getBudgetOverview().catch(() => []),
      ]);
      if (resumoData) setResumo(resumoData);
      setOrcamentos(orcamentosData || []);
    } catch {
      // Falha tratada graciosamente
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      carregarDados();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <aside className="fixed inset-y-0 right-0 w-80 sm:w-96 bg-void-950/95 border-l border-hologram-cyan/20 backdrop-blur-2xl z-40 flex flex-col shadow-2xl transition-all duration-300">
      {/* Header da Gaveta */}
      <div className="p-4 border-b border-hologram-cyan/15 flex items-center justify-between select-none">
        <div className="flex items-center gap-2">
          <Wallet className="w-5 h-5 text-hologram-cyan" />
          <h2 className="font-mono text-sm font-bold tracking-wider text-slate-100">
            TELEMETRIA // FINANCEIRA
          </h2>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={carregarDados}
            disabled={loading}
            className="p-1.5 rounded text-slate-400 hover:text-hologram-cyan hover:bg-slate-800/40 transition-colors"
            title="Atualizar"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-hologram-cyan' : ''}`} />
          </button>
          <button
            onClick={onClose}
            className="p-1.5 rounded text-slate-400 hover:text-rose-400 hover:bg-slate-800/40 transition-colors"
            title="Fechar"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Conteúdo com Scroll */}
      <div className="flex-1 overflow-y-auto p-4 space-y-6 custom-scrollbar">
        {/* Bloco 1: Balanço do Mês Atual */}
        <section className="space-y-3">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span>BALANÇO CONSOLIDADO</span>
            {resumo && <span>{String(resumo.mes).padStart(2, '0')}/{resumo.ano}</span>}
          </div>

          <div className="p-4 rounded-lg bg-void-900/80 border border-hologram-cyan/25 shadow-glow-cyan/10">
            <span className="text-[11px] font-mono text-slate-400">SALDO LÍQUIDO</span>
            <div className="mt-1 flex items-baseline gap-2">
              <span
                className={`font-mono text-2xl font-bold ${
                  (resumo?.saldo || 0) >= 0 ? 'text-emerald-400' : 'text-rose-400'
                }`}
              >
                R$ {(resumo?.saldo || 0).toFixed(2)}
              </span>
            </div>

            <div className="mt-4 grid grid-cols-2 gap-3 pt-3 border-t border-slate-800">
              <div className="flex items-center gap-2">
                <div className="p-1 rounded bg-emerald-500/10 text-emerald-400">
                  <ArrowUpRight className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-[10px] font-mono text-slate-400">RECEITAS</p>
                  <p className="text-xs font-mono font-semibold text-emerald-300">
                    R$ {(resumo?.totalReceitas || 0).toFixed(2)}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <div className="p-1 rounded bg-rose-500/10 text-rose-400">
                  <ArrowDownRight className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-[10px] font-mono text-slate-400">DESPESAS</p>
                  <p className="text-xs font-mono font-semibold text-rose-300">
                    R$ {(resumo?.totalDespesas || 0).toFixed(2)}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Bloco 2: Limites e Orçamentos */}
        <section className="space-y-3">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span>ORÇAMENTOS CATEGORIZADOS</span>
            <span>{orcamentos.length} LIMITES</span>
          </div>

          {orcamentos.length === 0 ? (
            <div className="p-4 rounded-lg bg-void-900/40 border border-slate-800 text-center text-xs font-mono text-slate-500">
              Nenhum limite orçamentário configurado para este mês.
            </div>
          ) : (
            <div className="space-y-2.5">
              {orcamentos.map((st) => {
                const perc = st.limite > 0 ? (st.gasto / st.limite) * 100 : 0;
                let colorClass = 'bg-hologram-cyan';
                let badge = 'NORMAL';
                if (st.estourado || perc > 100) {
                  colorClass = 'bg-rose-500 shadow-glow-red';
                  badge = 'ESTOURO';
                } else if (perc >= 90) {
                  colorClass = 'bg-amber-500 shadow-glow-gold';
                  badge = '90%+';
                } else if (perc >= 80) {
                  colorClass = 'bg-yellow-400';
                  badge = '80%+';
                }

                return (
                  <div
                    key={st.categoria}
                    className="p-3 rounded-lg bg-void-900/60 border border-slate-800 hover:border-hologram-cyan/30 transition-colors"
                  >
                    <div className="flex items-center justify-between text-xs mb-1.5">
                      <span className="font-medium text-slate-200 capitalize">
                        {st.categoria.toLowerCase().replace('_', ' ')}
                      </span>
                      <span
                        className={`font-mono text-[10px] px-1.5 py-0.2 rounded border ${
                          st.estourado
                            ? 'text-rose-400 border-rose-500/40 bg-rose-500/10'
                            : 'text-hologram-cyan border-hologram-cyan/30 bg-hologram-cyan/10'
                        }`}
                      >
                        {badge}
                      </span>
                    </div>

                    {/* Barra de Progresso */}
                    <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-300 ${colorClass}`}
                        style={{ width: `${Math.min(perc, 100)}%` }}
                      />
                    </div>

                    <div className="mt-2 flex items-center justify-between text-[11px] font-mono text-slate-400">
                      <span>R$ {st.gasto.toFixed(2)}</span>
                      <span>de R$ {st.limite.toFixed(2)}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {/* Bloco 3: Diretrizes de Uso */}
        <section className="p-3.5 rounded-lg bg-hologram-cyan/5 border border-hologram-cyan/15 text-xs text-slate-300 space-y-2">
          <div className="flex items-center gap-1.5 font-mono text-hologram-cyan text-[11px]">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>SINCRONIZAÇÃO EM TEMPO REAL</span>
          </div>
          <p className="text-[11px] leading-relaxed text-slate-400">
            Você pode pedir à assistente no chat: <em>"orçamento de alimentação 1200 reais"</em> ou <em>"gastei 45 no almoço"</em> para atualizar instantaneamente.
          </p>
        </section>
      </div>
    </aside>
  );
};
