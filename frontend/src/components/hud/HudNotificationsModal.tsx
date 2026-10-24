import React from 'react';
import { Notificacao } from '../../types';
import { Bell, Check, X, AlertTriangle, Info, AlertOctagon } from 'lucide-react';

interface HudNotificationsModalProps {
  isOpen: boolean;
  notifications: Notificacao[];
  onClose: () => void;
  onMarkAsRead: (id: number) => void;
}

export const HudNotificationsModal: React.FC<HudNotificationsModalProps> = ({
  isOpen,
  notifications,
  onClose,
  onMarkAsRead,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-void-950/80 backdrop-blur-md">
      <div className="w-full max-w-lg bg-void-950 border border-hologram-cyan/30 rounded-xl shadow-glow-cyan/20 overflow-hidden flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="px-5 py-4 border-b border-hologram-cyan/20 bg-void-900/60 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <Bell className="w-5 h-5 text-hologram-cyan" />
            <h3 className="font-mono text-sm font-bold tracking-wider text-slate-100">
              NOTIFICAÇÕES // ALERTAS
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded text-slate-400 hover:text-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Lista */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
          {notifications.length === 0 ? (
            <div className="p-8 text-center text-xs font-mono text-slate-500">
              Nenhuma notificação registrada no momento.
            </div>
          ) : (
            notifications.map((notif) => {
              const isEstouro = notif.tipoAlerta === 'ESTOURO';
              const isAlerta = notif.tipoAlerta === 'ALERTA_90' || notif.tipoAlerta === 'AVISO_80';

              return (
                <div
                  key={notif.id}
                  className={`p-3.5 rounded-lg border transition-all ${
                    notif.lida
                      ? 'bg-void-900/30 border-slate-800 text-slate-400'
                      : isEstouro
                      ? 'bg-rose-500/10 border-rose-500/40 text-slate-200 shadow-glow-red/10'
                      : isAlerta
                      ? 'bg-amber-500/10 border-amber-500/40 text-slate-200 shadow-glow-gold/10'
                      : 'bg-void-900/80 border-hologram-cyan/30 text-slate-200'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-2.5">
                      <div className="mt-0.5">
                        {isEstouro ? (
                          <AlertOctagon className="w-4 h-4 text-rose-400" />
                        ) : isAlerta ? (
                          <AlertTriangle className="w-4 h-4 text-amber-400" />
                        ) : (
                          <Info className="w-4 h-4 text-hologram-cyan" />
                        )}
                      </div>
                      <div>
                        <h4 className="text-xs font-semibold">{notif.titulo}</h4>
                        <p className="mt-1 text-xs text-slate-300 leading-relaxed">
                          {notif.mensagem}
                        </p>
                        <span className="mt-2 block font-mono text-[10px] text-slate-500">
                          {new Date(notif.criadoEm).toLocaleString()}
                        </span>
                      </div>
                    </div>

                    {!notif.lida && (
                      <button
                        onClick={() => onMarkAsRead(notif.id)}
                        className="p-1 rounded bg-slate-800/80 hover:bg-hologram-cyan/20 text-slate-400 hover:text-hologram-cyan border border-slate-700/60 transition-colors"
                        title="Marcar como lida"
                      >
                        <Check className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
