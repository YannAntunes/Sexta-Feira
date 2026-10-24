export type HologramState = 
  | 'IDLE' 
  | 'LISTENING' 
  | 'THINKING' 
  | 'SPEAKING' 
  | 'PROCESSING' 
  | 'ERROR';

export type PersonalityType = 
  | 'SEXTA_FEIRA' 
  | 'FORMAL' 
  | 'AMIGAVEL' 
  | 'JARVIS';

export interface UserProfile {
  id: number;
  nome: string;
  email: string;
  idiomaPreferido: string;
  personalidade: PersonalityType;
  criadoEm?: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  usuario: UserProfile;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  intent?: string;
  isVoice?: boolean;
}

export interface Notificacao {
  id: number;
  titulo: string;
  mensagem: string;
  tipoAlerta: 'INFO' | 'AVISO_80' | 'ALERTA_90' | 'ESTOURO' | 'SISTEMA';
  lida: boolean;
  criadoEm: string;
}

export interface ResumoFinanceiro {
  mes: number;
  ano: number;
  totalDespesas: number;
  totalReceitas: number;
  saldo: number;
}

export interface StatusOrcamento {
  categoria: string;
  mes: number;
  ano: number;
  limite: number;
  gasto: number;
  restante: number;
  estourado: boolean;
}
