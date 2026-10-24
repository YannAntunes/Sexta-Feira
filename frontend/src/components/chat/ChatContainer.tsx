import React, { useState, useRef, useEffect } from 'react';
import { ChatMessage, HologramState } from '../../types';
import { Send, Mic, MicOff, Square, Sparkles, TrendingUp, AlertTriangle } from 'lucide-react';

interface ChatContainerProps {
  messages: ChatMessage[];
  hologramState: HologramState;
  isRecording: boolean;
  onSendMessage: (text: string) => void;
  onToggleVoice: () => void;
  onInterrupt: () => void;
}

export const ChatContainer: React.FC<ChatContainerProps> = ({
  messages,
  hologramState,
  isRecording,
  onSendMessage,
  onToggleVoice,
  onInterrupt,
}) => {
  const [inputText, setInputText] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, hologramState]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;
    onSendMessage(inputText.trim());
    setInputText('');
  };

  const isSpeaking = hologramState === 'SPEAKING';
  const isThinking = hologramState === 'THINKING';

  return (
    <div className="flex flex-col h-full bg-void-950/60 backdrop-blur-md border border-hologram-cyan/15 rounded-lg overflow-hidden shadow-2xl relative">
      {/* Header do Chat / Barra de Status */}
      <div className="px-4 py-2.5 border-b border-hologram-cyan/15 bg-void-900/40 flex items-center justify-between text-xs font-mono text-slate-400 select-none">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-hologram-cyan animate-pulse" />
          <span className="text-slate-200">CANAL DIALÓGICO DIRETO</span>
        </div>
        <div className="text-[11px] text-hologram-cyan/70">
          HISTÓRICO ATIVO ({messages.length})
        </div>
      </div>

      {/* Área de Mensagens com Rolagem Suave */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar">
        {messages.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center p-6 text-slate-500 font-mono text-xs">
            <Sparkles className="w-8 h-8 text-hologram-cyan/40 mb-3 animate-pulse" />
            <p className="text-slate-300 font-medium">Sistemas inicializados e prontos.</p>
            <p className="mt-1 text-[11px] text-slate-500">
              Digite uma instrução ou inicie a conversa por voz.
            </p>
          </div>
        ) : (
          messages.map((msg) => {
            const isUser = msg.role === 'user';
            return (
              <div
                key={msg.id}
                className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} transition-opacity duration-200`}
              >
                <div className="flex items-center gap-1.5 mb-1 px-1 text-[10px] font-mono text-slate-400">
                  <span>{isUser ? 'VOCÊ' : 'SEXTA-FEIRA'}</span>
                  <span>•</span>
                  <span>{msg.timestamp}</span>
                </div>

                <div
                  className={`max-w-[85%] sm:max-w-[75%] rounded-lg p-3.5 text-sm leading-relaxed whitespace-pre-line ${
                    isUser
                      ? 'bg-void-800/80 border border-hologram-cyan/25 text-slate-100 rounded-br-none shadow-md'
                      : 'bg-void-900/90 border border-hologram-cyan/35 text-slate-100 rounded-bl-none shadow-glow-cyan/20 backdrop-blur-xl'
                  }`}
                >
                  {msg.content}
                </div>
              </div>
            );
          })
        )}

        {/* Indicador de Pensamento / Geração */}
        {isThinking && (
          <div className="flex items-start gap-2">
            <div className="bg-void-900/90 border border-hologram-purple/50 rounded-lg rounded-bl-none p-3.5 shadow-glow-purple flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-hologram-purple animate-ping" />
              <span className="font-mono text-xs text-hologram-purple">
                Sexta-Feira está processando parâmetros...
              </span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Sugestões Rápidas de Ação */}
      <div className="px-3 py-1.5 border-t border-hologram-cyan/10 bg-void-900/20 flex gap-2 overflow-x-auto no-scrollbar text-xs font-mono">
        <button
          onClick={() => onSendMessage('Resumo do mês')}
          className="px-2.5 py-1 rounded bg-slate-800/40 hover:bg-hologram-cyan/15 text-slate-300 hover:text-hologram-cyan border border-slate-700/50 hover:border-hologram-cyan/40 transition-colors whitespace-nowrap flex items-center gap-1"
        >
          <TrendingUp className="w-3 h-3 text-hologram-cyan" />
          Resumo do mês
        </button>
        <button
          onClick={() => onSendMessage('Status dos orçamentos')}
          className="px-2.5 py-1 rounded bg-slate-800/40 hover:bg-hologram-cyan/15 text-slate-300 hover:text-hologram-cyan border border-slate-700/50 hover:border-hologram-cyan/40 transition-colors whitespace-nowrap flex items-center gap-1"
        >
          <AlertTriangle className="w-3 h-3 text-amber-400" />
          Orçamentos
        </button>
        <button
          onClick={() => onSendMessage('Dicas de economia')}
          className="px-2.5 py-1 rounded bg-slate-800/40 hover:bg-hologram-cyan/15 text-slate-300 hover:text-hologram-cyan border border-slate-700/50 hover:border-hologram-cyan/40 transition-colors whitespace-nowrap flex items-center gap-1"
        >
          <Sparkles className="w-3 h-3 text-emerald-400" />
          Dicas de economia
        </button>
      </div>

      {/* Barra de Entrada de Texto e Voz */}
      <form onSubmit={handleSubmit} className="p-3 border-t border-hologram-cyan/15 bg-void-950/80 flex items-center gap-2">
        {/* Botão de Interrupção Imediata (Barge-in) se estiver falando */}
        {isSpeaking ? (
          <button
            type="button"
            onClick={onInterrupt}
            className="p-2.5 rounded bg-rose-500/20 border border-rose-500/60 text-rose-400 hover:bg-rose-500 hover:text-white transition-all shadow-glow-red flex items-center gap-1.5 text-xs font-mono animate-pulse"
            title="Interromper fala (Barge-in)"
          >
            <Square className="w-4 h-4 fill-current" />
            <span className="hidden sm:inline">PARAR</span>
          </button>
        ) : (
          <button
            type="button"
            onClick={onToggleVoice}
            className={`p-2.5 rounded transition-all duration-200 border ${
              isRecording
                ? 'bg-rose-500 text-white border-rose-400 shadow-glow-red animate-pulse'
                : 'bg-void-900/60 text-slate-300 border-slate-700/60 hover:border-hologram-cyan/60 hover:text-hologram-cyan'
            }`}
            title={isRecording ? 'Desativar Microfone' : 'Conversar por Voz'}
          >
            {isRecording ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
          </button>
        )}

        <input
          type="text"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder={isRecording ? 'Gravando áudio...' : 'Digite uma instrução ou mensagem...'}
          disabled={isRecording}
          className="flex-1 bg-void-900/60 border border-slate-700/60 focus:border-hologram-cyan/60 rounded px-3.5 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-1 focus:ring-hologram-cyan/30 transition-all font-sans"
        />

        <button
          type="submit"
          disabled={!inputText.trim()}
          className="p-2.5 rounded bg-hologram-cyan/20 border border-hologram-cyan/50 text-hologram-cyan hover:bg-hologram-cyan hover:text-void-950 transition-all disabled:opacity-30 disabled:cursor-not-allowed shadow-glow-cyan"
          title="Enviar Mensagem"
        >
          <Send className="w-4 h-4" />
        </button>
      </form>
    </div>
  );
};
