import React from 'react';

export const ShowroomBanner: React.FC = () => {
  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs p-4 flex items-center space-x-4">
      <img
        src="/carpet_banner_1791353378089.jpg"
        alt="Ekspozycja dywanów"
        className="w-20 h-20 rounded-xl object-cover shrink-0 shadow-xs border border-slate-100"
        onError={(e) => {
          // Fallback image if asset path not found
          (e.target as HTMLImageElement).src =
            'https://images.unsplash.com/photo-1600121848594-d8644e57abab?auto=format&fit=crop&w=400&q=80';
        }}
      />
      <div>
        <h3 className="text-sm font-bold text-slate-900 leading-snug">
          Stojaki ekspozycyjne (2 dywany / stojak)
        </h3>
        <p className="text-xs text-slate-500 mt-1 leading-relaxed">
          Wyszukuj po miejscu (np. 1a, 23b), kodzie ESL lub EAN. Dane dywanów i kody kreskowe są integrowane z leroymerlin.pl.
        </p>
      </div>
    </div>
  );
};
