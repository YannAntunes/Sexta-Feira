import React, { useEffect, useState } from 'react';
import { api } from '../../services/api';
import { BalancoMensal } from '../../types';
import {
  X,
  RefreshCw,
  PieChart,
  TrendingUp,
  TrendingDown,
  BarChart2,
  ShieldCheck,
  Scale,
  Image as ImageIcon,
  DollarSign,
  PiggyBank,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';

interface HudChartsModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialTab?: 'balanco' | 'categorias' | 'evolucao' | 'orcamento' | 'carteira';
  lastTransactionTimestamp?: number;
}

const CATEGORY_COLORS: Record<string, string> = {
  ALIMENTACAO: '#00f3ff',   // Cyan
  TRANSPORTE: '#3b82f6',    // Blue
  MORADIA: '#8b5cf6',       // Purple
  CONTAS_FIXAS: '#ec4899',  // Pink
  LAZER: '#f59e0b',         // Amber
  SAUDE: '#10b981',         // Emerald
  EDUCACAO: '#06b6d4',      // Teal
  INVESTIMENTOS: '#6366f1', // Indigo
  OUTROS: '#94a3b8',        // Slate
};

export const HudChartsModal: React.FC<HudChartsModalProps> = ({
  isOpen,
  onClose,
  initialTab = 'balanco',
  lastTransactionTimestamp,
}) => {
  const [activeTab, setActiveTab] = useState<'balanco' | 'categorias' | 'evolucao' | 'orcamento' | 'carteira'>(initialTab);
  const [categoriaTipo, setCategoriaTipo] = useState<'DESPESA' | 'RECEITA'>('DESPESA');
  const [loading, setLoading] = useState(false);
  const [viewPngMode, setViewPngMode] = useState(false);

  // Dados reais recebidos das APIs Spring Boot e Python
  const [balancoData, setBalancoData] = useState<BalancoMensal | null>(null);
  const [despesasCategorias, setDespesasCategorias] = useState<Record<string, number>>({});
  const [receitasCategorias, setReceitasCategorias] = useState<Record<string, number>>({});
  const [orcamentoData, setOrcamentoData] = useState<Array<{ categoria: string; orcamento: number; gasto: number }>>([]);
  const [serieData, setSerieData] = useState<Record<string, number>>({});
  const [pngUrl, setPngUrl] = useState<string | null>(null);

  useEffect(() => {
    if (initialTab) setActiveTab(initialTab);
  }, [initialTab]);

  const carregarDados = async () => {
    setLoading(true);
    try {
      const [balanco, despCat, recCat, orcList, serieMap] = await Promise.all([
        api.finance.getMonthlyBalance().catch(() => null),
        api.finance.getCategoryExpenses().catch(() => ({})),
        api.finance.getRevenueCategories().catch(() => ({})),
        api.finance.getBudgetVsExpenses().catch(() => []),
        api.finance.getDailyExpensesSeries().catch(() => ({})),
      ]);
      setBalancoData(balanco);
      setDespesasCategorias(despCat || {});
      setReceitasCategorias(recCat || {});
      setOrcamentoData(orcList || []);
      setSerieData(serieMap || {});
    } catch (err) {
      console.warn('Erro ao carregar telemetria gráfica:', err);
    } finally {
      setLoading(false);
    }
  };

  const carregarPngAtual = async () => {
    try {
      let tipo: 'gastos-mes' | 'orcamento-vs-gasto' | 'evolucao-gastos-mes' | 'carteira' = 'gastos-mes';
      if (activeTab === 'orcamento') tipo = 'orcamento-vs-gasto';
      else if (activeTab === 'evolucao') tipo = 'evolucao-gastos-mes';
      else if (activeTab === 'carteira') tipo = 'carteira';

      const url = await api.finance.getChartPngBlobUrl(tipo);
      setPngUrl(url);
    } catch {
      setPngUrl(null);
    }
  };

  useEffect(() => {
    if (isOpen) {
      carregarDados();
    }
  }, [isOpen, lastTransactionTimestamp]);

  useEffect(() => {
    if (isOpen && (viewPngMode || activeTab === 'carteira')) {
      carregarPngAtual();
    }
  }, [isOpen, activeTab, viewPngMode]);

  if (!isOpen) return null;

  // Cálculos para gráfico de Categorias selecionado
  const categoriasAtivas = categoriaTipo === 'DESPESA' ? despesasCategorias : receitasCategorias;
  const totalCategoriaAtiva = Object.values(categoriasAtivas).reduce((a, b) => a + Number(b), 0);
  const catEntries = Object.entries(categoriasAtivas).sort((a, b) => Number(b[1]) - Number(a[1]));

  // Dados do Balanço
  const totalReceitas = balancoData?.totalReceitas ?? Object.values(receitasCategorias).reduce((a, b) => a + Number(b), 0);
  const totalDespesas = balancoData?.totalDespesas ?? Object.values(despesasCategorias).reduce((a, b) => a + Number(b), 0);
  const saldoLiquido = balancoData?.saldo ?? (totalReceitas - totalDespesas);
  const taxaPoupanca = balancoData?.taxaPoupanca ?? (totalReceitas > 0 ? Math.max(0, (saldoLiquido / totalReceitas) * 100) : 0);
  const maxBalanco = Math.max(totalReceitas, totalDespesas, 100);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-void-950/80 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-4xl bg-void-900 border border-hologram-cyan/30 rounded-xl shadow-2xl flex flex-col max-h-[92vh] overflow-hidden text-slate-100">
        {/* Header do Modal */}
        <div className="px-5 py-4 border-b border-hologram-cyan/20 flex items-center justify-between select-none bg-void-950/60">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded bg-hologram-cyan/10 border border-hologram-cyan/40 text-hologram-cyan shadow-glow-cyan/20">
              <PieChart className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h2 className="font-mono text-base font-bold tracking-wider text-slate-100 flex items-center gap-2">
                TELEMETRIA FINANCEIRA // DASHBOARD SEXTA-FEIRA
              </h2>
              <p className="text-xs font-mono text-slate-400">
                Visualização vetorial interativa em tempo real integrada aos módulos de IA
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setViewPngMode(!viewPngMode)}
              className={`px-2.5 py-1.5 rounded text-xs font-mono border transition-all flex items-center gap-1.5 ${
                viewPngMode
                  ? 'border-hologram-purple bg-hologram-purple/20 text-hologram-purple shadow-glow-purple'
                  : 'border-slate-700 bg-slate-800/40 text-slate-300 hover:border-slate-500'
              }`}
              title="Alternar entre renderização vetorial e renderização Python Matplotlib de alta resolução"
            >
              <ImageIcon className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Render Matplotlib</span>
            </button>
            <button
              onClick={carregarDados}
              disabled={loading}
              className="p-2 rounded border border-slate-700 text-slate-300 hover:text-hologram-cyan hover:border-hologram-cyan/50 transition-colors"
              title="Recarregar dados"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-hologram-cyan' : ''}`} />
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded border border-slate-700 text-slate-400 hover:text-rose-400 hover:border-rose-500/50 transition-colors"
              title="Fechar"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Barra de Abas de Gráficos */}
        <div className="px-5 py-2.5 border-b border-hologram-cyan/15 bg-void-950/40 flex items-center gap-2 overflow-x-auto custom-scrollbar select-none">
          <button
            onClick={() => setActiveTab('balanco')}
            className={`px-3 py-1.5 rounded text-xs font-mono font-medium transition-all flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'balanco'
                ? 'bg-hologram-cyan/20 text-hologram-cyan border border-hologram-cyan/50 shadow-glow-cyan/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <Scale className="w-3.5 h-3.5" />
            Balanço (Receitas vs Despesas)
          </button>
          <button
            onClick={() => setActiveTab('categorias')}
            className={`px-3 py-1.5 rounded text-xs font-mono font-medium transition-all flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'categorias'
                ? 'bg-hologram-cyan/20 text-hologram-cyan border border-hologram-cyan/50 shadow-glow-cyan/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <PieChart className="w-3.5 h-3.5" />
            Distribuição por Categorias
          </button>
          <button
            onClick={() => setActiveTab('evolucao')}
            className={`px-3 py-1.5 rounded text-xs font-mono font-medium transition-all flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'evolucao'
                ? 'bg-hologram-cyan/20 text-hologram-cyan border border-hologram-cyan/50 shadow-glow-cyan/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <TrendingUp className="w-3.5 h-3.5" />
            Evolução Diária
          </button>
          <button
            onClick={() => setActiveTab('orcamento')}
            className={`px-3 py-1.5 rounded text-xs font-mono font-medium transition-all flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'orcamento'
                ? 'bg-hologram-cyan/20 text-hologram-cyan border border-hologram-cyan/50 shadow-glow-cyan/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <BarChart2 className="w-3.5 h-3.5" />
            Orçamento vs Gasto
          </button>
          <button
            onClick={() => setActiveTab('carteira')}
            className={`px-3 py-1.5 rounded text-xs font-mono font-medium transition-all flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'carteira'
                ? 'bg-hologram-cyan/20 text-hologram-cyan border border-hologram-cyan/50 shadow-glow-cyan/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            Carteira & Ativos
          </button>
        </div>

        {/* Conteúdo do Gráfico */}
        <div className="flex-1 overflow-y-auto p-5 space-y-6 custom-scrollbar">
          {viewPngMode || activeTab === 'carteira' ? (
            <div className="flex flex-col items-center justify-center p-4 bg-void-950/70 border border-slate-800 rounded-lg">
              {pngUrl ? (
                <img
                  src={pngUrl}
                  alt="Gráfico de alta resolução"
                  className="max-h-[500px] w-auto rounded border border-hologram-cyan/20 shadow-glow-cyan/20"
                />
              ) : (
                <div className="py-20 text-center font-mono text-xs text-slate-500">
                  {loading ? 'Sintetizando imagem do gráfico via Matplotlib...' : 'Nenhum dado financeiro suficiente para compor a série do gráfico.'}
                </div>
              )}
            </div>
          ) : (
            <>
              {/* TAB 0: BALANÇO (RECEITAS VS DESPESAS) */}
              {activeTab === 'balanco' && (
                <div className="space-y-6">
                  {/* Cards de KPIs Principais */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    <div className="p-3.5 rounded-lg bg-void-950/70 border border-emerald-500/30">
                      <div className="flex items-center justify-between text-xs font-mono text-emerald-400 mb-1">
                        <span>RECEITAS</span>
                        <TrendingUp className="w-3.5 h-3.5" />
                      </div>
                      <div className="text-lg font-mono font-bold text-emerald-400">
                        R$ {totalReceitas.toFixed(2)}
                      </div>
                      <div className="text-[10px] font-mono text-slate-400 mt-1">Total do mês</div>
                    </div>

                    <div className="p-3.5 rounded-lg bg-void-950/70 border border-rose-500/30">
                      <div className="flex items-center justify-between text-xs font-mono text-rose-400 mb-1">
                        <span>DESPESAS</span>
                        <TrendingDown className="w-3.5 h-3.5" />
                      </div>
                      <div className="text-lg font-mono font-bold text-rose-400">
                        R$ {totalDespesas.toFixed(2)}
                      </div>
                      <div className="text-[10px] font-mono text-slate-400 mt-1">Total do mês</div>
                    </div>

                    <div className={`p-3.5 rounded-lg bg-void-950/70 border ${
                      saldoLiquido >= 0 ? 'border-hologram-cyan/30 text-hologram-cyan' : 'border-rose-500/40 text-rose-400'
                    }`}>
                      <div className="flex items-center justify-between text-xs font-mono mb-1">
                        <span>SALDO LÍQUIDO</span>
                        <DollarSign className="w-3.5 h-3.5" />
                      </div>
                      <div className="text-lg font-mono font-bold">
                        R$ {saldoLiquido.toFixed(2)}
                      </div>
                      <div className="text-[10px] font-mono text-slate-400 mt-1">
                        {saldoLiquido >= 0 ? 'Superávit' : 'Déficit'}
                      </div>
                    </div>

                    <div className="p-3.5 rounded-lg bg-void-950/70 border border-indigo-500/30">
                      <div className="flex items-center justify-between text-xs font-mono text-indigo-400 mb-1">
                        <span>POUPANÇA</span>
                        <PiggyBank className="w-3.5 h-3.5" />
                      </div>
                      <div className="text-lg font-mono font-bold text-indigo-400">
                        {taxaPoupanca.toFixed(1)}%
                      </div>
                      <div className="text-[10px] font-mono text-slate-400 mt-1">Taxa de retenção</div>
                    </div>
                  </div>

                  {/* Comparativo Gráfico de Barras Proporcionais */}
                  <div className="p-5 rounded-lg bg-void-950/70 border border-slate-800 space-y-5">
                    <div className="flex items-center justify-between text-xs font-mono text-slate-300">
                      <span className="font-semibold tracking-wider">COMPARATIVO DE FLUXO DE CAIXA</span>
                      <span className="text-slate-400">MÊS ATUAL</span>
                    </div>

                    <div className="space-y-4">
                      {/* Barra Receitas */}
                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between text-xs font-mono">
                          <span className="text-emerald-400 font-semibold flex items-center gap-1.5">
                            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400" />
                            Entradas (Receitas, Vendas, Salários)
                          </span>
                          <span className="text-emerald-400 font-bold">R$ {totalReceitas.toFixed(2)}</span>
                        </div>
                        <div className="w-full h-4 rounded-full bg-slate-800/80 overflow-hidden p-0.5 border border-emerald-500/20">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-teal-400 transition-all duration-700 shadow-sm"
                            style={{ width: `${Math.min((totalReceitas / maxBalanco) * 100, 100)}%` }}
                          />
                        </div>
                      </div>

                      {/* Barra Despesas */}
                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between text-xs font-mono">
                          <span className="text-rose-400 font-semibold flex items-center gap-1.5">
                            <span className="w-2.5 h-2.5 rounded-full bg-rose-400" />
                            Saídas (Despesas, Gastos, Contas)
                          </span>
                          <span className="text-rose-400 font-bold">R$ {totalDespesas.toFixed(2)}</span>
                        </div>
                        <div className="w-full h-4 rounded-full bg-slate-800/80 overflow-hidden p-0.5 border border-rose-500/20">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-rose-500 to-amber-500 transition-all duration-700 shadow-sm"
                            style={{ width: `${Math.min((totalDespesas / maxBalanco) * 100, 100)}%` }}
                          />
                        </div>
                      </div>
                    </div>

                    {/* Resumo do Status Financeiro */}
                    <div className="p-3 rounded bg-void-900 border border-slate-800 flex items-center gap-3">
                      {saldoLiquido >= 0 ? (
                        <>
                          <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                          <div className="text-xs font-mono text-slate-300">
                            <span className="text-emerald-400 font-bold">Fluxo Positivo:</span> Suas receitas superam suas saídas em{' '}
                            <span className="font-bold text-slate-100">R$ {saldoLiquido.toFixed(2)}</span>. Ótimo momento para aportes e expansão da reserva!
                          </div>
                        </>
                      ) : (
                        <>
                          <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
                          <div className="text-xs font-mono text-slate-300">
                            <span className="text-rose-400 font-bold">Atenção ao Orçamento:</span> Suas despesas excedem suas receitas em{' '}
                            <span className="font-bold text-slate-100">R$ {Math.abs(saldoLiquido).toFixed(2)}</span> este mês.
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* TAB 1: CATEGORIAS COM TOGGLE DESPESAS / RECEITAS */}
              {activeTab === 'categorias' && (
                <div className="space-y-6">
                  {/* Seletor Despesas vs Receitas */}
                  <div className="flex items-center justify-between pb-2 border-b border-slate-800">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setCategoriaTipo('DESPESA')}
                        className={`px-3 py-1 rounded text-xs font-mono font-medium transition-all ${
                          categoriaTipo === 'DESPESA'
                            ? 'bg-rose-500/20 text-rose-400 border border-rose-500/40'
                            : 'text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        Despesas por Categoria
                      </button>
                      <button
                        onClick={() => setCategoriaTipo('RECEITA')}
                        className={`px-3 py-1 rounded text-xs font-mono font-medium transition-all ${
                          categoriaTipo === 'RECEITA'
                            ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
                            : 'text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        Receitas por Categoria
                      </button>
                    </div>

                    <div className="text-xs font-mono text-slate-400">
                      Total: <span className="text-slate-100 font-bold">R$ {totalCategoriaAtiva.toFixed(2)}</span>
                    </div>
                  </div>

                  {catEntries.length === 0 ? (
                    <div className="py-20 text-center font-mono text-sm text-slate-500">
                      Nenhum registro de {categoriaTipo === 'DESPESA' ? 'despesa' : 'receita'} encontrado para este período.
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
                      {/* Gráfico Donut SVG */}
                      <div className="flex flex-col items-center justify-center relative p-4">
                        <svg className="w-64 h-64 -rotate-90 transform" viewBox="0 0 100 100">
                          {(() => {
                            let acumulado = 0;
                            return catEntries.map(([cat, val]) => {
                              const pct = totalCategoriaAtiva > 0 ? (Number(val) / totalCategoriaAtiva) * 100 : 0;
                              const dash = `${pct} ${100 - pct}`;
                              const offset = -acumulado;
                              acumulado += pct;
                              const color = CATEGORY_COLORS[cat] || '#00f3ff';
                              return (
                                <circle
                                  key={cat}
                                  cx="50"
                                  cy="50"
                                  r="38"
                                  fill="transparent"
                                  stroke={color}
                                  strokeWidth="16"
                                  strokeDasharray={dash}
                                  strokeDashoffset={offset}
                                  pathLength="100"
                                  className="transition-all duration-500 hover:opacity-80"
                                />
                              );
                            });
                          })()}
                        </svg>
                        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                          <span className="text-[11px] font-mono text-slate-400">
                            {categoriaTipo === 'DESPESA' ? 'TOTAL GASTO' : 'TOTAL RECEBIDO'}
                          </span>
                          <span className={`text-xl font-mono font-bold ${
                            categoriaTipo === 'DESPESA' ? 'text-hologram-cyan' : 'text-emerald-400'
                          }`}>
                            R$ {totalCategoriaAtiva.toFixed(2)}
                          </span>
                        </div>
                      </div>

                      {/* Legenda e Distribuição Percentual */}
                      <div className="space-y-3">
                        <h3 className="text-xs font-mono text-slate-400 uppercase tracking-wider">
                          DISTRIBUIÇÃO DETALHADA
                        </h3>
                        <div className="space-y-2.5">
                          {catEntries.map(([cat, val]) => {
                            const pct = totalCategoriaAtiva > 0 ? ((Number(val) / totalCategoriaAtiva) * 100).toFixed(1) : '0';
                            const color = CATEGORY_COLORS[cat] || '#00f3ff';
                            return (
                              <div
                                key={cat}
                                className="p-2.5 rounded bg-void-950/60 border border-slate-800 hover:border-hologram-cyan/30 transition-colors flex items-center justify-between"
                              >
                                <div className="flex items-center gap-2.5">
                                  <span
                                    className="w-3 h-3 rounded-full shadow-sm"
                                    style={{ backgroundColor: color }}
                                  />
                                  <span className="text-xs font-medium text-slate-200 capitalize">
                                    {cat.toLowerCase().replace('_', ' ')}
                                  </span>
                                </div>
                                <div className="text-right">
                                  <span className="text-xs font-mono font-bold text-slate-100">
                                    R$ {Number(val).toFixed(2)}
                                  </span>
                                  <span className="text-[10px] font-mono text-slate-400 ml-2">
                                    ({pct}%)
                                  </span>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* TAB 2: EVOLUÇÃO TEMPORAL */}
              {activeTab === 'evolucao' && (
                <div className="space-y-6">
                  {Object.keys(serieData).length === 0 ? (
                    <div className="py-20 text-center font-mono text-sm text-slate-500">
                      Nenhum histórico diário disponível no mês atual.
                    </div>
                  ) : (
                    <div className="space-y-4">
                      <div className="p-4 rounded-lg bg-void-950/70 border border-slate-800 space-y-4">
                        <div className="flex items-center justify-between text-xs font-mono text-slate-400">
                          <span>PROGRESSÃO DIÁRIA DE GASTOS</span>
                          <span>{Object.keys(serieData).length} DIAS NO MÊS</span>
                        </div>

                        {/* Gráfico de Barras SVG Futurista */}
                        <div className="h-60 flex items-end gap-1.5 pt-6 pb-2 px-2 border-b border-slate-800">
                          {(() => {
                            const valores = Object.values(serieData).map(Number);
                            const maxVal = Math.max(...valores, 10);
                            return Object.entries(serieData).map(([dia, val]) => {
                              const alturaPct = (Number(val) / maxVal) * 100;
                              const diaFmt = dia.slice(8); // dd
                              return (
                                <div key={dia} className="flex-1 flex flex-col items-center gap-1.5 group relative">
                                  {/* Tooltip Hover */}
                                  <div className="absolute -top-8 hidden group-hover:block bg-void-900 border border-hologram-cyan/40 px-2 py-0.5 rounded text-[10px] font-mono text-hologram-cyan whitespace-nowrap shadow-lg z-20">
                                    {dia}: R$ {Number(val).toFixed(2)}
                                  </div>
                                  <div className="w-full bg-slate-800/40 rounded-t h-full flex items-end">
                                    <div
                                      className="w-full rounded-t bg-gradient-to-t from-hologram-cyan/60 to-hologram-cyan transition-all duration-300 group-hover:brightness-125"
                                      style={{ height: `${Math.max(alturaPct, 4)}%` }}
                                    />
                                  </div>
                                  <span className="text-[10px] font-mono text-slate-400">{diaFmt}</span>
                                </div>
                              );
                            });
                          })()}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* TAB 3: ORÇAMENTO VS GASTO */}
              {activeTab === 'orcamento' && (
                <div className="space-y-6">
                  {orcamentoData.length === 0 ? (
                    <div className="py-20 text-center font-mono text-sm text-slate-500">
                      Nenhum orçamento configurado. Diga: "Definir orçamento de alimentação 1000 reais" para parametrizar!
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {orcamentoData.map((item) => {
                        const perc = item.orcamento > 0 ? (item.gasto / item.orcamento) * 100 : 0;
                        const isEstourado = perc > 100;
                        const barColor = isEstourado ? 'bg-rose-500' : perc >= 80 ? 'bg-amber-400' : 'bg-hologram-cyan';
                        return (
                          <div
                            key={item.categoria}
                            className="p-3.5 rounded-lg bg-void-950/60 border border-slate-800 hover:border-hologram-cyan/30 transition-colors space-y-2"
                          >
                            <div className="flex items-center justify-between text-xs">
                              <span className="font-semibold text-slate-200 capitalize">
                                {item.categoria.toLowerCase().replace('_', ' ')}
                              </span>
                              <span className="font-mono text-xs text-slate-300">
                                R$ {item.gasto.toFixed(2)} de R$ {item.orcamento.toFixed(2)} ({perc.toFixed(0)}%)
                              </span>
                            </div>
                            <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                              <div
                                className={`h-full rounded-full transition-all duration-500 ${barColor}`}
                                style={{ width: `${Math.min(perc, 100)}%` }}
                              />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>

        {/* Rodapé Informativo */}
        <div className="px-5 py-3 border-t border-hologram-cyan/15 bg-void-950/80 flex items-center justify-between text-[11px] font-mono text-slate-400">
          <span>STATUS: CONECTADO AO NÚCLEO SEXTA-FEIRA</span>
          <span>VALORES EM TEMPO REAL // ISOLAMENTO POR USUÁRIO</span>
        </div>
      </div>
    </div>
  );
};
