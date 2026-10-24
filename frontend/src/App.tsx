import { useState, useEffect, useCallback, useRef } from 'react';
import { HologramState, UserProfile, ChatMessage, Notificacao } from './types';
import { api } from './services/api';
import { voiceService } from './services/voice';
import { HologramCanvas } from './components/hologram/HologramCanvas';
import { HudHeader } from './components/hud/HudHeader';
import { HudSidebar } from './components/hud/HudSidebar';
import { HudNotificationsModal } from './components/hud/HudNotificationsModal';
import { HudChartsModal } from './components/hud/HudChartsModal';
import { HudVoiceSettingsModal } from './components/hud/HudVoiceSettingsModal';
import { ChatContainer } from './components/chat/ChatContainer';
import { VoiceVisualizer } from './components/voice/VoiceVisualizer';
import { AuthModal } from './components/auth/AuthModal';
import { MessageSquare, Mic, MicOff, Square, Maximize2, Minimize2 } from 'lucide-react';

export function App() {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [hologramState, setHologramState] = useState<HologramState>('IDLE');
  const [audioLevel, setAudioLevel] = useState<number>(0);
  const [frequencyData, setFrequencyData] = useState<Uint8Array | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [notifications, setNotifications] = useState<Notificacao[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);

  // Estados de HUD e Modais
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const [isChartsOpen, setIsChartsOpen] = useState(false);
  const [isVoiceSettingsOpen, setIsVoiceSettingsOpen] = useState(false);
  const [isChatCollapsed, setIsChatCollapsed] = useState(true); // Chat recolhido por padrão para expansão do holograma
  const [isRecording, setIsRecording] = useState(false);
  const [lastTransactionTimestamp, setLastTransactionTimestamp] = useState<number>(Date.now());
  const lastAudioUpdateRef = useRef<number>(0);

  // 1. Carrega Perfil do Usuário
  const carregarPerfil = useCallback(async () => {
    try {
      const perfil = await api.auth.getProfile();
      setUser(perfil);
      carregarHistorico();
      carregarNotificacoes();
    } catch {
      setUser(null);
    }
  }, []);

  // 2. Carrega Histórico Conversacional da API
  const carregarHistorico = async () => {
    try {
      const hist = await api.chat.getHistory(25);
      if (hist && hist.length > 0) {
        setMessages(hist);
      }
    } catch {
      // fallback gracioso
    }
  };

  // 3. Carrega Notificações In-App
  const carregarNotificacoes = async () => {
    try {
      const notifs = await api.notifications.list(false);
      setNotifications(notifs);
      const unread = notifs.filter((n) => !n.lida).length;
      setUnreadCount(unread);
    } catch {
      // fallback gracioso
    }
  };

  useEffect(() => {
    carregarPerfil();
  }, [carregarPerfil]);

  // 4. Configuração dos Callbacks de Voz e Web Audio
  useEffect(() => {
    voiceService.setCallbacks({
      onTranscript: (text, isFinal) => {
        if (isFinal) {
          handleSendMessage(text, true);
        }
      },
      onAudioLevel: (level, freq) => {
        const now = performance.now();
        if (now - lastAudioUpdateRef.current > 45) {
          lastAudioUpdateRef.current = now;
          setAudioLevel(level);
          setFrequencyData(freq);
        }
      },
      onStateChange: (voiceState) => {
        if (voiceState === 'LISTENING') {
          setHologramState('LISTENING');
          setIsRecording(true);
        } else if (voiceState === 'SPEAKING') {
          setHologramState('SPEAKING');
        } else {
          setHologramState('IDLE');
          setIsRecording(false);
        }
      },
      onError: (err) => {
        console.warn(err);
        setHologramState('ERROR');
        setIsRecording(false);
        setTimeout(() => setHologramState('IDLE'), 3000);
      },
    });
  }, []);

  // 5. Envio de Mensagem para a Sexta-Feira
  const handleSendMessage = async (text: string, fromVoice = false) => {
    if (!text.trim()) return;

    // Se estava gravando, pausa o microfone enquanto a IA processa
    if (isRecording) {
      voiceService.stopListening();
    }

    const userMsg: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isVoice: fromVoice,
    };

    setMessages((prev) => [...prev, userMsg]);
    setHologramState('THINKING');

    try {
      const res = await api.chat.send(text);
      const assistantMsg: ChatMessage = {
        id: `asst-${Date.now()}`,
        role: 'assistant',
        content: res.resposta,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setMessages((prev) => [...prev, assistantMsg]);

      // Se a resposta ou pergunta envolver gráficos, abre o modal de gráficos automaticamente
      const lowerResp = res.resposta.toLowerCase();
      const lowerAsk = text.toLowerCase();
      if (
        lowerResp.includes('gráfico') ||
        lowerResp.includes('grafico') ||
        lowerAsk.includes('gráfico') ||
        lowerAsk.includes('grafico')
      ) {
        setIsChartsOpen(true);
      }

      // Atualiza notificações e telemetria financeira em background
      carregarNotificacoes();
      setLastTransactionTimestamp(Date.now());

      // Fala a resposta por voz com síntese feminina natural
      if (fromVoice) {
        voiceService.speak(res.resposta, () => {
          setHologramState('IDLE');
        });
      } else {
        setHologramState('IDLE');
      }
    } catch (err: any) {
      const errorMsg: ChatMessage = {
        id: `err-${Date.now()}`,
        role: 'assistant',
        content: `Falha na transmissão: ${err.message || 'Servidor indisponível'}`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, errorMsg]);
      setHologramState('ERROR');
      setTimeout(() => setHologramState('IDLE'), 3000);
    }
  };

  // Alterna Gravação de Voz
  const handleToggleVoice = async () => {
    if (isRecording) {
      voiceService.stopListening();
    } else {
      await voiceService.startListening();
    }
  };

  // Interrupção Imediata (Barge-in)
  const handleInterrupt = () => {
    voiceService.stopSpeaking();
    setHologramState('IDLE');
  };

  // Marcar Notificação como Lida
  const handleMarkNotificationAsRead = async (id: number) => {
    try {
      await api.notifications.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, lida: true } : n))
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch {
      // ignorar
    }
  };

  // Logout
  const handleLogout = () => {
    api.clearTokens();
    setUser(null);
    setMessages([]);
  };

  return (
    <div className="h-screen w-screen flex flex-col bg-void-950 text-slate-100 overflow-hidden relative font-sans">
      {/* Grade Tática de Fundo */}
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#00f3ff08_1px,transparent_1px),linear-gradient(to_bottom,#00f3ff08_1px,transparent_1px)] bg-[size:40px_40px] pointer-events-none" />

      {/* Linha de Scanline Holográfica */}
      <div className="absolute inset-x-0 h-1 bg-gradient-to-r from-transparent via-hologram-cyan/20 to-transparent animate-scanline pointer-events-none z-10" />

      {/* Header do HUD */}
      <HudHeader
        state={hologramState}
        user={user}
        unreadCount={unreadCount}
        onOpenNotifications={() => setIsNotifOpen(true)}
        onOpenCharts={() => setIsChartsOpen(true)}
        onOpenVoiceSettings={() => setIsVoiceSettingsOpen(true)}
        onLogout={handleLogout}
      />

      {/* Conteúdo Principal com Layout Flexível e Recolhimento */}
      <main className="flex-1 flex flex-col md:flex-row overflow-hidden relative z-20">
        {/* Seção 1: Núcleo Holográfico 3D (Expande para 100% quando chat recolhido) */}
        <section
          className={`flex-1 flex flex-col items-center justify-center relative transition-all duration-300 ${
            isChatCollapsed ? 'w-full h-full' : 'min-h-[40vh] md:min-h-0'
          }`}
        >
          <HologramCanvas
            state={hologramState}
            audioLevel={audioLevel}
            frequencyData={frequencyData}
          />

          {/* Medidor de Frequências Sonoras no Rodapé do Holograma */}
          <div className="absolute bottom-3 inset-x-0 flex flex-col items-center gap-3">
            <VoiceVisualizer
              state={hologramState}
              audioLevel={audioLevel}
              frequencyData={frequencyData}
            />

            {/* Controles Flutuantes quando o Chat estiver recolhido */}
            {isChatCollapsed && (
              <div className="flex items-center gap-3 bg-void-900/90 border border-hologram-cyan/30 px-4 py-2 rounded-full shadow-2xl backdrop-blur-lg animate-fadeIn">
                <button
                  onClick={handleToggleVoice}
                  className={`p-3 rounded-full border transition-all ${
                    isRecording
                      ? 'bg-rose-500/20 border-rose-500 text-rose-400 shadow-glow-red animate-pulse'
                      : 'bg-hologram-cyan/20 border-hologram-cyan/50 text-hologram-cyan shadow-glow-cyan hover:scale-105'
                  }`}
                  title={isRecording ? 'Parar escuta' : 'Iniciar escuta por voz'}
                >
                  {isRecording ? <MicOff className="w-5 h-5" /> : <Mic className="w-5 h-5" />}
                </button>

                {hologramState === 'SPEAKING' && (
                  <button
                    onClick={handleInterrupt}
                    className="p-3 rounded-full bg-amber-500/20 border border-amber-400 text-amber-300 hover:scale-105 transition-all shadow-glow-gold"
                    title="Interromper fala (Barge-in)"
                  >
                    <Square className="w-5 h-5" />
                  </button>
                )}

                <button
                  onClick={() => setIsChatCollapsed(false)}
                  className="px-3.5 py-2 rounded-full bg-slate-800/80 border border-slate-700 hover:border-hologram-cyan/50 text-xs font-mono text-slate-200 flex items-center gap-2 transition-all"
                  title="Reabrir janela de conversa"
                >
                  <MessageSquare className="w-4 h-4 text-hologram-cyan" />
                  <span>Abrir Chat</span>
                </button>
              </div>
            )}
          </div>

          {/* Botão de Toggle de Recolhimento do Chat (Canto Superior Direito da Área do Holograma) */}
          <button
            onClick={() => setIsChatCollapsed(!isChatCollapsed)}
            className="absolute top-4 right-4 p-2 rounded-lg bg-void-900/80 border border-hologram-cyan/30 text-slate-300 hover:text-hologram-cyan hover:border-hologram-cyan/60 transition-all shadow-lg backdrop-blur-md flex items-center gap-1.5 font-mono text-xs z-30"
            title={isChatCollapsed ? 'Restaurar layout com chat' : 'Recolher chat e expandir holograma em tela cheia'}
          >
            {isChatCollapsed ? (
              <>
                <Minimize2 className="w-4 h-4 text-hologram-cyan" />
                <span className="hidden sm:inline">Exibir Chat</span>
              </>
            ) : (
              <>
                <Maximize2 className="w-4 h-4 text-hologram-cyan" />
                <span className="hidden sm:inline">Modo Imersivo</span>
              </>
            )}
          </button>
        </section>

        {/* Seção 2: Interface Conversacional (Ocultável) */}
        {!isChatCollapsed && (
          <section className="w-full md:w-[460px] lg:w-[500px] h-[55vh] md:h-full p-3 sm:p-4 md:border-l border-hologram-cyan/15 flex flex-col transition-all duration-300 bg-void-950/40 backdrop-blur-sm">
            <ChatContainer
              messages={messages}
              hologramState={hologramState}
              isRecording={isRecording}
              onSendMessage={handleSendMessage}
              onToggleVoice={handleToggleVoice}
              onInterrupt={handleInterrupt}
            />
          </section>
        )}
      </main>

      {/* Modal Interativo de Gráficos e Telemetria Financeira */}
      <HudChartsModal
        isOpen={isChartsOpen}
        onClose={() => setIsChartsOpen(false)}
        lastTransactionTimestamp={lastTransactionTimestamp}
      />

      {/* Modal de Configuração de Voz & Síntese TTS */}
      <HudVoiceSettingsModal
        isOpen={isVoiceSettingsOpen}
        onClose={() => setIsVoiceSettingsOpen(false)}
      />

      {/* Painel Retrátil Lateral de Telemetria Financeira */}
      <HudSidebar
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
      />

      {/* Modal de Notificações e Alertas In-App */}
      <HudNotificationsModal
        isOpen={isNotifOpen}
        notifications={notifications}
        onClose={() => setIsNotifOpen(false)}
        onMarkAsRead={handleMarkNotificationAsRead}
      />

      {/* Modal de Autenticação Segura se não autenticado */}
      {!user && <AuthModal onSuccess={(u) => setUser(u)} />}
    </div>
  );
}

export default App;
