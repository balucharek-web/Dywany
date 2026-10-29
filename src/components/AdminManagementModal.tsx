import { useState } from 'react';
import { AdminUser, SUPER_ADMIN_EMAIL } from '../types';
import { X, UserPlus, Trash2, ShieldCheck, Mail, Calendar, AlertCircle, Check } from 'lucide-react';

interface AdminManagementModalProps {
  isOpen: boolean;
  onClose: () => void;
  admins: AdminUser[];
  onAddAdmin: (email: string) => Promise<void>;
  onRemoveAdmin: (email: string) => Promise<void>;
  currentSuperAdminEmail: string;
}

export function AdminManagementModal({
  isOpen,
  onClose,
  admins,
  onAddAdmin,
  onRemoveAdmin,
  currentSuperAdminEmail
}: AdminManagementModalProps) {
  const [newEmail, setNewEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    const email = newEmail.trim().toLowerCase();
    setError(null);
    setSuccess(null);

    if (!email || !email.includes('@')) {
      setError('Wpisz poprawny adres e-mail pracownika.');
      return;
    }

    if (email === currentSuperAdminEmail.toLowerCase()) {
      setError('Ten adres jest już głównym Super Administratorem.');
      return;
    }

    if (admins.some((a) => a.email.toLowerCase() === email)) {
      setError('Użytkownik z tym adresem jest już administratorem.');
      return;
    }

    try {
      setLoading(true);
      await onAddAdmin(email);
      setNewEmail('');
      setSuccess(`Pomyślnie dodano uprawnienia dla ${email}`);
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Wystąpił błąd podczas dodawania');
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (email: string) => {
    if (window.confirm(`Czy na pewno chcesz odebrać uprawnienia administratora dla ${email}?`)) {
      try {
        setLoading(true);
        await onRemoveAdmin(email);
        setSuccess(`Odebrano uprawnienia dla ${email}`);
        setTimeout(() => setSuccess(null), 3000);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : 'Wystąpił błąd podczas usuwania');
      } finally {
        setLoading(false);
      }
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
      <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-purple-50">
          <div className="flex items-center gap-2 font-bold text-purple-900 text-sm sm:text-base">
            <ShieldCheck className="w-5 h-5 text-purple-600" />
            Zarządzanie administratorami sklepu 038
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-purple-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-5">
          {/* Super Admin Notice */}
          <div className="bg-purple-50/80 border border-purple-200 rounded-xl p-3.5 flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-purple-700 shrink-0 mt-0.5" />
            <div className="text-xs">
              <span className="font-bold text-purple-900 block">Konto Super Admina:</span>
              <span className="font-mono text-purple-800 font-semibold">{currentSuperAdminEmail}</span>
              <p className="text-purple-700 mt-1">
                Jako Super Admin możesz autoryzować adresy e-mail innych pracowników sklepu 038, którzy będą mogli logować się kontem Google i edytować dane magazynowe.
              </p>
            </div>
          </div>

          {/* Add form */}
          <div>
            <label className="text-xs font-bold text-gray-700 block mb-1.5">
              Dodaj nowego administratora (adres e-mail Google)
            </label>
            <form onSubmit={handleAdd} className="flex gap-2">
              <div className="relative flex-1">
                <Mail className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  placeholder="np. jan.kowalski@leroymerlin.pl lub gmail.com"
                  className="w-full pl-9 pr-3 py-2 text-xs sm:text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:outline-hidden"
                  disabled={loading}
                />
              </div>
              <button
                type="submit"
                disabled={loading || !newEmail.trim()}
                className="px-4 py-2 bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white rounded-lg text-xs sm:text-sm font-semibold flex items-center gap-1.5 transition-colors shadow-xs"
              >
                <UserPlus className="w-4 h-4" />
                Dodaj
              </button>
            </form>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-2.5 flex items-center gap-2 text-xs text-red-700">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-2.5 flex items-center gap-2 text-xs text-emerald-700">
              <Check className="w-4 h-4 shrink-0" />
              <span>{success}</span>
            </div>
          )}

          {/* Active Admins List */}
          <div>
            <span className="text-xs font-bold text-gray-700 block mb-2">
              Lista aktywnych administratorów ({admins.length + 1})
            </span>

            <div className="flex flex-col gap-2">
              {/* Super Admin Row */}
              <div className="flex items-center justify-between p-3 rounded-xl border-2 border-purple-300 bg-purple-50/50">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-full bg-purple-600 text-white flex items-center justify-center font-bold text-xs">
                    SA
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-xs sm:text-sm font-bold text-gray-900 font-mono">
                        {currentSuperAdminEmail}
                      </span>
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-purple-200 text-purple-900">
                        SUPER ADMIN
                      </span>
                    </div>
                    <span className="text-[11px] text-gray-500">Główny właściciel sklepu 038</span>
                  </div>
                </div>
              </div>

              {/* Added Admins */}
              {admins.map((adm) => (
                <div
                  key={adm.email}
                  className="flex items-center justify-between p-3 rounded-xl border border-gray-200 bg-white hover:border-gray-300 transition-colors"
                >
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center font-bold text-xs">
                      AD
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs sm:text-sm font-bold text-gray-800 font-mono">
                          {adm.email}
                        </span>
                        <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-blue-800">
                          ADMIN
                        </span>
                      </div>
                      <div className="text-[11px] text-gray-400 flex items-center gap-2">
                        {adm.createdAt && <span>Dodano: {adm.createdAt}</span>}
                        {adm.addedBy && <span>przez: {adm.addedBy}</span>}
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={() => handleRemove(adm.email)}
                    disabled={loading}
                    className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    title="Odbierz uprawnienia"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}

              {admins.length === 0 && (
                <div className="p-4 text-center border border-dashed border-gray-200 rounded-xl text-gray-400 text-xs">
                  Brak dodatkowych administratorów. Wpisz e-mail powyżej, aby nadać uprawnienia kolejnym pracownikom.
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t border-gray-200 bg-gray-50 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-800 hover:bg-gray-900 text-white rounded-lg text-xs sm:text-sm font-semibold transition-colors"
          >
            Gotowe
          </button>
        </div>
      </div>
    </div>
  );
}
