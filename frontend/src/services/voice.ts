/**
 * VoiceProvider — Pipeline de Voz Híbrido, VAD, Barge-in e Síntese Natural
 * 
 * - Seleção inteligente de voz feminina natural em português do Brasil (evita voz masculina robótica).
 * - Higienização fonética avançada: converte R$ em 'reais', remove markdown, URLs e pontuação truncada.
 * - Barge-in real: interrupção instantânea ao acionar o microfone ou receber nova fala.
 * - Arquitetura extensível (TTSProvider) com suporte nativo Web Speech e gancho para ElevenLabs / Azure / OpenAI.
 */

export interface VoiceCallback {
  onTranscript: (text: string, isFinal: boolean) => void;
  onAudioLevel: (level: number, frequencyData: Uint8Array) => void;
  onStateChange: (state: 'IDLE' | 'LISTENING' | 'SPEAKING') => void;
  onError: (error: string) => void;
}

export interface TTSProvider {
  name: string;
  speak(text: string, onEnd?: () => void): void;
  stop(): void;
}

/**
 * Higieniza o texto bruto antes de enviar para síntese fonética.
 * Converte moedas, remove markdown, stripa URLs e suaviza pontuação para criar pausas naturais.
 */
export function sanitizeForSpeech(text: string): string {
  if (!text) return '';

  let t = text;

  // 1. Remove URLs completamente (evita falar http dois pontos barra barra...)
  t = t.replace(/https?:\/\/[^\s)]+/gi, '');

  // 2. Remove emojis e caracteres gráficos
  t = t.replace(/[\u{1F600}-\u{1F64F}\u{1F300}-\u{1F5FF}\u{1F680}-\u{1F6FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}\u{1F900}-\u{1F9FF}]/gu, '');

  // 3. Converte valores monetários: R$ 500,00 -> 500 reais; R$ 80,50 -> 80 reais e 50 centavos
  t = t.replace(/r\$\s*(\d+(?:\.\d{3})*),(\d{2})/gi, (_, inteiros, centavos) => {
    const limpo = inteiros.replace(/\./g, '');
    if (centavos === '00') {
      return `${limpo} reais`;
    }
    return `${limpo} reais e ${parseInt(centavos, 10)} centavos`;
  });
  t = t.replace(/r\$\s*(\d+(?:\.\d{3})*(?:,\d+)?)/gi, (_, valor) => {
    return `${valor.replace(',', '.')} reais`;
  });

  // 4. Converte porcentagens: 80% -> 80 por cento
  t = t.replace(/(\d+(?:[.,]\d+)?)\s*%/g, '$1 por cento');

  // 5. Converte termos monetários / financeiros em pronúncia natural
  t = t.replace(/\bBRL\b/gi, 'reais');
  t = t.replace(/\bUSD\b/gi, 'dólares');

  // 6. Remove sintaxe de Markdown: negrito, itálico, cabeçalhos, links, listas
  t = t.replace(/`([^`]+)`/g, '$1');
  t = t.replace(/\*\*([^*]+)\*\*/g, '$1');
  t = t.replace(/\*([^*]+)\*/g, '$1');
  t = t.replace(/__([^_]+)__/g, '$1');
  t = t.replace(/_([^_]+)_/g, '$1');
  t = t.replace(/^#+\s+/gm, '');
  t = t.replace(/^[ \t]*[-*•]\s+/gm, '');
  t = t.replace(/\[([^\]]+)\]\([^)]+\)/g, '$1');
  t = t.replace(/^[ \t]*>\s+/gm, '');

  // 7. Normaliza pausas e pontuações excessivas
  t = t.replace(/\.{2,}/g, '. ');
  t = t.replace(/!{2,}/g, '! ');
  t = t.replace(/\?{2,}/g, '? ');
  t = t.replace(/\s+/g, ' ').trim();

  return t;
}

/**
 * Seleciona a melhor voz feminina brasileira disponível no sistema.
 */
export function selectBestFemaleVoice(voices: SpeechSynthesisVoice[]): SpeechSynthesisVoice | null {
  if (!voices || voices.length === 0) return null;

  // Lista de prioridades para vozes femininas naturais em pt-BR
  const femaleKeywords = [
    'francisca', // Microsoft Francisca Online (Natural) - pt-BR
    'maria',     // Microsoft Maria - pt-BR
    'yara',      // Microsoft Yara - pt-BR
    'helena',    // Helena - pt-BR
    'luciana',   // Luciana - pt-BR
    'leticia',   // Letícia - pt-BR
    'victoria',  // Victoria - pt-BR
    'google português do brasil', // Google pt-BR feminino
  ];

  const ptVoices = voices.filter((v) => {
    const lang = v.lang.replace('_', '-').toLowerCase();
    return lang.startsWith('pt-br') || lang.startsWith('pt');
  });

  // 1. Busca por vozes reconhecidamente femininas
  for (const kw of femaleKeywords) {
    const match = ptVoices.find((v) => {
      const name = v.name.toLowerCase();
      return (
        name.includes(kw) &&
        !name.includes('daniel') &&
        !name.includes('antonio') &&
        !name.includes('felipe') &&
        !name.includes('male')
      );
    });
    if (match) return match;
  }

  // 2. Busca genérica por voz feminina em pt-BR
  const genericFemale = ptVoices.find((v) => {
    const name = v.name.toLowerCase();
    return (
      (name.includes('female') || name.includes('mulher')) &&
      !name.includes('daniel') &&
      !name.includes('male')
    );
  });
  if (genericFemale) return genericFemale;

  // 3. Fallback: qualquer voz pt-BR que não seja explicitamente masculina
  const nonMale = ptVoices.find((v) => {
    const name = v.name.toLowerCase();
    return !name.includes('daniel') && !name.includes('male') && !name.includes('antonio');
  });
  if (nonMale) return nonMale;

  // 4. Último caso
  return ptVoices[0] || null;
}

export class VoiceService {
  private recognition: any = null;
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private micStream: MediaStream | null = null;
  private animFrameId: number | null = null;
  private ttsSimInterval: any = null;
  private isListening = false;
  private isSpeaking = false;
  private callbacks: VoiceCallback | null = null;

  constructor() {
    this.initSpeechRecognition();
  }

  private initSpeechRecognition() {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (SpeechRecognition) {
      this.recognition = new SpeechRecognition();
      this.recognition.continuous = true;
      this.recognition.interimResults = true;
      this.recognition.lang = 'pt-BR';

      this.recognition.onresult = (event: any) => {
        let interimTranscript = '';
        let finalTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTranscript += event.results[i][0].transcript;
          } else {
            interimTranscript += event.results[i][0].transcript;
          }
        }

        if (finalTranscript && this.callbacks) {
          this.callbacks.onTranscript(finalTranscript.trim(), true);
        } else if (interimTranscript && this.callbacks) {
          this.callbacks.onTranscript(interimTranscript.trim(), false);
        }
      };

      this.recognition.onerror = (event: any) => {
        if (event.error !== 'no-speech' && this.callbacks) {
          this.callbacks.onError(`Erro no microfone: ${event.error}`);
        }
      };

      this.recognition.onend = () => {
        if (this.isListening) {
          try {
            this.recognition.start();
          } catch {
            // ignorar
          }
        }
      };
    }
  }

  setCallbacks(cb: VoiceCallback) {
    this.callbacks = cb;
  }

  /**
   * Inicia a escuta por microfone com Barge-in imediato.
   */
  async startListening(): Promise<boolean> {
    // Interrompe imediatamente qualquer fala em andamento (Barge-in instantâneo)
    this.stopSpeaking();

    try {
      this.micStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });

      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      const source = this.audioContext.createMediaStreamSource(this.micStream);
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 64;
      source.connect(this.analyser);

      this.isListening = true;
      if (this.recognition) {
        try {
          this.recognition.start();
        } catch {
          // pode já estar rodando
        }
      }

      this.startAudioLoop();
      this.callbacks?.onStateChange('LISTENING');
      return true;
    } catch {
      this.callbacks?.onError('Permissão de microfone negada ou dispositivo não disponível.');
      return false;
    }
  }

  stopListening() {
    this.isListening = false;
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch {
        // ignorar
      }
    }

    if (this.micStream) {
      this.micStream.getTracks().forEach((track) => track.stop());
      this.micStream = null;
    }

    if (this.animFrameId) {
      cancelAnimationFrame(this.animFrameId);
      this.animFrameId = null;
    }

    if (this.audioContext) {
      this.audioContext.close().catch(() => {});
      this.audioContext = null;
    }

    this.callbacks?.onStateChange('IDLE');
  }

  private startAudioLoop() {
    if (!this.analyser) return;

    const dataArray = new Uint8Array(this.analyser.frequencyBinCount);

    const checkAudio = () => {
      if (!this.isListening && !this.isSpeaking) return;

      this.analyser?.getByteFrequencyData(dataArray);

      let sum = 0;
      for (let i = 0; i < dataArray.length; i++) {
        sum += dataArray[i];
      }
      const average = sum / dataArray.length;
      const level = Math.min(1.0, average / 128.0);

      this.callbacks?.onAudioLevel(level, dataArray);
      this.animFrameId = requestAnimationFrame(checkAudio);
    };

    checkAudio();
  }

  /**
   * Sintetiza áudio com voz feminina natural, fluida e sanitizada.
   */
  speak(text: string, onEnd?: () => void) {
    if (!('speechSynthesis' in window)) {
      onEnd?.();
      return;
    }

    // Barge-in: Interrompe qualquer fala anterior imediatamente
    this.stopSpeaking();

    const cleanText = sanitizeForSpeech(text);
    if (!cleanText) {
      onEnd?.();
      return;
    }

    const utterance = new SpeechSynthesisUtterance(cleanText);
    utterance.lang = 'pt-BR';
    utterance.rate = 1.02; // Cadência natural, sem pressa
    utterance.pitch = 1.02; // Tom feminino natural e caloroso

    // Seleciona a melhor voz: prioriza preferência salva no localStorage, senão voz feminina recomendada
    const preferredUri = localStorage.getItem('sexta_preferred_voice');
    const voices = window.speechSynthesis.getVoices();
    let selectedVoice: SpeechSynthesisVoice | null = null;
    if (preferredUri) {
      selectedVoice = voices.find((v) => v.voiceURI === preferredUri || v.name === preferredUri) || null;
    }
    if (!selectedVoice) {
      selectedVoice = selectBestFemaleVoice(voices);
    }
    if (selectedVoice) {
      utterance.voice = selectedVoice;
    }

    this.isSpeaking = true;
    this.callbacks?.onStateChange('SPEAKING');

    // Simula telemetria harmônica orgânica e suave de fala humana para alimentar o holograma 3D
    if (this.ttsSimInterval) clearInterval(this.ttsSimInterval);
    let simPhase = 0;
    this.ttsSimInterval = setInterval(() => {
      if (!this.isSpeaking) {
        clearInterval(this.ttsSimInterval);
        return;
      }
      simPhase += 0.28;
      // Modulação envelope senoidal suave que imita fonemas reais sem picos randômicos bruscos
      const envelope = 0.38 + 0.18 * Math.sin(simPhase) + 0.08 * Math.sin(simPhase * 2.3);
      const simLevel = Math.max(0.18, Math.min(0.68, envelope));
      const simData = new Uint8Array(32);
      for (let i = 0; i < simData.length; i++) {
        const falloff = Math.exp(-i / 10);
        simData[i] = Math.floor((130 + 70 * Math.sin(simPhase + i * 0.35)) * falloff * simLevel);
      }
      this.callbacks?.onAudioLevel(simLevel, simData);
    }, 55);

    const handleEnd = () => {
      this.isSpeaking = false;
      if (this.ttsSimInterval) {
        clearInterval(this.ttsSimInterval);
        this.ttsSimInterval = null;
      }
      this.callbacks?.onStateChange('IDLE');
      onEnd?.();
    };

    utterance.onend = handleEnd;
    utterance.onerror = handleEnd;

    window.speechSynthesis.speak(utterance);
  }

  /**
   * Cancela a síntese imediatamente (Barge-in manual ou acionado por evento).
   */
  stopSpeaking() {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    this.isSpeaking = false;
    if (this.ttsSimInterval) {
      clearInterval(this.ttsSimInterval);
      this.ttsSimInterval = null;
    }
    this.callbacks?.onStateChange('IDLE');
  }

  getAvailableVoices(): SpeechSynthesisVoice[] {
    if (!('speechSynthesis' in window)) return [];
    return window.speechSynthesis.getVoices();
  }

  getPreferredVoiceURI(): string | null {
    return localStorage.getItem('sexta_preferred_voice');
  }

  setPreferredVoiceURI(uri: string | null) {
    if (uri) {
      localStorage.setItem('sexta_preferred_voice', uri);
    } else {
      localStorage.removeItem('sexta_preferred_voice');
    }
  }
}

export const voiceService = new VoiceService();
