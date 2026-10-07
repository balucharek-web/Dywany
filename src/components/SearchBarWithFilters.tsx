import React from 'react';
import { SlotFilter, RackRange } from '../types';
import { Search, X, QrCode } from 'lucide-react';

interface SearchBarWithFiltersProps {
  query: string;
  onQueryChange: (q: string) => void;
  currentFilter: SlotFilter;
  onFilterChange: (f: SlotFilter) => void;
  currentRange: RackRange;
  onRangeChange: (r: RackRange) => void;
  onOpenScanner: () => void;
}

export const SearchBarWithFilters: React.FC<SearchBarWithFiltersProps> = ({
  query,
  onQueryChange,
  currentFilter,
  onFilterChange,
  currentRange,
  onRangeChange,
  onOpenScanner
}) => {
  return (
    <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
      {/* Top Search bar + Skanuj button */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-5 h-5 absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            placeholder="Szukaj miejsca (np. 1a, 23b), kodu ESL, EAN lub nazwy dywanu..."
            className="w-full pl-11 pr-10 py-3 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-blue-600 focus:border-transparent text-sm transition"
          />
          {query && (
            <button
              onClick={() => onQueryChange('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        <button
          onClick={onOpenScanner}
          className="flex items-center justify-center space-x-2 px-5 py-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-sm transition cursor-pointer"
        >
          <QrCode className="w-4 h-4 text-blue-700" />
          <span>Wpisz / Skanuj kod</span>
        </button>
      </div>

      {/* Filter Chips & Ranges */}
      <div className="flex flex-wrap items-center gap-2 pt-1 border-t border-slate-100 text-xs">
        <span className="text-slate-400 font-medium mr-1">Status:</span>
        <button
          onClick={() => onFilterChange('ALL')}
          className={`px-3 py-1.5 rounded-lg font-semibold transition cursor-pointer ${
            currentFilter === 'ALL'
              ? 'bg-blue-700 text-white shadow-xs'
              : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
          }`}
        >
          Wszystkie
        </button>
        <button
          onClick={() => onFilterChange('OCCUPIED')}
          className={`px-3 py-1.5 rounded-lg font-medium transition cursor-pointer ${
            currentFilter === 'OCCUPIED'
              ? 'bg-blue-700 text-white shadow-xs font-semibold'
              : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
          }`}
        >
          Tylko zajęte
        </button>
        <button
          onClick={() => onFilterChange('EMPTY')}
          className={`px-3 py-1.5 rounded-lg font-medium transition cursor-pointer ${
            currentFilter === 'EMPTY'
              ? 'bg-blue-700 text-white shadow-xs font-semibold'
              : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
          }`}
        >
          Wolne miejsca
        </button>

        <span className="text-slate-300 font-medium mx-2 hidden sm:inline">|</span>
        <span className="text-slate-400 font-medium mr-1 hidden sm:inline">
          Zakres stojaków:
        </span>
        <button
          onClick={() => onRangeChange('ALL')}
          className={`px-3 py-1.5 rounded-lg font-medium transition cursor-pointer ${
            currentRange === 'ALL'
              ? 'bg-blue-700 text-white shadow-xs font-semibold'
              : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
          }`}
        >
          Wszystkie
        </button>
        <button
          onClick={() => onRangeChange('1-10')}
          className={`px-3 py-1.5 rounded-lg font-medium transition cursor-pointer ${
            currentRange === '1-10'
              ? 'bg-blue-700 text-white shadow-xs font-semibold'
              : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
          }`}
        >
          1–10
        </button>
        <button
          onClick={() => onRangeChange('11-20')}
          className={`px-3 py-1.5 rounded-lg font-medium transition cursor-pointer ${
            currentRange === '11-20'
              ? 'bg-blue-700 text-white shadow-xs font-semibold'
              : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
          }`}
        >
          11–20
        </button>
        <button
          onClick={() => onRangeChange('21-30')}
          className={`px-3 py-1.5 rounded-lg font-medium transition cursor-pointer ${
            currentRange === '21-30'
              ? 'bg-blue-700 text-white shadow-xs font-semibold'
              : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
          }`}
        >
          21–30
        </button>
      </div>
    </div>
  );
};
