import React from 'react';
import { StoreStats, UserProfile } from '../types';
import { Layers, CheckCircle2, PackageOpen, Barcode, Lock } from 'lucide-react';

interface StatsHeaderProps {
  stats: StoreStats;
  currentUser: UserProfile | null;
  onLogin: () => void;
}

export const StatsHeader: React.FC<StatsHeaderProps> = ({
  stats,
  currentUser,
  onLogin
}) => {
  return (
    <div className="space-y-4">
      {/* 4 Stat Boxes Strip */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {/* Total Racks */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center text-lg">
            <Layers className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
              Stojaki
            </span>
            <span className="text-xl font-extrabold text-slate-900">
              {stats.totalRacks}
            </span>
          </div>
        </div>

        {/* Occupied */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-700 flex items-center justify-center text-lg">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
              Zajęte
            </span>
            <span className="text-xl font-extrabold text-amber-700">
              {stats.occupiedCount}
            </span>
          </div>
        </div>

        {/* Empty Slots */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center text-lg">
            <PackageOpen className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
              Wolne miejsca
            </span>
            <span className="text-xl font-extrabold text-emerald-700">
              {stats.emptyCount}
            </span>
          </div>
        </div>

        {/* Total Slots */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-700 flex items-center justify-center text-lg">
            <Barcode className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
              Miejsca łącznie
            </span>
            <span className="text-xl font-extrabold text-indigo-700">
              {stats.totalSlots}
            </span>
          </div>
        </div>
      </div>

      {/* Read-only notice when not authenticated */}
      {!currentUser && (
        <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Lock className="w-4 h-4 text-amber-600 shrink-0" />
            <span>
              Tryb tylko do odczytu. Zaloguj się kontem Google, aby móc dodawać, edytować i przenosić dywany.
            </span>
          </div>
          <button
            onClick={onLogin}
            className="font-bold underline text-amber-900 hover:text-amber-700 ml-2 whitespace-nowrap cursor-pointer"
          >
            Zaloguj teraz
          </button>
        </div>
      )}
    </div>
  );
};
