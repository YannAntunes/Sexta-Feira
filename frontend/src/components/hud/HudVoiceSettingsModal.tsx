import React, { useEffect, useState } from 'react';
import { voiceService, selectBestFemaleVoice, sanitizeForSpeech } from '../../services/voice';
import {
  X,
  Volume2,
  VolumeX,
  Check,
  Sparkles,
  Info,
  Play,
  RotateCcw,
  Cpu,
  Cloud,
} from 'lucide-react';

interface HudVoiceSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const HudVoiceSettingsModal: React.FC<HudVoiceSettingsModalProps> = ({
  isOpen,
  onClose,
}) => {
  const [voices, setVoices] = useState<SpeechSynthesisVoice[]>([]);
  const [selectedUri, setSelectedUri] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [playingVoiceName, setPlayingVoiceName] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'voices' | 'architecture'>('voices');

  const SAMPLE_PT = 'Olá! Eu sou a Sexta-Feira, sua assistente pessoal. O que faremos com seus 1.500 reais hoje?';
  const SAMPLE_EN = 'Hello! I am Friday, your personal assistant. How can I optimize your budget today?';

  const loadVoices = () => {
    if (!('speechSynthesis' in window)) return;
    const allVoices = window.speechSynthesis.getVoices();
    setVoices(allVoices);
    const saved = voiceService.getPreferredVoiceURI();
    if (saved) {
      setSelectedUri(saved);
    } else {
      const best = selectBestFemaleVoice(allVoices);
      if (best) setSelectedUri(best.voiceURI);
    }
  };

  useEffect(() => {
    if (isOpen) {
      loadVoices();
      if ('speechSynthesis' in window) {
        window.speechSynthesis.onvoiceschanged = loadVoices;
      }
    } else {
      stopPreview();
    }
  }, [isOpen]);

  const stopPreview = () => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    setIsPlaying(false);
    setPlayingVoiceName(null);
  };

  const playPreview = (voice: SpeechSynthesisVoice, langSample: 'pt' | 'en' = 'pt') => {
    stopPreview();
    const text = langSample === 'en' ? SAMPLE_EN : SAMPLE_PT;
    const utterance = new SpeechSynthesisUtterance(sanitizeForSpeech(text));
    utterance.voice = voice;
    utterance.lang = voice.lang;
    utterance.rate = 1.02;
    utterance.pitch = 1.02;

    utterance.onend = () => {
      setIsPlaying(false);
      setPlayingVoiceName(null);
    };
    utterance.onerror = () => {
      setIsPlaying(false);
      setPlayingVoiceName(null);
    };

    setIsPlaying(true);
    setPlayingVoiceName(voice.name);
    window.speechSynthesis.speak(utterance);
  };

  const handleSelectVoice = (uri: string) => {
    setSelectedUri(uri);
    voiceService.setPreferredVoiceURI(uri);
  };

  const handleResetToAuto = () => {
    voiceService.setPreferredVoiceURI(null);
    const best = selectBestFemaleVoice(voices);
    setSelectedUri(best ? best.voiceURI : null);
  };

  if (!isOpen) return null;

  // Filtra e organiza vozes: pt-BR primeiro, depois en-US, depois outras
  const ptVoices = voices.filter((v) => v.lang.toLowerCase().startsWith('pt'));
  const enVoices = voices.filter((v) => v.lang.toLowerCase().startsWith('en'));
  const bestFemale = selectBestFemaleVoice(voices);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-void-950/80 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-3xl bg-void-900 border border-hologram-cyan/30 rounded-xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden text-slate-100">
        {/* Header do Modal */}
        <div className="px-5 py-4 border-b border-hologram-cyan/20 flex items-center justify-between select-none bg-void-950/60">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded bg-hologram-cyan/10 border border-hologram-cyan/40 text-hologram-cyan shadow-glow-cyan/20">
              <Volume2 className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h2 className="font-mono text-base font-bold tracking-wider text-slate-100 flex items-center gap-2">
                CONFIGURAÇÃO DE VOZ & SÍNTESE // SEXTA-FEIRA
              </h2>
              <p className="text-xs font-mono text-slate-400">
                Seleção de voz local nativa, amostragem acústica e arquitetura neural
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {isPlaying && (
              <button
                onClick={stopPreview}
                className="px-2.5 py-1.5 rounded text-xs font-mono bg-rose-500/20 text-rose-400 border border-rose-500/40 hover:bg-rose-500/30 transition-all flex items-center gap-1.5"
              >
                <VolumeX className="w-3.5 h-3.5" />
                Interromper Áudio
              </button>
            )}
            <button
              onClick={onClose}
              className="p-2 rounded border border-slate-700 text-slate-400 hover:text-rose-400 hover:border-rose-500/50 transition-colors"
              title="Fechar"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Abas */}
        <div className="px-5 py-2.5 border-b border-hologram-cyan/15 bg-void-950/40 flex items-center justify-between text-xs font-mono select-none">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setActiveTab('voices')}
              className={`px-3 py-1.5 rounded transition-all flex items-center gap-2 ${
                activeTab === 'voices'
                  ? 'bg-hologram-cyan/20 text-hologram-cyan border border-hologram-cyan/50 shadow-glow-cyan/20'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Cpu className="w-3.5 h-3.5" />
              Vozes Instaladas no Sistema ({voices.length})
            </button>
            <button
              onClick={() => setActiveTab('architecture')}
              className={`px-3 py-1.5 rounded transition-all flex items-center gap-2 ${
                activeTab === 'architecture'
                  ? 'bg-hologram-cyan/20 text-hologram-cyan border border-hologram-cyan/50 shadow-glow-cyan/20'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Cloud className="w-3.5 h-3.5" />
              Arquitetura TTS (Nativa vs Neural)
            </button>
          </div>

          <button
            onClick={handleResetToAuto}
            className="text-[11px] text-slate-400 hover:text-hologram-cyan transition-colors flex items-center gap-1"
            title="Restaurar voz feminina inteligente sugerida"
          >
            <RotateCcw className="w-3 h-3" />
            Auto Seleção
          </button>
        </div>

        {/* Conteúdo */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4 custom-scrollbar">
          {activeTab === 'voices' ? (
            <>
              <div className="p-3 rounded-lg bg-void-950/70 border border-slate-800 text-xs font-mono text-slate-300 flex items-start gap-2.5">
                <Info className="w-4 h-4 text-hologram-cyan shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold text-slate-100 mb-0.5">Vozes Disponíveis no seu Sistema Operacional</p>
                  <p className="text-slate-400 text-[11px]">
                    Para a experiência natural ideal em português brasileiro, utilize vozes femininas como{' '}
                    <span className="text-hologram-cyan">Microsoft Francisca (Natural)</span>,{' '}
                    <span className="text-hologram-cyan">Maria</span> ou{' '}
                    <span className="text-hologram-cyan">Google português do Brasil</span>.
                  </p>
                </div>
              </div>

              {/* Seção Português do Brasil */}
              <div className="space-y-2">
                <h3 className="text-xs font-mono text-hologram-cyan uppercase tracking-wider flex items-center gap-1.5">
                  <span>🇧🇷</span> Português do Brasil ({ptVoices.length})
                </h3>

                {ptVoices.length === 0 ? (
                  <div className="p-4 text-center font-mono text-xs text-slate-500 bg-void-950/40 rounded border border-slate-800">
                    Nenhuma voz em português brasileiro detectada no navegador. Instale o pacote de fala pt-BR nas configurações do Windows/SO.
                  </div>
                ) : (
                  <div className="grid grid-cols-1 gap-2">
                    {ptVoices.map((voice) => {
                      const isSelected = selectedUri === voice.voiceURI || selectedUri === voice.name;
                      const isRecommended = bestFemale?.voiceURI === voice.voiceURI;
                      const isVoicePlaying = isPlaying && playingVoiceName === voice.name;

                      return (
                        <div
                          key={voice.voiceURI || voice.name}
                          className={`p-3 rounded-lg border transition-all flex items-center justify-between ${
                            isSelected
                              ? 'bg-hologram-cyan/10 border-hologram-cyan text-slate-100 shadow-glow-cyan/10'
                              : 'bg-void-950/60 border-slate-800 hover:border-slate-700 text-slate-300'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <button
                              onClick={() => handleSelectVoice(voice.voiceURI)}
                              className={`w-5 h-5 rounded-full border flex items-center justify-center transition-all ${
                                isSelected
                                  ? 'border-hologram-cyan bg-hologram-cyan text-void-950 font-bold'
                                  : 'border-slate-600 hover:border-slate-400'
                              }`}
                            >
                              {isSelected && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                            </button>

                            <div>
                              <div className="text-xs font-semibold flex items-center gap-2">
                                <span>{voice.name}</span>
                                {isRecommended && (
                                  <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-hologram-cyan/20 text-hologram-cyan border border-hologram-cyan/40 flex items-center gap-1">
                                    <Sparkles className="w-2.5 h-2.5" />
                                    Recomendada
                                  </span>
                                )}
                              </div>
                              <div className="text-[10px] font-mono text-slate-400">
                                {voice.lang} • {voice.localService ? 'Processamento Local' : 'Rede'}
                              </div>
                            </div>
                          </div>

                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => playPreview(voice, 'pt')}
                              disabled={isVoicePlaying}
                              className={`px-2.5 py-1 rounded text-[11px] font-mono border transition-all flex items-center gap-1 ${
                                isVoicePlaying
                                  ? 'border-hologram-cyan bg-hologram-cyan/20 text-hologram-cyan animate-pulse'
                                  : 'border-slate-700 bg-slate-800/60 text-slate-300 hover:border-hologram-cyan/50 hover:text-hologram-cyan'
                              }`}
                            >
                              <Play className="w-3 h-3 fill-current" />
                              {isVoicePlaying ? 'Reproduzindo...' : 'Testar pt-BR'}
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Seção Inglês */}
              <div className="space-y-2 pt-3">
                <h3 className="text-xs font-mono text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <span>🇺🇸</span> Inglês / Internacional ({enVoices.length})
                </h3>

                <div className="grid grid-cols-1 gap-2 max-h-56 overflow-y-auto custom-scrollbar">
                  {enVoices.map((voice) => {
                    const isSelected = selectedUri === voice.voiceURI || selectedUri === voice.name;
                    const isVoicePlaying = isPlaying && playingVoiceName === voice.name;

                    return (
                      <div
                        key={voice.voiceURI || voice.name}
                        className={`p-2.5 rounded-lg border transition-all flex items-center justify-between ${
                          isSelected
                            ? 'bg-hologram-cyan/10 border-hologram-cyan text-slate-100'
                            : 'bg-void-950/40 border-slate-800/80 hover:border-slate-700 text-slate-300'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <button
                            onClick={() => handleSelectVoice(voice.voiceURI)}
                            className={`w-4 h-4 rounded-full border flex items-center justify-center transition-all ${
                              isSelected
                                ? 'border-hologram-cyan bg-hologram-cyan text-void-950'
                                : 'border-slate-600 hover:border-slate-400'
                            }`}
                          >
                            {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                          </button>
                          <div className="text-xs">
                            <span className="font-medium text-slate-200">{voice.name}</span>
                            <span className="text-[10px] font-mono text-slate-500 ml-2">({voice.lang})</span>
                          </div>
                        </div>

                        <button
                          onClick={() => playPreview(voice, 'en')}
                          disabled={isVoicePlaying}
                          className="px-2 py-0.5 rounded text-[11px] font-mono border border-slate-700 bg-slate-800/40 text-slate-300 hover:border-hologram-cyan/50 hover:text-hologram-cyan transition-all flex items-center gap-1"
                        >
                          <Play className="w-2.5 h-2.5 fill-current" />
                          Testar en
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>
            </>
          ) : (
            /* TAB: ARQUITETURA TTS */
            <div className="space-y-5">
              <div className="p-4 rounded-lg bg-void-950/70 border border-slate-800 space-y-3">
                <h3 className="text-xs font-mono font-bold text-hologram-cyan uppercase tracking-wider flex items-center gap-2">
                  <Cpu className="w-4 h-4" />
                  Comparativo de Motores de Síntese de Voz (TTS)
                </h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  A Sexta-Feira foi desenvolvida com abstração de provedor de fala (<span className="font-mono text-hologram-cyan">TTSProvider</span>).
                  Abaixo está o diagnóstico comparativo entre o motor nativo atual e os motores neurais em nuvem.
                </p>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
                  <div className="p-3.5 rounded bg-void-900 border border-emerald-500/30 space-y-2">
                    <div className="flex items-center justify-between text-xs font-mono text-emerald-400 font-bold">
                      <span>WEB SPEECH API (NATIVA)</span>
                      <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-[10px]">ATIVO HOJE</span>
                    </div>
                    <ul className="text-xs space-y-1.5 text-slate-300 font-mono">
                      <li>• <strong>Latência:</strong> 0ms (instantânea, sem round-trip)</li>
                      <li>• <strong>Custo:</strong> R$ 0,00 (gratuita e ilimitada)</li>
                      <li>• <strong>Privacidade:</strong> Totalmente processada no dispositivo</li>
                      <li>• <strong>Offline:</strong> Funciona sem conexão à internet</li>
                      <li>• <strong>Limitação:</strong> Depende dos pacotes de voz instalados no SO</li>
                    </ul>
                  </div>

                  <div className="p-3.5 rounded bg-void-900 border border-indigo-500/30 space-y-2">
                    <div className="flex items-center justify-between text-xs font-mono text-indigo-400 font-bold">
                      <span>NEURAL TTS STREAMING</span>
                      <span className="px-1.5 py-0.5 rounded bg-indigo-500/20 text-[10px]">EXTENSÃO SUPORTADA</span>
                    </div>
                    <ul className="text-xs space-y-1.5 text-slate-300 font-mono">
                      <li>• <strong>Latência:</strong> 250ms - 450ms (streaming HTTP)</li>
                      <li>• <strong>Custo:</strong> ~US$ 15-30 por 1M caracteres (ElevenLabs / Azure)</li>
                      <li>• <strong>Prosódia:</strong> Hiper-realista com respiração e ênfase emocional</li>
                      <li>• <strong>Dependência:</strong> Requer chave de API e conexão banda larga</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Rodapé */}
        <div className="px-5 py-3 border-t border-hologram-cyan/15 bg-void-950/80 flex items-center justify-between text-[11px] font-mono text-slate-400">
          <span>BARGE-IN ATIVO // INTERRUPÇÃO IMEDIATA</span>
          <span>PREFERÊNCIAS SALVAS LOCALMENTE</span>
        </div>
      </div>
    </div>
  );
};
