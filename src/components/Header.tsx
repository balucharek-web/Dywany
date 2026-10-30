import { User } from 'firebase/auth';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';
import { STORE_NUMBER, STORE_CITY, SUPER_ADMIN_EMAIL } from '../types';
import { LogOut, ShieldCheck, Database, Search, Users, RefreshCw, History } from 'lucide-react';

interface HeaderProps {
  currentView: 'search' | 'warehouse';
  setCurrentView: (view: 'search' | 'warehouse') => void;
  user: User | null;
  isAdmin: boolean;
  isSuperAdmin: boolean;
  onLoginClick: () => void;
  onLogoutClick: () => void;
  onSwitchAccount?: () => void;
  onOpenSuperAdminModal: () => void;
  onOpenActivityLogs: () => void;
  isSyncing: boolean;
}

export function Header({
  currentView,
  setCurrentView,
  user,
  isAdmin,
  isSuperAdmin,
  onLoginClick,
  onLogoutClick,
  onSwitchAccount,
  onOpenSuperAdminModal,
  onOpenActivityLogs,
  isSyncing
}: HeaderProps) {
  return (
    <header className="bg-white border-b border-gray-200 px-3 py-2.5 sm:px-6 shadow-xs sticky top-0 z-30">
      <div className="max-w-6xl mx-auto flex flex-wrap items-center justify-between gap-2">
        {/* Brand left */}
        <div className="flex items-center gap-3">
          <LeroyMerlinLogo className="h-10 sm:h-12" />
          <div className="leading-tight">
            <div className="text-base sm:text-lg font-black text-blue-600 tracking-tight flex items-center gap-1.5">
              Sklep {STORE_NUMBER}
              <span
                title={isSyncing ? "Synchronizowanie zmian..." : "Połączono w czasie rzeczywistym z bazą sklepu 038"}
                className={`inline-block w-2.5 h-2.5 rounded-full ${isSyncing ? "bg-amber-400 animate-ping" : "bg-emerald-500"}`}
              />
            </div>
            <div className="text-xs sm:text-sm font-bold text-gray-800 tracking-wider">
              {STORE_CITY}
            </div>
          </div>
        </div>

        {/* Action buttons right */}
        <div className="flex items-center gap-2 flex-wrap">
          {/* User info & account switch if logged in */}
          {user && (
            <div className="flex items-center gap-1.5 px-2.5 py-1 bg-gray-100/90 border border-gray-200 rounded-xl text-xs text-gray-700">
              {isSuperAdmin ? (
                <span className="flex items-center gap-1 font-semibold text-purple-700 bg-purple-100 px-1.5 py-0.5 rounded text-[11px]">
                  <ShieldCheck className="w-3.5 h-3.5" /> <span className="hidden sm:inline">Super Admin</span><span className="sm:hidden">Super</span>
                </span>
              ) : isAdmin ? (
                <span className="flex items-center gap-1 font-semibold text-blue-700 bg-blue-100 px-1.5 py-0.5 rounded text-[11px]">
                  <ShieldCheck className="w-3.5 h-3.5" /> <span className="hidden sm:inline">Admin 038</span><span className="sm:hidden">Admin</span>
                </span>
              ) : (
                <span className="text-amber-700 bg-amber-100 px-1.5 py-0.5 rounded font-medium text-[11px]" title="To konto nie ma uprawnień do edycji magazynu">
                  Brak uprawnień
                </span>
              )}

              <span className="truncate max-w-[100px] sm:max-w-[140px] text-[11px] font-medium" title={user.email || ''}>
                {user.email?.split('@')[0]}
              </span>

              {/* Quick switch account button */}
              {onSwitchAccount && (
                <button
                  onClick={onSwitchAccount}
                  className="p-1 text-gray-500 hover:text-blue-600 hover:bg-white rounded-md transition-colors"
                  title="Zmień konto Google (przełącz na profil służbowy / osobisty)"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                </button>
              )}

              {/* Logout button */}
              <button
                onClick={onLogoutClick}
                className="p-1 text-gray-500 hover:text-red-600 hover:bg-white rounded-md transition-colors"
                title="Wyloguj"
              >
                <LogOut className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* Super admin button */}
          {isSuperAdmin && currentView === 'warehouse' && (
            <button
              onClick={onOpenSuperAdminModal}
              className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg bg-purple-600 hover:bg-purple-700 text-white transition-colors shadow-xs"
              title="Zarządzaj uprawnieniami administratorów"
            >
              <Users className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Admini</span>
            </button>
          )}

          {/* Historia zmian (Audit trail) button */}
          <button
            onClick={onOpenActivityLogs}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-800 transition-colors shadow-2xs cursor-pointer"
            title="Historia zmian: sprawdź kto, co i kiedy zmienił w bazie"
          >
            <History className="w-3.5 h-3.5 text-purple-600" />
            <span className="hidden sm:inline">Historia zmian</span>
          </button>

          {/* Toggle between Search & Warehouse */}
          {currentView === 'search' ? (
            <button
              onClick={() => {
                if (!user || !isAdmin) {
                  onLoginClick();
                } else {
                  setCurrentView('warehouse');
                }
              }}
              className="flex items-center gap-1.5 px-3.5 py-1.5 text-xs sm:text-sm font-semibold rounded-lg bg-[#f04438] hover:bg-[#e03125] text-white transition-all shadow-xs active:scale-95"
            >
              <Database className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              Zarządzaj magazynem
            </button>
          ) : (
            <button
              onClick={() => setCurrentView('search')}
              className="flex items-center gap-1.5 px-3.5 py-1.5 text-xs sm:text-sm font-semibold rounded-lg bg-blue-600 hover:bg-blue-700 text-white transition-all shadow-xs active:scale-95"
            >
              <Search className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              Wyszukiwarka
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
