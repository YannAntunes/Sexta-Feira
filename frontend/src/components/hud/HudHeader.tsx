import React from 'react';
import { HologramState, PersonalityType, UserProfile } from '../../types';
import { Shield, Sparkles, Bell, BarChart3, LogOut, Radio } from 'lucide-react';

interface HudHeaderProps {
  state: HologramState;
  user: UserProfile | null;
  unreadCount: number;
  onOpenNotifications: () => void;
  onToggleSidebar: () => void;
  onChangePersonality: (p: PersonalityType) => void;
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
  onToggleSidebar,
  onChangePersonality,
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

      {/* Centro: Modos de Personalidade */}
      {user && (
        <div className="hidden lg:flex items-center gap-1 p-1 rounded border border-hologram-cyan/20 bg-void-900/50 backdrop-blur-md">
          {(['SEXTA_FEIRA', 'FORMAL', 'AMIGAVEL', 'JARVIS'] as PersonalityType[]).map((p) => {
            const active = user.personalidade === p;
            return (
              <button
                key={p}
                onClick={() => onChangePersonality(p)}
                className={`px-2.5 py-1 text-xs font-mono rounded transition-all duration-200 flex items-center gap-1.5 ${
                  active
                    ? 'bg-hologram-cyan/20 text-hologram-cyan border border-hologram-cyan/50 shadow-glow-cyan'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
                }`}
              >
                {p === 'JARVIS' && <Shield className="w-3 h-3" />}
                {p === 'SEXTA_FEIRA' && <Sparkles className="w-3 h-3" />}
                {p.replace('_', '-')}
              </button>
            );
          })}
        </div>
      )}

      {/* Lado Direito: Notificações, Telemetria Financeira e Perfil */}
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

        {/* Botão do Painel de Telemetria Financeira */}
        <button
          onClick={onToggleSidebar}
          className="p-2 rounded border border-slate-700/60 bg-void-900/40 text-slate-300 hover:text-hologram-cyan hover:border-hologram-cyan/50 transition-colors"
          title="Telemetria Financeira"
        >
          <BarChart3 className="w-4 h-4" />
        </button>

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
