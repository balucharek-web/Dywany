import React, { useState, useEffect, useMemo } from 'react';
import {
  CarpetSlot,
  SlotFilter,
  RackRange,
  StoreStats,
  UserProfile
} from './types';
import {
  subscribeToSlots,
  subscribeToAuth,
  loginWithGoogle,
  logoutUser,
  saveCarpetSlot,
  clearCarpetSlot,
  moveCarpetSlot,
  seedFirestoreIfEmpty,
  getInitialSlots
} from './services/firebase';
import { Navbar } from './components/Navbar';
import { StatsHeader } from './components/StatsHeader';
import { SearchBarWithFilters } from './components/SearchBarWithFilters';
import { ShowroomBanner } from './components/ShowroomBanner';
import { RackCard } from './components/RackCard';
import { EditSlotModal } from './components/EditSlotModal';
import { MoveCarpetModal } from './components/MoveCarpetModal';
import { BarcodeScannerModal } from './components/BarcodeScannerModal';
import { Toast } from './components/Toast';
import { QrCode, SearchX } from 'lucide-react';

export const App: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [slots, setSlots] = useState<CarpetSlot[]>(() => getInitialSlots());

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilter, setSelectedFilter] = useState<SlotFilter>('ALL');
  const [racksFilter, setRacksFilter] = useState<RackRange>('ALL');

  const [editingSlot, setEditingSlot] = useState<CarpetSlot | null>(null);
  const [movingSlot, setMovingSlot] = useState<CarpetSlot | null>(null);
  const [isScannerVisible, setIsScannerVisible] = useState(false);

  const [toast, setToast] = useState<{ message: string; type?: 'success' | 'error' | 'info' } | null>(null);

  // Subscribe to Auth
  useEffect(() => {
    const unsub = subscribeToAuth((user) => {
      setCurrentUser(user);
      if (user?.email) {
        seedFirestoreIfEmpty(user.email);
      }
    });
    return () => unsub();
  }, []);

  // Subscribe to Realtime Slots
  useEffect(() => {
    const unsub = subscribeToSlots(
      (newSlots) => {
        setSlots(newSlots);
      },
      (err) => {
        console.warn("Firestore status:", err);
      }
    );
    return () => unsub();
  }, []);

  const handleLogin = async () => {
    setIsLoggingIn(true);
    try {
      const user = await loginWithGoogle();
      setCurrentUser(user);
      setToast({ message: `Zalogowano: ${user.displayName || user.email}`, type: 'success' });
      if (user.email) {
        await seedFirestoreIfEmpty(user.email);
      }
    } catch (err: any) {
      console.error(err);
      setToast({ message: `Logowanie: ${err?.message || 'anulowano lub wystąpił błąd'}`, type: 'error' });
    } finally {
      setIsLoggingIn(false);
    }
  };

  const handleLogout = async () => {
    try {
      await logoutUser();
      setCurrentUser(null);
      setToast({ message: 'Wylogowano pomyślnie', type: 'info' });
    } catch (err: any) {
      setToast({ message: `Błąd wylogowania: ${err.message}`, type: 'error' });
    }
  };

  // Compute store stats
  const stats: StoreStats = useMemo(() => {
    const racksSet = new Set(slots.map((s) => s.rackNumber));
    const maxRack = racksSet.size > 0 ? Math.max(...Array.from(racksSet), 20) : 20;
    const occupied = slots.filter((s) => s.occupied).length;
    const total = slots.length;
    return {
      totalRacks: maxRack,
      totalSlots: total,
      occupiedCount: occupied,
      emptyCount: Math.max(0, total - occupied)
    };
  }, [slots]);

  // Filter helper
  const matchesSearch = (slot: CarpetSlot, query: string): boolean => {
    if (!query) return true;
    const q = query.trim().toLowerCase();
    const loc = `${slot.rackNumber}${slot.slotLetter}`.toLowerCase();
    const rackLabel = `stojak ${slot.rackNumber}`.toLowerCase();
    const magLabel = `magazyn ${slot.rackNumber}`.toLowerCase();

    return (
      loc.includes(q) ||
      loc === q ||
      rackLabel.includes(q) ||
      magLabel.includes(q) ||
      slot.slotId.toLowerCase().includes(q) ||
      slot.eslCode.toLowerCase().includes(q) ||
      slot.ean.toLowerCase().includes(q) ||
      slot.referenceNumber.toLowerCase().includes(q) ||
      slot.productName.toLowerCase().includes(q)
    );
  };

  const matchesFilter = (slot: CarpetSlot, filter: SlotFilter): boolean => {
    if (filter === 'ALL') return true;
    if (filter === 'OCCUPIED') return slot.occupied;
    if (filter === 'EMPTY') return !slot.occupied;
    return true;
  };

  // Group slots by rack number into Map
  const filteredRacks = useMemo(() => {
    const maxRack = Math.max(20, ...slots.map((s) => s.rackNumber));
    const allRackNums = Array.from({ length: maxRack }, (_, i) => i + 1);

    const result: Array<{ rackNumber: number; slotA: CarpetSlot; slotB: CarpetSlot }> = [];

    allRackNums.forEach((rackNum) => {
      // Check rack range filter
      let inRange = true;
      if (racksFilter === '1-10') inRange = rackNum >= 1 && rackNum <= 10;
      else if (racksFilter === '11-20') inRange = rackNum >= 11 && rackNum <= 20;
      else if (racksFilter === '21-30') inRange = rackNum >= 21 && rackNum <= 30;

      if (!inRange) return;

      const slotA =
        slots.find((s) => s.rackNumber === rackNum && s.slotLetter.toLowerCase() === 'a') || {
          slotId: `${rackNum}a`,
          rackNumber: rackNum,
          slotLetter: 'a',
          occupied: false,
          productName: '',
          ean: '',
          referenceNumber: '',
          eslCode: `ESL-${rackNum}A`,
          price: '',
          imageUrl: ''
        };

      const slotB =
        slots.find((s) => s.rackNumber === rackNum && s.slotLetter.toLowerCase() === 'b') || {
          slotId: `${rackNum}b`,
          rackNumber: rackNum,
          slotLetter: 'b',
          occupied: false,
          productName: '',
          ean: '',
          referenceNumber: '',
          eslCode: `ESL-${rackNum}B`,
          price: '',
          imageUrl: ''
        };

      const matchA = matchesSearch(slotA, searchQuery) && matchesFilter(slotA, selectedFilter);
      const matchB = matchesSearch(slotB, searchQuery) && matchesFilter(slotB, selectedFilter);

      if (!searchQuery.trim() && selectedFilter === 'ALL') {
        result.push({ rackNumber: rackNum, slotA, slotB });
      } else if (matchA || matchB) {
        result.push({ rackNumber: rackNum, slotA, slotB });
      }
    });

    return result;
  }, [slots, searchQuery, selectedFilter, racksFilter]);

  const handleSaveSlot = async (
    slotId: string,
    rackNumber: number,
    slotLetter: string,
    productName: string,
    ean: string,
    referenceNumber: string,
    eslCode: string,
    price: string,
    imageUrl: string
  ) => {
    const userEmail = currentUser?.email || 'pracownik@dywanmag.pl';
    try {
      await saveCarpetSlot(
        {
          slotId,
          rackNumber,
          slotLetter,
          productName,
          ean,
          referenceNumber,
          eslCode,
          price,
          imageUrl
        },
        userEmail
      );
      setEditingSlot(null);
      setToast({ message: `Zapisano dywan na miejscu ${slotId.toUpperCase()}`, type: 'success' });
    } catch (err: any) {
      setToast({ message: `Błąd zapisu: ${err.message}`, type: 'error' });
    }
  };

  const handleClearSlot = async (slotId: string) => {
    const userEmail = currentUser?.email || 'pracownik@dywanmag.pl';
    try {
      await clearCarpetSlot(slotId, userEmail);
      setEditingSlot(null);
      setToast({ message: `Zwolniono miejsce ${slotId.toUpperCase()}`, type: 'info' });
    } catch (err: any) {
      setToast({ message: `Błąd: ${err.message}`, type: 'error' });
    }
  };

  const handleMoveSlot = async (
    targetSlotId: string,
    targetRack: number,
    targetLetter: string
  ) => {
    if (!movingSlot) return;
    const userEmail = currentUser?.email || 'pracownik@dywanmag.pl';
    try {
      await moveCarpetSlot(
        movingSlot.slotId,
        targetSlotId,
        targetRack,
        targetLetter,
        userEmail
      );
      setMovingSlot(null);
      setToast({
        message: `Przeniesiono dywan z ${movingSlot.slotId.toUpperCase()} na ${targetSlotId.toUpperCase()}`,
        type: 'success'
      });
    } catch (err: any) {
      setToast({ message: `Błąd przenoszenia: ${err.message}`, type: 'error' });
    }
  };

  const handleScannedCode = (code: string) => {
    setIsScannerVisible(false);
    setSearchQuery(code);
    setToast({ message: `Zeskanowano kod: ${code}`, type: 'info' });
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      {/* Top Navbar */}
      <Navbar
        currentUser={currentUser}
        isLoggingIn={isLoggingIn}
        onLogin={handleLogin}
        onLogout={handleLogout}
        onOpenScanner={() => setIsScannerVisible(true)}
      />

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 flex-1 w-full space-y-6">
        {/* Statistics Header */}
        <StatsHeader
          stats={stats}
          currentUser={currentUser}
          onLogin={handleLogin}
        />

        {/* Search Bar & Status/Range Filters */}
        <SearchBarWithFilters
          query={searchQuery}
          onQueryChange={setSearchQuery}
          currentFilter={selectedFilter}
          onFilterChange={setSelectedFilter}
          currentRange={racksFilter}
          onRangeChange={setRacksFilter}
          onOpenScanner={() => setIsScannerVisible(true)}
        />

        {/* Showroom Information Banner */}
        <ShowroomBanner />

        {/* Racks Grid Display */}
        {filteredRacks.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {filteredRacks.map(({ rackNumber, slotA, slotB }) => (
              <RackCard
                key={rackNumber}
                rackNumber={rackNumber}
                slotA={slotA}
                slotB={slotB}
                isAuthenticated={Boolean(currentUser)}
                onEditSlot={(slot) => setEditingSlot(slot)}
                onAddSlot={(rackNum, letter) =>
                  setEditingSlot({
                    slotId: `${rackNum}${letter}`,
                    rackNumber: rackNum,
                    slotLetter: letter,
                    occupied: false,
                    productName: '',
                    ean: '',
                    referenceNumber: '',
                    eslCode: `ESL-${rackNum}${letter.toUpperCase()}`,
                    price: '',
                    imageUrl: ''
                  })
                }
                onMoveSlot={(slot) => setMovingSlot(slot)}
                onPromptLogin={handleLogin}
              />
            ))}
          </div>
        ) : (
          /* Empty Search Results State */
          <div className="text-center py-12 bg-white rounded-2xl border border-slate-200/80 p-8 space-y-3 shadow-xs">
            <div className="w-14 h-14 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto text-2xl">
              <SearchX className="w-7 h-7 text-slate-400" />
            </div>
            <h3 className="text-base font-bold text-slate-800">
              Brak wyników wyszukiwania
            </h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Nie znaleziono dywanu ani stojaka pasującego do zapytania "{searchQuery}". Sprawdź numer miejsca (np. 1a) lub zresetuj filtry.
            </p>
            <button
              onClick={() => {
                setSearchQuery('');
                setSelectedFilter('ALL');
                setRacksFilter('ALL');
              }}
              className="px-4 py-2 rounded-xl bg-blue-50 text-blue-700 font-semibold text-xs hover:bg-blue-100 transition cursor-pointer"
            >
              Pokaż wszystkie stojaki
            </button>
          </div>
        )}
      </main>

      {/* Floating Action Button for Scanning Barcode / ESL */}
      <button
        onClick={() => setIsScannerVisible(true)}
        className="fixed bottom-6 right-6 z-40 flex items-center space-x-2 px-5 py-3 rounded-full bg-blue-700 hover:bg-blue-800 text-white font-bold text-sm shadow-xl transition transform hover:scale-105 cursor-pointer"
      >
        <QrCode className="w-5 h-5 text-white" />
        <span className="hidden sm:inline">Skanuj ESL / EAN</span>
      </button>

      {/* Modals */}
      {editingSlot && (
        <EditSlotModal
          slot={editingSlot}
          onSave={handleSaveSlot}
          onClear={handleClearSlot}
          onClose={() => setEditingSlot(null)}
          onOpenScanner={() => setIsScannerVisible(true)}
        />
      )}

      {movingSlot && (
        <MoveCarpetModal
          sourceSlot={movingSlot}
          onMove={handleMoveSlot}
          onClose={() => setMovingSlot(null)}
        />
      )}

      {isScannerVisible && (
        <BarcodeScannerModal
          onScanCode={handleScannedCode}
          onClose={() => setIsScannerVisible(false)}
        />
      )}

      {/* Toast Notification */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
};
