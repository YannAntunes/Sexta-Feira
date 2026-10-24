import React from 'react';
import { HologramState, UserProfile } from '../../types';
import { Sparkles, Bell, BarChart3, LogOut, Radio, Volume2 } from 'lucide-react';

interface HudHeaderProps {
  state: HologramState;
  user: UserProfile | null;
  unreadCount: number;
  onOpenNotifications: () => void;
  onOpenCharts: () => void;
  onOpenVoiceSettings?: () => void;
  onLogout: () => void;
}

const STATE_LABELS: Record<HologramState, { label: string; color: string; badge: string }> = {
  IDLE: { label: 'VIGILÂNCIA // ATIVA', color: 'text-hologram-cyan', badge: 'bg-hologram-cyan/20 border-hologram-cyan/40' },
  LISTENING: { label: 'OUVINDO // ENTRADA DE VOZ', color: 'text-hologram-cyan animate-pulse', badge: 'bg-hologram-cyan/30 border-hologram-cyan' },
  THINKING: { label: 'PROCESSANDO // RACIOCÍNIO', color: 'text-hologram-purple animate-pulse', badge: 'bg-hologram-purple/30 border-hologram-purple' },
  SPEAKING: { label: 'FALANDO // RESPOSTA', color: 'text-emerald-400', badge: 'bg-emerald-500/20 border-emerald-400/50' },
  PROCESSING: { label: 'EXECUTANDO // SISTEMA', color: 'text-amber-400', badge: 'bg-amber-500/20 border-amber-400/50' },
  ERROR: { label: 'ALERTA // INTERFERÊNCIA', color: 'text-rose-500 animate-bounce', badge: 'bg-rose-500/20 border-rose-500/60' },
};

export const HudHeader: React.FC<HudHeaderProps> = ({
  state,
  user,
  unreadCount,
  onOpenNotifications,
  onOpenCharts,
  onOpenVoiceSettings,
  onLogout,
}) => {
  const currentStatus = STATE_LABELS[state];

  return (
    <header className="h-16 px-4 md:px-6 border-b border-hologram-cyan/15 bg-void-950/70 backdrop-blur-xl flex items-center justify-between z-30 select-none">
      {/* Lado Esquerdo: Identidade & Status de Operação */}
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded border border-hologram-cyan/40 bg-hologram-cyan/10 flex items-center justify-center shadow-glow-cyan">
            <Radio className="w-4 h-4 text-hologram-cyan animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-sm font-bold tracking-widest text-slate-100">
                SEXTA-FEIRA
              </span>
              <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-hologram-cyan/10 text-hologram-cyan border border-hologram-cyan/30">
                V3.0
              </span>
            </div>
            <div className="flex items-center gap-1.5 text-[10px] font-mono">
              <span className={`w-1.5 h-1.5 rounded-full ${currentStatus.color.includes('emerald') ? 'bg-emerald-400' : currentStatus.color.includes('purple') ? 'bg-hologram-purple' : currentStatus.color.includes('rose') ? 'bg-rose-500' : 'bg-hologram-cyan'} animate-ping`} />
              <span className={currentStatus.color}>{currentStatus.label}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Centro: Identidade Unificada e Coerente da Sexta-Feira */}
      <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full border border-hologram-cyan/25 bg-void-900/60 backdrop-blur-md shadow-glow-cyan/10">
        <Sparkles className="w-3.5 h-3.5 text-hologram-cyan animate-pulse" />
        <span className="font-mono text-xs font-semibold tracking-wider text-slate-200">
          SISTEMA SEXTA-FEIRA // IA PESSOAL
        </span>
      </div>

      {/* Lado Direito: Notificações, Telemetria Gráfica e Perfil */}
      <div className="flex items-center gap-2">
        {/* Botão de Notificações In-App */}
        <button
          onClick={onOpenNotifications}
          className="relative p-2 rounded border border-slate-700/60 bg-void-900/40 text-slate-300 hover:text-hologram-cyan hover:border-hologram-cyan/50 transition-colors"
          title="Notificações e Alertas"
        >
          <Bell className="w-4 h-4" />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-rose-500 text-white font-mono text-[9px] flex items-center justify-center animate-pulse">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </button>

        {/* Botão de Gráficos e Telemetria Financeira Interativa */}
        <button
          onClick={onOpenCharts}
          className="p-2 rounded border border-slate-700/60 bg-void-900/40 text-slate-300 hover:text-hologram-cyan hover:border-hologram-cyan/50 transition-colors flex items-center gap-1.5"
          title="Dashboard de Gráficos e Telemetria"
        >
          <BarChart3 className="w-4 h-4 text-hologram-cyan" />
          <span className="hidden md:inline font-mono text-xs text-slate-300">Gráficos</span>
        </button>

        {/* Botão de Voz e TTS */}
        {onOpenVoiceSettings && (
          <button
            onClick={onOpenVoiceSettings}
            className="p-2 rounded border border-slate-700/60 bg-void-900/40 text-slate-300 hover:text-hologram-cyan hover:border-hologram-cyan/50 transition-colors flex items-center gap-1.5"
            title="Configurações de Voz e Fala"
          >
            <Volume2 className="w-4 h-4 text-hologram-cyan" />
            <span className="hidden lg:inline font-mono text-xs text-slate-300">Voz</span>
          </button>
        )}

        {/* Informações do Usuário & Logout */}
        {user && (
          <div className="flex items-center gap-2 pl-2 border-l border-slate-800">
            <div className="hidden sm:block text-right">
              <p className="text-xs font-medium text-slate-200">{user.nome}</p>
              <p className="text-[10px] font-mono text-slate-400">{user.email}</p>
            </div>
            <button
              onClick={onLogout}
              className="p-2 rounded text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
              title="Encerrar Sessão"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
