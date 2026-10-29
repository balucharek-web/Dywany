import { useEffect, useRef } from 'react';
import { Carpet, STORE_NUMBER, STORE_CITY } from '../types';
import { X, Printer, Tag } from 'lucide-react';
import JsBarcode from 'jsbarcode';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';

interface FiszkaModalProps {
  carpet: Carpet | null;
  onClose: () => void;
}

export function FiszkaModal({ carpet, onClose }: FiszkaModalProps) {
  const barcodeRef = useRef<SVGSVGElement | null>(null);

  useEffect(() => {
    if (carpet && barcodeRef.current && carpet.ean) {
      try {
        const cleanEan = carpet.ean.replace(/\D/g, '');
        if (cleanEan.length === 13 || cleanEan.length === 12 || cleanEan.length === 8) {
          JsBarcode(barcodeRef.current, cleanEan, {
            format: cleanEan.length === 13 ? 'EAN13' : 'CODE128',
            width: 2,
            height: 55,
            displayValue: true,
            fontSize: 14,
            margin: 0
          });
        } else {
          JsBarcode(barcodeRef.current, carpet.ean, {
            format: 'CODE128',
            width: 1.8,
            height: 50,
            displayValue: true,
            fontSize: 13,
            margin: 0
          });
        }
      } catch (err) {
        console.warn('Barcode generation warning:', err);
      }
    }
  }, [carpet]);

  if (!carpet) return null;

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
      <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Top Bar (hidden on print) */}
        <div className="flex items-center justify-between px-5 py-3 border-b border-gray-200 bg-gray-50 print:hidden">
          <div className="flex items-center gap-2 font-bold text-gray-800 text-sm sm:text-base">
            <Tag className="w-4 h-4 text-emerald-600" />
            Fiszka ekspozycyjna dywanu
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-200 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Fiszka printable preview */}
        <div className="p-6 overflow-y-auto flex-1 flex flex-col items-center">
          <div
            id="fiszka-printable"
            className="w-full max-w-sm border-2 border-gray-900 rounded-xl p-5 bg-white text-gray-900 shadow-sm print:shadow-none print:border-2 print:border-black print:max-w-none print:w-full"
          >
            {/* Header */}
            <div className="flex items-center justify-between border-b-2 border-gray-900 pb-3 mb-4">
              <div className="flex items-center gap-2">
                <LeroyMerlinLogo className="h-8" />
                <div className="leading-tight">
                  <div className="text-xs font-black uppercase tracking-wider text-green-700">
                    LEROY MERLIN
                  </div>
                  <div className="text-[11px] font-bold text-gray-600">
                    Sklep {STORE_NUMBER} {STORE_CITY}
                  </div>
                </div>
              </div>
              <div className="text-right">
                <span className="text-[10px] uppercase font-bold text-gray-500 block">Sektor</span>
                <span className="text-xs font-black text-gray-800">Dywany / Wykładziny</span>
              </div>
            </div>

            {/* Miejsce highlight box */}
            <div className="bg-amber-100 border-2 border-amber-500 rounded-lg p-2.5 mb-4 text-center">
              <span className="text-[11px] font-bold uppercase tracking-wider text-amber-800 block">
                MIEJSCE EKSPOZYCJI
              </span>
              <span className="text-3xl font-black text-gray-900 tracking-wider">
                {carpet.miejsce || `${carpet.magazyn} ${carpet.podmagazyn}`}
              </span>
              <span className="text-[10px] text-amber-900 font-medium block mt-0.5">
                Regał {carpet.magazyn} • Podmagazyn {carpet.podmagazyn}
              </span>
            </div>

            {/* Product Name */}
            <div className="mb-3">
              <span className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">
                NAZWA PRODUKTU
              </span>
              <div className="text-base sm:text-lg font-black text-gray-900 leading-snug break-words">
                {carpet.nazwa || 'DYWAN (brak nazwy)'}
              </div>
            </div>

            {/* Reference & Date */}
            <div className="grid grid-cols-2 gap-2 py-2 border-t border-b border-gray-200 mb-4 text-xs">
              <div>
                <span className="text-[10px] text-gray-500 font-bold uppercase block">REF. LM</span>
                <span className="font-extrabold text-sm text-gray-900">{carpet.ref || '—'}</span>
              </div>
              <div>
                <span className="text-[10px] text-gray-500 font-bold uppercase block">Data dodania</span>
                <span className="font-semibold text-gray-700">{carpet.dodano || '—'}</span>
              </div>
            </div>

            {/* Barcode section */}
            <div className="flex flex-col items-center justify-center pt-1">
              <span className="text-[10px] text-gray-500 font-bold uppercase mb-1">
                KOD KRESKOWY EAN
              </span>
              <div className="max-w-full overflow-hidden flex justify-center">
                <svg ref={barcodeRef} className="max-w-full h-16"></svg>
              </div>
              <div className="text-xs font-mono font-bold tracking-widest mt-1 text-gray-800">
                {carpet.ean || 'Brak EAN'}
              </div>
            </div>
          </div>
        </div>

        {/* Modal Bottom Actions (hidden on print) */}
        <div className="px-5 py-3.5 border-t border-gray-200 bg-gray-50 flex items-center justify-between gap-3 print:hidden">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs sm:text-sm font-semibold text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded-lg transition-colors"
          >
            Zamknij
          </button>
          <button
            onClick={handlePrint}
            className="flex items-center gap-2 px-5 py-2 text-xs sm:text-sm font-bold bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-all shadow-md active:scale-95"
          >
            <Printer className="w-4 h-4" />
            Drukuj fiszkę
          </button>
        </div>
      </div>
    </div>
  );
}
