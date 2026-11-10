import { User } from 'firebase/auth';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';
import { STORE_NUMBER, STORE_CITY } from '../types';
import { LogOut, ShieldCheck, Database, Search, Users, RefreshCw, Sun, Moon } from 'lucide-react';
import { PWAInstallButton } from './PWAInstallButton';

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
  isSyncing: boolean;
  theme?: 'light' | 'dark';
  toggleTheme?: () => void;
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
  isSyncing,
  theme = 'light',
  toggleTheme
}: HeaderProps) {
  return (
    <header className="bg-white border-b border-gray-200 px-3 py-2 sm:px-6 sm:py-2.5 shadow-xs sticky top-0 z-30 transition-colors">
      <div className="max-w-6xl xl:max-w-7xl mx-auto flex items-center justify-between gap-2 flex-nowrap">
        {/* Brand left */}
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <LeroyMerlinLogo className="h-8 sm:h-11 shrink-0" />
          <div className="leading-tight">
            <div className="text-sm sm:text-base font-black text-blue-600 tracking-tight flex items-center gap-1.5">
              <span>Sklep {STORE_NUMBER}</span>
              <span
                title={isSyncing ? "Synchronizowanie zmian..." : `Połączono w czasie rzeczywistym z bazą sklepu ${STORE_NUMBER}`}
                className={`inline-block w-2 h-2 sm:w-2.5 sm:h-2.5 rounded-full shrink-0 ${isSyncing ? "bg-amber-400 animate-ping" : "bg-emerald-500"}`}
              />
            </div>
            <div className="text-[10px] sm:text-xs font-bold text-gray-500 uppercase tracking-wider">
              {STORE_CITY}
            </div>
          </div>
        </div>

        {/* Action buttons right */}
        <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
          {/* OLED Dark Mode Sun / Moon Switcher */}
          {toggleTheme && (
            <button
              key="btn-theme-toggle"
              onClick={toggleTheme}
              className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl text-gray-600 hover:text-gray-900 bg-gray-100/90 hover:bg-gray-200/90 border border-gray-200 transition-colors cursor-pointer flex items-center justify-center shadow-2xs active:scale-95"
              title={theme === 'dark' ? 'Włącz tryb jasny' : 'Włącz tryb Pure OLED Black'}
            >
              {theme === 'dark' ? (
                <Sun className="w-4 h-4 text-amber-400" />
              ) : (
                <Moon className="w-4 h-4 text-gray-700" />
              )}
            </button>
          )}

          {/* User info & account switch if logged in */}
          {user && (
            <div className="flex items-center">
              {/* Mobile compact user button: opens LoginModal */}
              <button
                onClick={onLoginClick}
                className="sm:hidden flex items-center gap-1 px-2.5 h-9 rounded-xl text-xs font-bold bg-blue-50 border border-blue-200 text-blue-700 cursor-pointer active:scale-95 transition-all shadow-2xs"
                title={`Zalogowano jako ${user.email}. Kliknij, aby zarządzać kontem.`}
              >
                <ShieldCheck className="w-3.5 h-3.5 shrink-0" />
                <span className="truncate max-w-[65px] font-semibold text-[11px]">
                  {user.email?.split('@')[0]}
                </span>
              </button>

              {/* Tablet/Desktop full user badge */}
              <div
                className="hidden sm:flex items-center gap-1.5 px-2.5 py-1.5 bg-gray-100/90 border border-gray-200 rounded-xl text-xs text-gray-700 transition-colors"
              >
                <button
                  onClick={onLoginClick}
                  className="flex items-center gap-1.5 cursor-pointer hover:opacity-80 transition-opacity"
                  title="Zarządzaj kontem i profilem służbowym"
                >
                  {isSuperAdmin ? (
                    <span className="flex items-center gap-1 font-semibold text-purple-700 bg-purple-100 px-1.5 py-0.5 rounded text-[11px]">
                      <ShieldCheck className="w-3.5 h-3.5" />
                      <span>Super Admin</span>
                    </span>
                  ) : isAdmin ? (
                    <span className="flex items-center gap-1 font-semibold text-blue-700 bg-blue-100 px-1.5 py-0.5 rounded text-[11px]">
                      <ShieldCheck className="w-3.5 h-3.5" />
                      <span>Admin {STORE_NUMBER}</span>
                    </span>
                  ) : (
                    <span className="text-amber-700 bg-amber-100 px-1.5 py-0.5 rounded font-medium text-[11px]">
                      Brak uprawnień
                    </span>
                  )}
                  <span className="truncate max-w-[120px] font-medium text-[11px]" title={user.email || ''}>
                    {user.email?.split('@')[0]}
                  </span>
                </button>

                {onSwitchAccount && (
                  <button
                    onClick={onSwitchAccount}
                    className="p-1 text-gray-400 hover:text-blue-600 hover:bg-white rounded-md transition-colors cursor-pointer"
                    title="Zmień konto Google (przełącz na profil służbowy)"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                  </button>
                )}

                <button
                  onClick={onLogoutClick}
                  className="p-1 text-gray-400 hover:text-red-600 hover:bg-white rounded-md transition-colors cursor-pointer"
                  title="Wyloguj"
                >
                  <LogOut className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          )}

          {/* Super admin button (visible on tablet/desktop) */}
          {isSuperAdmin && currentView === 'warehouse' && (
            <button
              onClick={onOpenSuperAdminModal}
              className="hidden md:flex items-center gap-1.5 px-2.5 h-10 text-xs font-semibold rounded-xl bg-purple-600 hover:bg-purple-700 text-white transition-colors shadow-xs cursor-pointer active:scale-95"
              title="Zarządzaj uprawnieniami administratorów"
            >
              <Users className="w-3.5 h-3.5" />
              <span>Admini</span>
            </button>
          )}

          {/* Toggle between Search & Warehouse */}
          {currentView === 'search' ? (
            <button
              key="btn-view-toggle-warehouse"
              onClick={() => {
                if (!user || !isAdmin) {
                  onLoginClick();
                } else {
                  setCurrentView('warehouse');
                }
              }}
              className="flex items-center gap-1.5 h-9 sm:h-10 px-3 sm:px-4 text-xs sm:text-sm font-bold rounded-xl bg-[#f04438] hover:bg-[#e03125] text-white transition-all shadow-xs active:scale-95 cursor-pointer shrink-0"
            >
              <Database className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              <span>Magazyn</span>
            </button>
          ) : (
            <button
              key="btn-view-toggle-search"
              onClick={() => setCurrentView('search')}
              className="flex items-center gap-1.5 h-9 sm:h-10 px-3 sm:px-4 text-xs sm:text-sm font-bold rounded-xl bg-blue-600 hover:bg-blue-700 text-white transition-all shadow-xs active:scale-95 cursor-pointer shrink-0"
            >
              <Search className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              <span>Szukaj</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
