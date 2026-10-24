import { AuthTokens, BalancoMensal, ChatMessage, Notificacao, ResumoFinanceiro, StatusOrcamento, UserProfile } from '../types';

const API_BASE = '/api/v1';

class ApiService {
  private tokenKey = 'sexta_access_token';
  private refreshKey = 'sexta_refresh_token';

  getAccessToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  setTokens(access: string, refresh: string): void {
    localStorage.setItem(this.tokenKey, access);
    localStorage.setItem(this.refreshKey, refresh);
  }

  clearTokens(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshKey);
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers || {});
    if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }

    const token = this.getAccessToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    let res = await fetch(`${API_BASE}${endpoint}`, {
      ...options,
      headers,
    });

    // Se 401 Unauthorized e tiver refresh token, tenta renovar
    if (res.status === 401 && localStorage.getItem(this.refreshKey)) {
      const refreshed = await this.refreshToken();
      if (refreshed) {
        headers.set('Authorization', `Bearer ${this.getAccessToken()}`);
        res = await fetch(`${API_BASE}${endpoint}`, {
          ...options,
          headers,
        });
      }
    }

    if (!res.ok) {
      const errorText = await res.text();
      let errorJson: { error?: string } = {};
      try {
        errorJson = JSON.parse(errorText);
      } catch {
        // fallback
      }
      throw new Error(errorJson.error || errorText || `Erro na requisição: ${res.status}`);
    }

    return res.json() as Promise<T>;
  }

  async refreshToken(): Promise<boolean> {
    const refreshToken = localStorage.getItem(this.refreshKey);
    if (!refreshToken) return false;

    try {
      const res = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (!res.ok) {
        this.clearTokens();
        return false;
      }

      const data: AuthTokens = await res.json();
      this.setTokens(data.accessToken, data.refreshToken);
      return true;
    } catch {
      this.clearTokens();
      return false;
    }
  }

  // --- Módulos de Autenticação & Usuário ---
  auth = {
    login: async (email: string, senha: string): Promise<AuthTokens> => {
      const data = await this.request<AuthTokens>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, senha }),
      });
      this.setTokens(data.accessToken, data.refreshToken);
      return data;
    },

    register: async (nome: string, email: string, senha: string, idioma = 'pt'): Promise<AuthTokens> => {
      const data = await this.request<AuthTokens>('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ nome, email, senha, idiomaPreferido: idioma }),
      });
      this.setTokens(data.accessToken, data.refreshToken);
      return data;
    },

    getProfile: async (): Promise<UserProfile> => {
      return this.request<UserProfile>('/usuario/perfil');
    },

    updateProfile: async (dados: { nome?: string; idiomaPreferido?: string; personalidade?: string }): Promise<UserProfile> => {
      return this.request<UserProfile>('/usuario/perfil', {
        method: 'PUT',
        body: JSON.stringify(dados),
      });
    },
  };

  // --- Módulo Conversacional ---
  chat = {
    send: async (mensagem: string): Promise<{ resposta: string }> => {
      return this.request<{ resposta: string }>('/chat', {
        method: 'POST',
        body: JSON.stringify({ mensagem }),
      });
    },

    getHistory: async (limit = 20): Promise<ChatMessage[]> => {
      const raw = await this.request<Array<{ role: 'user' | 'assistant'; content: string }>>(`/mensagens/historico?limite=${limit}`);
      return raw.map((item, index) => ({
        id: `msg-${index}-${Date.now()}`,
        role: item.role,
        content: item.content,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      }));
    },
  };

  // --- Módulo de Notificações In-App ---
  notifications = {
    list: async (somenteNaoLidas = false): Promise<Notificacao[]> => {
      return this.request<Notificacao[]>(`/notificacoes?somenteNaoLidas=${somenteNaoLidas}`);
    },

    countUnread: async (): Promise<number> => {
      const data = await this.request<{ count: number }>('/notificacoes/nao-lidas/count');
      return data.count;
    },

    markAsRead: async (id: number): Promise<void> => {
      await this.request(`/notificacoes/${id}/lida`, { method: 'PATCH' });
    },
  };

  // --- Módulo Financeiro & Telemetria ---
  finance = {
    getMonthSummary: async (ano?: number, mes?: number): Promise<ResumoFinanceiro> => {
      const query = (ano && mes) ? `?ano=${ano}&mes=${mes}` : '';
      return this.request<ResumoFinanceiro>(`/transacoes/resumo-mensal${query}`);
    },

    getBudgetOverview: async (ano?: number, mes?: number): Promise<StatusOrcamento[]> => {
      const query = (ano && mes) ? `?ano=${ano}&mes=${mes}` : '';
      return this.request<StatusOrcamento[]>(`/orcamentos/resumo${query}`);
    },

    getCategoryExpenses: async (): Promise<Record<string, number>> => {
      return this.request<Record<string, number>>('/charts/gastos-categoria/dados');
    },

    getRevenueCategories: async (): Promise<Record<string, number>> => {
      return this.request<Record<string, number>>('/charts/receitas-categoria/dados');
    },

    getMonthlyBalance: async (): Promise<BalancoMensal> => {
      return this.request<BalancoMensal>('/charts/balanco-mensal/dados');
    },

    getBudgetVsExpenses: async (): Promise<Array<{ categoria: string; orcamento: number; gasto: number }>> => {
      return this.request<Array<{ categoria: string; orcamento: number; gasto: number }>>('/charts/orcamento-gasto/dados');
    },

    getDailyExpensesSeries: async (): Promise<Record<string, number>> => {
      return this.request<Record<string, number>>('/charts/serie-gastos/dados');
    },

    getChartPngBlobUrl: async (chartType: 'gastos-mes' | 'orcamento-vs-gasto' | 'evolucao-gastos-mes' | 'carteira'): Promise<string> => {
      const endpoint = chartType === 'carteira' ? '/carteira/grafico?range=THIS_MONTH' : `/charts/${chartType}.png`;
      const token = this.getAccessToken();
      const res = await fetch(`${API_BASE}${endpoint}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!res.ok) throw new Error('Falha ao carregar gráfico');
      const blob = await res.blob();
      return URL.createObjectURL(blob);
    },
  };
}

export const api = new ApiService();
