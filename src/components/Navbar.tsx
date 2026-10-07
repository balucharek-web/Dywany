import React from 'react';
import { UserProfile } from '../types';
import { Layers, QrCode, LogIn, LogOut, Radio } from 'lucide-react';

interface NavbarProps {
  currentUser: UserProfile | null;
  isLoggingIn: boolean;
  onLogin: () => void;
  onLogout: () => void;
  onOpenScanner: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentUser,
  isLoggingIn,
  onLogin,
  onLogout,
  onOpenScanner
}) => {
  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-blue-700 text-white flex items-center justify-center font-extrabold text-xl shadow-md">
            <Layers className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="text-lg font-extrabold text-slate-900 tracking-tight leading-tight">
              DywanMag
            </h1>
            <p className="text-xs text-slate-500 font-medium">
              Stojaki ekspozycyjne — Magazyny 1a..23b
            </p>
          </div>
        </div>

        {/* Right side controls: Sync badge, Scanner, Auth */}
        <div className="flex items-center space-x-3">
          {/* Sync badge */}
          <div className="hidden sm:flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-semibold border border-emerald-200">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <Radio className="w-3.5 h-3.5 text-emerald-600" />
            <span>Synchronizacja Real-time</span>
          </div>

          {/* Quick Scanner button in app bar */}
          <button
            onClick={onOpenScanner}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs sm:text-sm transition cursor-pointer"
            title="Skaner kodów ESL / EAN"
          >
            <QrCode className="w-4 h-4 text-blue-700" />
            <span className="hidden md:inline">Skaner</span>
          </button>

          {/* User Profile / Auth */}
          {currentUser ? (
            <div className="flex items-center space-x-2">
              <div className="text-right hidden sm:block">
                <span className="block text-xs sm:text-sm font-bold text-slate-800">
                  {currentUser.displayName || currentUser.email?.split('@')[0] || 'Pracownik'}
                </span>
                <span className="block text-[10px] text-emerald-600 font-semibold">
                  Edycja aktywna
                </span>
              </div>
              <button
                onClick={onLogout}
                title="Wyloguj"
                className="w-9 h-9 rounded-xl bg-slate-100 hover:bg-rose-50 text-slate-600 hover:text-rose-600 flex items-center justify-center transition cursor-pointer border border-slate-200"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={onLogin}
              disabled={isLoggingIn}
              className="flex items-center space-x-2 px-3.5 py-2 rounded-xl bg-blue-700 hover:bg-blue-800 text-white text-xs sm:text-sm font-semibold transition shadow-xs cursor-pointer disabled:opacity-50"
            >
              <LogIn className="w-4 h-4" />
              <span>{isLoggingIn ? 'Logowanie...' : 'Zaloguj przez Google'}</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
