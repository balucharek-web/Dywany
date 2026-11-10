import { useState } from 'react';
import { User } from 'firebase/auth';
import {
  X,
  ShieldAlert,
  LogIn,
  CheckCircle2,
  RefreshCw,
  LogOut,
  AlertTriangle,
  Briefcase,
  ExternalLink,
  Copy,
  Check,
  Share2
} from 'lucide-react';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';
import { STORE_NUMBER, STORE_CITY } from '../types';

interface LoginModalProps {
  isOpen: boolean;
  onClose: () => void;
  onGoogleSignIn: () => Promise<void>;
  currentUser?: User | null;
  isAdmin?: boolean;
  isSuperAdmin?: boolean;
  superAdminEmail?: string;
  onSwitchAccount?: () => Promise<void>;
  onLogout?: () => Promise<void>;
}

export function LoginModal({
  isOpen,
  onClose,
  onGoogleSignIn,
  currentUser,
  isAdmin,
  isSuperAdmin,
  superAdminEmail,
  onSwitchAccount,
  onLogout
}: LoginModalProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [workProfileCopied, setWorkProfileCopied] = useState(false);
  const [workProfileFeedback, setWorkProfileFeedback] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSwitchToWorkProfile = async () => {
    const currentUrl = window.location.href;
    setWorkProfileFeedback(null);

    // 1. Try native Web Share API (On Android Enterprise, this directly presents the "Służbowe 💼" tab!)
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'LM Dywany - Sklep 038 Mikołów',
          url: currentUrl
        });
        setWorkProfileFeedback('Kliknij zakładkę „Służbowe 💼” na dole ekranu i wybierz aplikację Chrome.');
        return;
      } catch (err: unknown) {
        const e = err as Error;
        if (e?.name === 'AbortError') return; // User closed sheet
        console.warn('Share not completed:', err);
      }
    }

    // 2. Clipboard fallback
    try {
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(currentUrl);
      }
      setWorkProfileCopied(true);
      setWorkProfileFeedback('Link skopiowany! Otwórz przeglądarkę Chrome w profilu służbowym (z walizką 💼) i wklej link.');
      setTimeout(() => setWorkProfileCopied(false), 5000);
    } catch {
      setWorkProfileFeedback('Otwórz aplikację Chrome w profilu służbowym (z walizką 💼) pod adresem: ' + currentUrl);
    }
  };

  const handleSignIn = async () => {
    try {
      setLoading(true);
      setError(null);
      await onGoogleSignIn();
      onClose();
    } catch (err: unknown) {
      const errMsg = err instanceof Error ? err.message : String(err);
      const errCode = (err as { code?: string })?.code || '';
      if (
        errCode === 'auth/popup-closed-by-user' ||
        errCode === 'auth/cancelled-popup-request' ||
        errMsg.includes('popup-closed-by-user') ||
        errMsg.includes('cancelled-popup-request') ||
        errMsg.includes('Pending promise was never set')
      ) {
        // User cancelled or closed the popup - benign action, not an application error
        return;
      }

      console.error('Google Sign In Error details:', err);
      if (errMsg.includes('unauthorized-domain')) {
        setError(
          'Domena balucharek-web.github.io nie jest jeszcze dodana do Autoryzowanych Domen w Firebase Auth. Otwórz aplikację z autoryzowanego serwera Cloud Run (link poniżej), gdzie logowanie działa od ręki.'
        );
        return;
      }
      if (errMsg.includes('popup-blocked')) {
        setError(
          'Przeglądarka zablokowała wyskakujące okno logowania Google. Włącz wyskakujące okna (pop-up) w ustawieniach Chrome lub kliknij ponownie.'
        );
        return;
      }
      if (errMsg.includes('Brak uprawnień')) {
        setError(errMsg);
        return;
      }
      setError(`Problem z logowaniem: ${errMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const handleSwitch = async () => {
    try {
      setLoading(true);
      setError(null);
      if (onSwitchAccount) {
        await onSwitchAccount();
      } else {
        await onGoogleSignIn();
      }
      onClose();
    } catch (err: unknown) {
      const errMsg = err instanceof Error ? err.message : String(err);
      const errCode = (err as { code?: string })?.code || '';
      if (
        errCode === 'auth/popup-closed-by-user' ||
        errCode === 'auth/cancelled-popup-request' ||
        errMsg.includes('popup-closed-by-user') ||
        errMsg.includes('cancelled-popup-request') ||
        errMsg.includes('Pending promise was never set')
      ) {
        return;
      }
      console.error('Switch account error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
      <div className="bg-white rounded-3xl max-w-md w-full shadow-2xl overflow-hidden flex flex-col p-6 sm:p-8">
        {/* Close button */}
        <div className="flex justify-end -mt-2 -mr-2">
          <button
            onClick={onClose}
            className="p-1 rounded-full text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Brand visual matching Screenshot 1 */}
        <div className="flex flex-col items-center text-center">
          <LeroyMerlinLogo className="h-16 mb-2" />
          <h2 className="text-xl sm:text-2xl font-black text-gray-900 tracking-tight">
            System Ekspozycji Dywanów
          </h2>
          <div className="text-xs font-bold text-blue-600 mt-1 uppercase tracking-wider">
            Sklep {STORE_NUMBER} {STORE_CITY}
          </div>
        </div>

        {/* Content Box */}
        <div className="mt-6 bg-gray-50/80 border border-gray-200 rounded-2xl p-5 flex flex-col gap-4">
          {currentUser ? (
            <div className="flex flex-col gap-3">
              <div className={`p-3.5 rounded-xl border text-xs sm:text-sm ${
                isAdmin ? 'bg-emerald-50 border-emerald-200 text-emerald-900' : 'bg-amber-50 border-amber-200 text-amber-900'
              }`}>
                <div className="font-bold flex items-center gap-1.5 mb-1">
                  {isAdmin ? (
                    <>
                      <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                      <span>Zalogowano pomyślnie</span>
                    </>
                  ) : (
                    <>
                      <AlertTriangle className="w-4 h-4 text-amber-600" />
                      <span>Konto bez uprawnień administratora</span>
                    </>
                  )}
                </div>
                <div className="font-mono text-xs break-all bg-white/70 px-2 py-1 rounded border border-black/5 mb-1.5">
                  {currentUser.email}
                </div>
                <div className="text-[11px] leading-relaxed">
                  {isSuperAdmin ? (
                    <span className="font-semibold text-purple-700">Rola: Super Administrator (pełny dostęp do sklepu i magazynów)</span>
                  ) : isAdmin ? (
                    <span className="font-semibold text-blue-700">Rola: Administrator Sklepu {STORE_NUMBER}</span>
                  ) : (
                    <span>
                      Ten adres e-mail nie ma uprawnień do edycji magazynu. Aby uzyskać dostęp, poproś Super Administratora Sklepu {STORE_NUMBER} o dodanie Twojego adresu w zakładce <strong>„Admini”</strong>.
                    </span>
                  )}
                </div>
              </div>

              {/* Action buttons when logged in */}
              <div className="flex flex-col gap-2">
                <button
                  onClick={handleSwitch}
                  disabled={loading}
                  className="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-xl font-bold text-xs sm:text-sm flex items-center justify-center gap-2 transition-all shadow-sm active:scale-98 cursor-pointer"
                >
                  <RefreshCw className="w-4 h-4" />
                  <span>{loading ? 'Przełączanie...' : 'Zmień konto Google'}</span>
                </button>

                {/* Switch to Work Profile button when logged in */}
                <button
                  type="button"
                  onClick={handleSwitchToWorkProfile}
                  className="w-full py-2.5 px-4 bg-gray-900 hover:bg-black text-white rounded-xl font-medium text-xs sm:text-sm flex items-center justify-between transition-all shadow-sm active:scale-98 cursor-pointer border border-gray-800"
                >
                  <div className="flex items-center gap-2">
                    <Briefcase className="w-4 h-4 text-amber-400 shrink-0" />
                    <span>Przełącz na profil służbowy 💼</span>
                  </div>
                  <ExternalLink className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                </button>

                {onLogout && (
                  <button
                    onClick={async () => {
                      await onLogout();
                      onClose();
                    }}
                    className="w-full py-2 px-4 bg-gray-200 hover:bg-gray-300 text-gray-800 rounded-xl font-medium text-xs flex items-center justify-center gap-2 transition-colors cursor-pointer"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>Wyloguj</span>
                  </button>
                )}
              </div>
            </div>
          ) : (
            <>
              <div className="text-xs sm:text-sm text-gray-700 leading-relaxed text-center">
                <span className="font-bold text-gray-900 block mb-1">
                  Logowanie administratora magazynu
                </span>
                Logowanie kontem Google jest wymagane wyłącznie do dodawania, edycji i usuwania pozycji w magazynie. Wyszukiwanie jest dostępne dla wszystkich bez logowania.
              </div>

              {error && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex flex-col gap-2">
                  <div className="flex items-start gap-2">
                    <ShieldAlert className="w-4 h-4 shrink-0 text-red-600 mt-0.5" />
                    <span className="leading-relaxed">{error}</span>
                  </div>
                  {error.includes('Brak uprawnień') && (
                    <button
                      onClick={onClose}
                      className="mt-1 py-2 px-3 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-bold transition-colors cursor-pointer text-center"
                    >
                      Powrót do wyszukiwarki
                    </button>
                  )}
                  {window.location.hostname.includes('github.io') && !error.includes('Brak uprawnień') && (
                    <a
                      href="https://ais-pre-yplwi2gtghtglug5l43wte-12487892955.europe-west2.run.app"
                      className="mt-1 py-2 px-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold flex items-center justify-center gap-2 text-center transition-colors"
                    >
                      🚀 Otwórz wersję z działającym logowaniem Google
                    </a>
                  )}
                </div>
              )}

              {/* Google Sign-in button */}
              <button
                onClick={handleSignIn}
                disabled={loading}
                className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-xl font-bold text-sm sm:text-base flex items-center justify-center gap-3 transition-all shadow-md active:scale-98 cursor-pointer"
              >
                {/* Google G logo */}
                <svg className="w-5 h-5 bg-white rounded-full p-0.5" viewBox="0 0 24 24">
                  <path
                    fill="#4285F4"
                    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                  />
                  <path
                    fill="#34A853"
                    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  />
                  <path
                    fill="#FBBC05"
                    d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                  />
                  <path
                    fill="#EA4335"
                    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                  />
                </svg>
                <span>{loading ? 'Logowanie...' : 'Zaloguj z kontem Google'}</span>
              </button>

              {/* Divider LUB */}
              <div className="relative my-1 flex items-center justify-center">
                <div className="border-t border-gray-200 w-full"></div>
                <span className="bg-gray-50/90 px-2 text-[10px] font-bold text-gray-400 uppercase tracking-wider absolute">
                  LUB
                </span>
              </div>

              {/* Switch to Work Profile tile - exactly like Gmail */}
              <button
                type="button"
                onClick={handleSwitchToWorkProfile}
                className="w-full py-2.5 px-3.5 bg-gray-900 hover:bg-black text-white rounded-2xl font-medium text-xs sm:text-sm flex items-center justify-between transition-all shadow-md active:scale-98 cursor-pointer border border-gray-800 group"
              >
                <div className="flex items-center gap-2.5 text-left">
                  <div className="w-8 h-8 rounded-xl bg-gray-800 flex items-center justify-center text-amber-400 group-hover:scale-105 transition-transform shrink-0">
                    <Briefcase className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-bold text-white text-xs sm:text-sm leading-tight flex items-center gap-1.5">
                      Przełącz na profil służbowy
                      <span className="text-xs">💼</span>
                    </div>
                    <div className="text-[10px] text-gray-400 leading-tight mt-0.5">
                      Otwórz w profilu z walizką na telefonie
                    </div>
                  </div>
                </div>
                <ExternalLink className="w-4 h-4 text-gray-400 group-hover:text-white transition-colors shrink-0 ml-2" />
              </button>
            </>
          )}

          {/* Feedback banner after tapping Work Profile */}
          {workProfileFeedback && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-900 flex items-start gap-2 text-left">
              <CheckCircle2 className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <div className="leading-relaxed">
                {workProfileFeedback}
              </div>
            </div>
          )}

          {superAdminEmail && (
            <div className="text-[11px] text-gray-500 text-center">
              Super Administrator Sklepu: <span className="font-mono font-semibold text-gray-700">{superAdminEmail}</span>
            </div>
          )}

          {/* Android Work Profile / Profil Służbowy Tip */}
          <div className="mt-2 pt-3 border-t border-gray-200">
            <details className="group text-left text-xs text-gray-600 bg-blue-50/60 rounded-xl p-3 border border-blue-100" open={!isAdmin && Boolean(currentUser)}>
              <summary className="font-semibold text-blue-900 cursor-pointer flex items-center justify-between list-none select-none">
                <span className="flex items-center gap-1.5">
                  <span className="text-base">💼</span>
                  <span>Jak logować się profilem służbowym i osobistym?</span>
                </span>
                <span className="text-blue-500 text-[11px] group-open:rotate-180 transition-transform">▼</span>
              </summary>
              <div className="mt-2 text-[11px] leading-relaxed text-blue-950 space-y-2 pt-1.5 border-t border-blue-200/60">
                <p>
                  <strong>Krok 1 (Uprawnienia):</strong> Aby profil służbowy mógł edytować magazyn, poproś Super Administratora sklepu o dodanie Twojego adresu firmowego w zakładce <strong>„Admini”</strong>.
                </p>
                <p>
                  <strong>Krok 2 (Zmień konto w 1 kliknięcie):</strong> Kliknij przycisk <strong>„Zmień konto” 🔄</strong> w nagłówku. Google pokaże okno z wyborem wszystkich kont na urządzeniu (osobistych i służbowych).
                </p>
                <p>
                  <strong>Krok 3 (Na telefonie z profilem służbowym):</strong> Możesz otworzyć aplikację w <strong>Chrome ze znaczkiem walizeczki 💼</strong> lub w normalnym Chrome kliknąć <em>„Użyj innego konta”</em>.
                </p>
              </div>
            </details>
          </div>
        </div>
      </div>
    </div>
  );
}
