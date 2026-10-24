import React, { useState } from 'react';
import { api } from '../../services/api';
import { UserProfile } from '../../types';
import { Lock, Mail, User, Shield, AlertCircle } from 'lucide-react';

interface AuthModalProps {
  onSuccess: (user: UserProfile) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({ onSuccess }) => {
  const [isRegister, setIsRegister] = useState(false);
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (isRegister) {
        if (!nome.trim()) throw new Error('Nome é obrigatório.');
        const res = await api.auth.register(nome.trim(), email.trim(), senha);
        onSuccess(res.usuario);
      } else {
        const res = await api.auth.login(email.trim(), senha);
        onSuccess(res.usuario);
      }
    } catch (err: any) {
      setError(err.message || 'Falha na autenticação.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-void-950/90 backdrop-blur-xl">
      <div className="w-full max-w-md bg-void-950 border border-hologram-cyan/35 rounded-2xl shadow-glow-cyan/20 p-6 sm:p-8 relative overflow-hidden">
        {/* Glow de fundo */}
        <div className="absolute -top-24 -left-24 w-48 h-48 bg-hologram-cyan/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -right-24 w-48 h-48 bg-hologram-purple/20 rounded-full blur-3xl pointer-events-none" />

        {/* Header */}
        <div className="text-center mb-6">
          <div className="inline-flex p-3 rounded-xl border border-hologram-cyan/40 bg-hologram-cyan/10 text-hologram-cyan mb-3 shadow-glow-cyan">
            <Shield className="w-6 h-6" />
          </div>
          <h2 className="font-mono text-lg font-bold tracking-widest text-slate-100">
            SEXTA-FEIRA // IDENTIDADE
          </h2>
          <p className="mt-1 text-xs text-slate-400 font-sans">
            {isRegister
              ? 'Cadastre sua conta para sincronização holográfica'
              : 'Autentique-se com credenciais seguras'}
          </p>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/40 text-rose-300 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {isRegister && (
            <div>
              <label className="block text-xs font-mono text-slate-300 mb-1.5">
                NOME COMPLETO
              </label>
              <div className="relative">
                <User className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
                <input
                  type="text"
                  required
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  placeholder="Ex: Tony Stark"
                  className="w-full bg-void-900/80 border border-slate-700 focus:border-hologram-cyan/60 rounded-lg pl-9 pr-3.5 py-2 text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-hologram-cyan/30"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-mono text-slate-300 mb-1.5">
              E-MAIL
            </label>
            <div className="relative">
              <Mail className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="seu.email@exemplo.com"
                className="w-full bg-void-900/80 border border-slate-700 focus:border-hologram-cyan/60 rounded-lg pl-9 pr-3.5 py-2 text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-hologram-cyan/30"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-mono text-slate-300 mb-1.5">
              SENHA
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
              <input
                type="password"
                required
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                placeholder="••••••••••••"
                className="w-full bg-void-900/80 border border-slate-700 focus:border-hologram-cyan/60 rounded-lg pl-9 pr-3.5 py-2 text-sm text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-hologram-cyan/30"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 mt-2 rounded-lg bg-hologram-cyan text-void-950 font-mono text-xs font-bold tracking-wider hover:bg-cyan-300 transition-colors shadow-glow-cyan disabled:opacity-50"
          >
            {loading ? 'AUTENTICANDO...' : isRegister ? 'CRIAR CONTA' : 'ACESSAR ASSISTENTE'}
          </button>
        </form>

        <div className="mt-5 text-center">
          <button
            type="button"
            onClick={() => {
              setIsRegister(!isRegister);
              setError(null);
            }}
            className="text-xs font-mono text-slate-400 hover:text-hologram-cyan transition-colors"
          >
            {isRegister
              ? 'Já possui uma conta? Faça login'
              : 'Novo por aqui? Crie seu perfil de assistente'}
          </button>
        </div>
      </div>
    </div>
  );
};
