import { useState, useRef } from 'react';
import { Carpet } from '../types';
import { X, Upload, FileText, CheckCircle2, AlertCircle, RefreshCw, FileSpreadsheet } from 'lucide-react';
import { initialCarpets } from '../data/seedData';
import * as XLSX from 'xlsx';

interface ImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImport: (carpets: Carpet[], mode: 'merge' | 'replace') => Promise<void>;
}

export function ImportModal({ isOpen, onClose, onImport }: ImportModalProps) {
  const [fileData, setFileData] = useState<Carpet[] | null>(null);
  const [fileName, setFileName] = useState<string>('');
  const [mode, setMode] = useState<'merge' | 'replace'>('merge');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  if (!isOpen) return null;

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    setError(null);
    setSuccess(null);
    const file = e.target.files?.[0];
    if (!file) return;

    setFileName(file.name);
    const reader = new FileReader();

    if (file.name.endsWith('.json')) {
      reader.onload = (event) => {
        try {
          const content = event.target?.result as string;
          const parsed = JSON.parse(content);
          const list = Array.isArray(parsed) ? parsed : parsed.carpets || [];
          if (list.length === 0) {
            throw new Error('Plik JSON nie zawiera poprawnych rekordów dywanów');
          }
          setFileData(list);
          setSuccess(`Wczytano ${list.length} pozycji z pliku JSON.`);
        } catch (err: unknown) {
          setError('Nie udało się przetworzyć pliku JSON: ' + (err instanceof Error ? err.message : String(err)));
        }
      };
      reader.readAsText(file);
    } else if (file.name.endsWith('.xlsx') || file.name.endsWith('.xls') || file.name.endsWith('.csv')) {
      reader.onload = (event) => {
        try {
          const data = new Uint8Array(event.target?.result as ArrayBuffer);
          const workbook = XLSX.read(data, { type: 'array' });
          const firstSheetName = workbook.SheetNames[0];
          const sheet = workbook.Sheets[firstSheetName];
          const rows: any[] = XLSX.utils.sheet_to_json(sheet);

          if (!rows || rows.length === 0) {
            throw new Error('Arkusz jest pusty');
          }

          const parsedCarpets: Carpet[] = rows.map((r, idx) => {
            const mag = Number(r.magazyn || r.Magazyn || r.Regal || r['Regał'] || Math.floor(idx / 2) + 1);
            const podmag = String(r.podmagazyn || r.Podmagazyn || r.Slot || (idx % 2 === 0 ? 'A' : 'B')).trim().toUpperCase();
            const miejsce = r.miejsce || r.Miejsce || `${mag} ${podmag}`;
            const nazwa = r.nazwa || r.Nazwa || r.Produkt || r['Nazwa produktu'] || `DYWAN #${idx + 1}`;
            const ref = String(r.ref || r.Ref || r.REF || r['REF. LM'] || '');
            const ean = String(r.ean || r.EAN || r['Kod EAN'] || '');
            const dodano = String(r.dodano || r.Dodano || r['Data dodania'] || new Date().toISOString().split('T')[0]);

            return {
              id: `imported_${Date.now()}_${idx}`,
              magazyn: mag,
              podmagazyn: podmag,
              miejsce,
              nazwa,
              ref,
              ean,
              dodano
            };
          });

          setFileData(parsedCarpets);
          setSuccess(`Wczytano ${parsedCarpets.length} pozycji z arkusza kalkulacyjnego.`);
        } catch (err: unknown) {
          setError('Nie udało się odczytać arkusza: ' + (err instanceof Error ? err.message : String(err)));
        }
      };
      reader.readAsArrayBuffer(file);
    } else {
      setError('Obsługiwane formaty: .json, .xlsx, .xls, .csv');
    }
  };

  const handleLoadDemo = () => {
    setFileData(initialCarpets);
    setFileName('baza_wzorcowa_sklep_038.json');
    setSuccess(`Załadowano ${initialCarpets.length} pozycji referencyjnych ze zrzutów ekranu sklepu 038.`);
    setError(null);
  };

  const handleConfirmImport = async () => {
    if (!fileData || fileData.length === 0) return;
    try {
      setLoading(true);
      setError(null);
      await onImport(fileData, mode);
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Wystąpił błąd zapisu do chmury');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-3 sm:p-4 backdrop-blur-xs">
      <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-amber-50">
          <div className="flex items-center gap-2 font-bold text-amber-900 text-sm sm:text-base">
            <Upload className="w-5 h-5 text-amber-600" />
            Import bazy magazynowej sklepu 038
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-amber-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 sm:p-6 overflow-y-auto flex-1 flex flex-col gap-4">
          <p className="text-xs text-gray-600">
            Zaimportuj dane ekspozycji dywanów z pliku JSON (kopia zapasowa) lub arkusza XLSX/CSV.
          </p>

          {/* Upload Drop area */}
          <div
            onClick={() => fileInputRef.current?.click()}
            className="border-2 border-dashed border-gray-300 hover:border-amber-500 rounded-2xl p-6 text-center cursor-pointer bg-gray-50/50 hover:bg-amber-50/30 transition-all flex flex-col items-center justify-center"
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".json,.xlsx,.xls,.csv"
              onChange={handleFileUpload}
              className="hidden"
            />
            <FileSpreadsheet className="w-10 h-10 text-amber-600 mb-2" />
            <span className="font-bold text-gray-800 text-xs sm:text-sm">
              Wybierz plik (.json, .xlsx, .csv)
            </span>
            <span className="text-[11px] text-gray-400 mt-1">
              lub upuść plik tutaj
            </span>
          </div>

          {/* Quick Demo Button */}
          <div className="flex justify-center">
            <button
              type="button"
              onClick={handleLoadDemo}
              className="text-xs text-amber-800 font-bold underline hover:text-amber-900 flex items-center gap-1.5"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              Wczytaj domyślną bazę zrzutów sklepu 038 (11 pozycji)
            </button>
          </div>

          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
              <span>{success}</span>
            </div>
          )}

          {fileData && (
            <div className="bg-gray-50 border border-gray-200 rounded-xl p-3 flex flex-col gap-2.5">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-gray-700 truncate max-w-[200px]">
                  Plik: {fileName}
                </span>
                <span className="font-bold text-emerald-700">
                  {fileData.length} pozycji
                </span>
              </div>

              {/* Mode choice */}
              <div className="pt-2 border-t border-gray-200 flex flex-col gap-1.5">
                <span className="text-xs font-bold text-gray-700">Tryb importu:</span>
                <label className="flex items-center gap-2 text-xs text-gray-700 cursor-pointer">
                  <input
                    type="radio"
                    name="import-mode"
                    checked={mode === 'merge'}
                    onChange={() => setMode('merge')}
                    className="text-amber-600"
                  />
                  <span>
                    <strong className="text-gray-900">Scal z obecną bazą</strong> (dodaj brakujące i zaktualizuj istniejące)
                  </span>
                </label>
                <label className="flex items-center gap-2 text-xs text-gray-700 cursor-pointer">
                  <input
                    type="radio"
                    name="import-mode"
                    checked={mode === 'replace'}
                    onChange={() => setMode('replace')}
                    className="text-amber-600"
                  />
                  <span>
                    <strong className="text-red-700">Zastąp całą bazę</strong> (usuń aktualne pozycje i wgraj nowy plik)
                  </span>
                </label>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-5 py-3.5 border-t border-gray-200 bg-gray-50 flex items-center justify-between">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs sm:text-sm font-semibold text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded-lg transition-colors"
          >
            Anuluj
          </button>
          <button
            onClick={handleConfirmImport}
            disabled={!fileData || loading}
            className="px-5 py-2 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-white rounded-lg text-xs sm:text-sm font-bold shadow-xs transition-colors"
          >
            {loading ? 'Importowanie...' : `Zatwierdź import (${fileData?.length || 0})`}
          </button>
        </div>
      </div>
    </div>
  );
}
