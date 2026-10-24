import { useState, useEffect, useCallback } from 'react';
import { HologramState, PersonalityType, UserProfile, ChatMessage, Notificacao } from './types';
import { api } from './services/api';
import { voiceService } from './services/voice';
import { HologramCanvas } from './components/hologram/HologramCanvas';
import { HudHeader } from './components/hud/HudHeader';
import { HudSidebar } from './components/hud/HudSidebar';
import { HudNotificationsModal } from './components/hud/HudNotificationsModal';
import { ChatContainer } from './components/chat/ChatContainer';
import { VoiceVisualizer } from './components/voice/VoiceVisualizer';
import { AuthModal } from './components/auth/AuthModal';

export function App() {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [hologramState, setHologramState] = useState<HologramState>('IDLE');
  const [audioLevel, setAudioLevel] = useState<number>(0);
  const [frequencyData, setFrequencyData] = useState<Uint8Array | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [notifications, setNotifications] = useState<Notificacao[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);

  // Estados dos modais de HUD
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const [isRecording, setIsRecording] = useState(false);

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
      // fallback
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
      // fallback
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
        setAudioLevel(level);
        setFrequencyData(freq);
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

      // Atualiza notificações e telemetria financeira em background
      carregarNotificacoes();

      // Fala a resposta por voz se a mensagem veio por voz
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

  // Troca de Personalidade
  const handleChangePersonality = async (nova: PersonalityType) => {
    if (!user) return;
    try {
      const atualizado = await api.auth.updateProfile({ personalidade: nova });
      setUser(atualizado);
    } catch (err) {
      console.error(err);
    }
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
        onToggleSidebar={() => setIsSidebarOpen(!isSidebarOpen)}
        onChangePersonality={handleChangePersonality}
        onLogout={handleLogout}
      />

      {/* Conteúdo Principal em Duas Seções Integradas */}
      <main className="flex-1 flex flex-col md:flex-row overflow-hidden relative z-20">
        {/* Seção 1: Núcleo Holográfico 3D */}
        <section className="flex-1 flex flex-col items-center justify-center relative min-h-[40vh] md:min-h-0">
          <HologramCanvas
            state={hologramState}
            audioLevel={audioLevel}
            frequencyData={frequencyData}
          />

          {/* Medidor de Frequências Sonoras no Rodapé do Holograma */}
          <div className="absolute bottom-2 inset-x-0 flex justify-center">
            <VoiceVisualizer
              state={hologramState}
              audioLevel={audioLevel}
              frequencyData={frequencyData}
            />
          </div>
        </section>

        {/* Seção 2: Interface Conversacional */}
        <section className="w-full md:w-[460px] lg:w-[500px] h-[55vh] md:h-full p-3 sm:p-4 md:border-l border-hologram-cyan/15 flex flex-col">
          <ChatContainer
            messages={messages}
            hologramState={hologramState}
            isRecording={isRecording}
            onSendMessage={handleSendMessage}
            onToggleVoice={handleToggleVoice}
            onInterrupt={handleInterrupt}
          />
        </section>
      </main>

      {/* Painel Retrátil de Telemetria Financeira */}
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
