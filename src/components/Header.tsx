import { User } from 'firebase/auth';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';
import { STORE_NUMBER, STORE_CITY, SUPER_ADMIN_EMAIL } from '../types';
import { LogOut, ShieldCheck, Database, Search, Users, RefreshCw } from 'lucide-react';
import { PWAInstallButton } from './PWAInstallButton';

interface HeaderProps {
  currentView: 'search' | 'warehouse';
  setCurrentView: (view: 'search' | 'warehouse') => void;
  user: User | null;
  isAdmin: boolean;
  isSuperAdmin: boolean;
  onLoginClick: () => void;
  onLogoutClick: () => void;
  onOpenSuperAdminModal: () => void;
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
  onOpenSuperAdminModal,
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
          {/* User info if logged in */}
          {user && (
            <div className="hidden md:flex items-center gap-1.5 px-2.5 py-1 bg-gray-100 rounded-lg text-xs text-gray-700">
              {isSuperAdmin ? (
                <span className="flex items-center gap-1 font-semibold text-purple-700 bg-purple-100 px-1.5 py-0.5 rounded">
                  <ShieldCheck className="w-3.5 h-3.5" /> Super Admin
                </span>
              ) : isAdmin ? (
                <span className="flex items-center gap-1 font-semibold text-blue-700 bg-blue-100 px-1.5 py-0.5 rounded">
                  <ShieldCheck className="w-3.5 h-3.5" /> Admin 038
                </span>
              ) : (
                <span className="text-gray-500 font-medium">Brak uprawnień</span>
              )}
              <span className="truncate max-w-[140px]" title={user.email || ''}>
                {user.email}
              </span>
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

          {/* PWA Install Button */}
          <PWAInstallButton />

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

          {/* Logout if in warehouse */}
          {user && currentView === 'warehouse' && (
            <button
              onClick={onLogoutClick}
              className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
              title="Wyloguj"
            >
              <LogOut className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
