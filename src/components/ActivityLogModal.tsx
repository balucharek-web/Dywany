import { useState, useMemo } from 'react';
import { ActivityLog, ActivityAction, SUPER_ADMIN_EMAIL } from '../types';
import { formatDateTime, formatRelativeTime } from '../utils/formatters';
import {
  History,
  X,
  Search,
  Download,
  PlusCircle,
  Pencil,
  Trash2,
  User,
  Clock,
  ArrowUpDown
} from 'lucide-react';
import * as XLSX from 'xlsx';

interface ActivityLogModalProps {
  isOpen: boolean;
  onClose: () => void;
  logs: ActivityLog[];
}

export function ActivityLogModal({ isOpen, onClose, logs }: ActivityLogModalProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedAction, setSelectedAction] = useState<'all' | 'add' | 'edit' | 'delete'>('all');
  const [selectedUser, setSelectedUser] = useState<string>('all');
  const [sortOrder, setSortOrder] = useState<'desc' | 'asc'>('desc');

  // Only consider carpet management operations: who added, who edited, who removed
  const carpetLogs = useMemo(() => {
    return logs.filter((log) => ['add', 'edit', 'delete'].includes(log.action));
  }, [logs]);

  // Unique users who performed these actions
  const uniqueUsers = useMemo(() => {
    const set = new Set<string>();
    carpetLogs.forEach((log) => {
      if (log.userEmail) set.add(log.userEmail);
    });
    return Array.from(set).sort();
  }, [carpetLogs]);

  // Filtered and sorted logs
  const filteredLogs = useMemo(() => {
    return carpetLogs
      .filter((log) => {
        // Action filter: all, add, edit, delete
        if (selectedAction !== 'all' && log.action !== selectedAction) {
          return false;
        }

        // User filter
        if (selectedUser !== 'all' && log.userEmail !== selectedUser) {
          return false;
        }

        // Search query
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase();
          const matchesTarget = log.target?.toLowerCase().includes(q);
          const matchesDetails = log.details?.toLowerCase().includes(q);
          const matchesEmail = log.userEmail?.toLowerCase().includes(q);
          const matchesMiejsce = log.miejsce?.toLowerCase().includes(q);
          const matchesRef = log.ref?.toLowerCase().includes(q);
          const matchesEan = log.ean?.toLowerCase().includes(q);
          return matchesTarget || matchesDetails || matchesEmail || matchesMiejsce || matchesRef || matchesEan;
        }

        return true;
      })
      .sort((a, b) => {
        const timeA = new Date(a.timestamp).getTime();
        const timeB = new Date(b.timestamp).getTime();
        return sortOrder === 'desc' ? timeB - timeA : timeA - timeB;
      });
  }, [carpetLogs, selectedAction, selectedUser, searchQuery, sortOrder]);

  // Export audit log to Excel
  const handleExportXLSX = () => {
    const exportRows = filteredLogs.map((log) => ({
      'Data i godzina': formatDateTime(log.timestamp),
      'Użytkownik (Kto dokonał zmiany)': log.userEmail,
      'Typ operacji': getActionLabel(log.action),
      'Dywan / Pozycja': log.target,
      'Miejsce w magazynie': log.miejsce || '',
      'REF LM': log.ref || '',
      'Kod EAN': log.ean || '',
      'Szczegóły operacji': log.details
    }));

    const worksheet = XLSX.utils.json_to_sheet(exportRows);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Rejestr zmian dywanów');

    worksheet['!cols'] = [
      { wch: 20 },
      { wch: 30 },
      { wch: 20 },
      { wch: 35 },
      { wch: 16 },
      { wch: 14 },
      { wch: 18 },
      { wch: 45 }
    ];

    const dateStr = new Date().toISOString().split('T')[0];
    XLSX.writeFile(workbook, `rejestr_zmian_dywany_sklep038_${dateStr}.xlsx`);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-60 bg-black/70 flex items-center justify-center p-2 sm:p-4 backdrop-blur-xs animate-in fade-in">
      <div className="bg-white rounded-2xl max-w-4xl w-full max-h-[92vh] flex flex-col shadow-2xl overflow-hidden border border-gray-200">
        {/* Header */}
        <div className="flex items-center justify-between px-4 sm:px-6 py-4 border-b border-gray-200 bg-linear-to-r from-gray-50 to-white">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center shadow-xs">
              <History className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base sm:text-lg font-black text-gray-900 tracking-tight flex items-center gap-2">
                Rejestr zmian w magazynie
                <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                  Na żywo
                </span>
              </h2>
              <p className="text-xs text-gray-500">
                Sprawdź kto zmienił dywan, kto zdjął dywan lub kto dodał dywan w sklepie 038
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleExportXLSX}
              disabled={filteredLogs.length === 0}
              className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold transition-colors cursor-pointer disabled:opacity-50"
              title="Eksportuj rejestr audytu do pliku Excel"
            >
              <Download className="w-3.5 h-3.5" />
              Eksportuj (.xlsx)
            </button>
            <button
              onClick={onClose}
              className="p-2 text-gray-400 hover:text-gray-700 hover:bg-gray-100 rounded-xl transition-colors cursor-pointer"
              title="Zamknij"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Filters & Search Toolbar */}
        <div className="p-3 sm:p-4 bg-gray-50 border-b border-gray-200 flex flex-col gap-2.5">
          <div className="flex flex-col sm:flex-row gap-2">
            {/* Search Input */}
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Szukaj po emailu, nazwie dywanu, REF, EAN lub miejscu..."
                className="w-full pl-9 pr-3 py-2 bg-white border border-gray-200 rounded-xl text-xs sm:text-sm font-medium focus:outline-hidden focus:ring-2 focus:ring-blue-600 focus:border-transparent"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-xs font-bold"
                >
                  ✕
                </button>
              )}
            </div>

            {/* Filter by User */}
            <div className="flex items-center gap-1.5 min-w-[200px]">
              <User className="w-4 h-4 text-gray-500 shrink-0" />
              <select
                value={selectedUser}
                onChange={(e) => setSelectedUser(e.target.value)}
                className="w-full py-2 px-2.5 bg-white border border-gray-200 rounded-xl text-xs font-semibold text-gray-700 focus:outline-hidden focus:ring-2 focus:ring-blue-600"
              >
                <option value="all">Wszyscy użytkownicy ({uniqueUsers.length})</option>
                {uniqueUsers.map((email) => (
                  <option key={email} value={email}>
                    {email}
                  </option>
                ))}
              </select>
            </div>

            {/* Sort order toggle */}
            <button
              onClick={() => setSortOrder(sortOrder === 'desc' ? 'asc' : 'desc')}
              className="flex items-center justify-center gap-1.5 px-3 py-2 bg-white border border-gray-200 hover:bg-gray-100 rounded-xl text-xs font-semibold text-gray-700 transition-colors cursor-pointer shrink-0"
              title="Zmień kolejność chronologiczną"
            >
              <ArrowUpDown className="w-3.5 h-3.5 text-gray-500" />
              {sortOrder === 'desc' ? 'Najnowsze najpierw' : 'Najstarsze najpierw'}
            </button>
          </div>

          {/* Action chips: exactly "kto zmienił dywan, kto zdjął dywan albo kto dodał dywan" */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
            <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mr-1">
              Opcje:
            </span>
            {[
              { id: 'all', label: 'Wszystkie akcje' },
              { id: 'edit', label: 'Kto zmienił dywan' },
              { id: 'delete', label: 'Kto zdjął dywan' },
              { id: 'add', label: 'Kto dodał dywan' }
            ].map((chip) => (
              <button
                key={chip.id}
                onClick={() => setSelectedAction(chip.id as any)}
                className={`px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer whitespace-nowrap text-xs flex items-center gap-1.5 ${
                  selectedAction === chip.id
                    ? 'bg-purple-600 text-white shadow-2xs'
                    : 'bg-white text-gray-700 hover:bg-gray-200/70 border border-gray-200'
                }`}
              >
                {chip.id === 'add' && <PlusCircle className="w-3.5 h-3.5" />}
                {chip.id === 'edit' && <Pencil className="w-3.5 h-3.5" />}
                {chip.id === 'delete' && <Trash2 className="w-3.5 h-3.5" />}
                <span>{chip.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Logs List */}
        <div className="flex-1 overflow-y-auto divide-y divide-gray-100 p-2 sm:p-4">
          {filteredLogs.length > 0 ? (
            filteredLogs.map((log) => {
              const isSuper = log.userEmail === SUPER_ADMIN_EMAIL;
              return (
                <div
                  key={log.id}
                  className="p-3 sm:p-3.5 hover:bg-purple-50/40 rounded-xl transition-colors flex flex-col sm:flex-row sm:items-start justify-between gap-3 text-xs"
                >
                  <div className="flex items-start gap-3">
                    {/* Action Icon */}
                    <div className="mt-0.5 shrink-0">{renderActionIcon(log.action)}</div>

                    {/* Details body */}
                    <div className="flex flex-col gap-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        {renderActionBadge(log.action)}

                        {log.miejsce && (
                          <span className="font-extrabold text-[11px] px-2 py-0.5 rounded bg-amber-50 text-amber-800 border border-amber-200">
                            Miejsce: {log.miejsce}
                          </span>
                        )}

                        <span className="font-bold text-gray-900 text-xs sm:text-sm">
                          {log.target}
                        </span>
                      </div>

                      <div className="text-gray-600 leading-relaxed font-medium">
                        {log.details}
                      </div>

                      {(log.ref || log.ean) && (
                        <div className="flex items-center gap-3 text-[11px] font-mono text-gray-500 mt-0.5">
                          {log.ref && <span>REF: <strong className="text-gray-800">{log.ref}</strong></span>}
                          {log.ean && <span>EAN: <strong className="text-gray-800">{log.ean}</strong></span>}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Right side: Who and When */}
                  <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-start gap-1 shrink-0 border-t sm:border-t-0 pt-2 sm:pt-0 border-gray-100">
                    <div className="flex items-center gap-1.5 font-bold text-gray-800">
                      <div className="w-5 h-5 rounded-full bg-gray-200 text-gray-700 flex items-center justify-center text-[10px] uppercase font-black">
                        {log.userEmail ? log.userEmail[0] : 'U'}
                      </div>
                      <span className="truncate max-w-[170px]" title={log.userEmail}>
                        {log.userEmail}
                      </span>
                      {isSuper && (
                        <span className="text-[10px] text-purple-700 bg-purple-100 px-1 py-0.2 rounded font-black">
                          Super
                        </span>
                      )}
                    </div>

                    <div
                      className="flex items-center gap-1 text-[11px] text-gray-400"
                      title={formatDateTime(log.timestamp)}
                    >
                      <Clock className="w-3 h-3 text-gray-400" />
                      <span>{formatRelativeTime(log.timestamp)}</span>
                    </div>
                  </div>
                </div>
              );
            })
          ) : (
            <div className="py-16 text-center text-gray-400 flex flex-col items-center justify-center">
              <History className="w-12 h-12 text-gray-300 mb-2 stroke-[1.5]" />
              <div className="font-bold text-gray-700 text-sm">Brak wpisów w historii dywanów</div>
              <p className="text-xs text-gray-500 mt-1 max-w-sm">
                {searchQuery || selectedAction !== 'all' || selectedUser !== 'all'
                  ? 'Żadne zarejestrowane zmiany nie spełniają wybranych kryteriów wyszukiwania.'
                  : 'Wszelkie operacje (kto dodał, zmienił lub zdjął dywan) będą automatycznie zapisywane tutaj.'}
              </p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-4 py-3 bg-gray-50 border-t border-gray-200 flex items-center justify-between text-xs text-gray-500">
          <div>
            Zarejestrowano ogółem: <strong className="text-gray-800">{carpetLogs.length}</strong> operacji
            {filteredLogs.length !== carpetLogs.length && (
              <span className="ml-1 text-purple-600 font-semibold">
                (wyświetlane: {filteredLogs.length})
              </span>
            )}
          </div>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-800 font-bold rounded-xl transition-colors cursor-pointer"
          >
            Zamknij
          </button>
        </div>
      </div>
    </div>
  );
}

function getActionLabel(action: ActivityAction): string {
  switch (action) {
    case 'add':
      return 'Dodał dywan';
    case 'edit':
      return 'Zmienił dywan';
    case 'delete':
      return 'Zdjął dywan';
    default:
      return action;
  }
}

function renderActionBadge(action: ActivityAction) {
  switch (action) {
    case 'add':
      return (
        <span className="px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider bg-emerald-100 text-emerald-800 border border-emerald-200">
          Dodał dywan
        </span>
      );
    case 'edit':
      return (
        <span className="px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider bg-blue-100 text-blue-800 border border-blue-200">
          Zmienił dywan
        </span>
      );
    case 'delete':
      return (
        <span className="px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider bg-rose-100 text-rose-800 border border-rose-200">
          Zdjął dywan
        </span>
      );
    default:
      return (
        <span className="px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider bg-gray-100 text-gray-800">
          {action}
        </span>
      );
  }
}

function renderActionIcon(action: ActivityAction) {
  switch (action) {
    case 'add':
      return (
        <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-100 shadow-2xs">
          <PlusCircle className="w-4 h-4" />
        </div>
      );
    case 'edit':
      return (
        <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center border border-blue-100 shadow-2xs">
          <Pencil className="w-4 h-4" />
        </div>
      );
    case 'delete':
      return (
        <div className="w-8 h-8 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center border border-rose-100 shadow-2xs">
          <Trash2 className="w-4 h-4" />
        </div>
      );
    default:
      return (
        <div className="w-8 h-8 rounded-xl bg-gray-50 text-gray-600 flex items-center justify-center">
          <History className="w-4 h-4" />
        </div>
      );
  }
}
