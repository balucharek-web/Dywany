import { useState, useEffect } from 'react';
import {
  collection,
  onSnapshot,
  doc,
  setDoc,
  deleteDoc,
  writeBatch
} from 'firebase/firestore';
import {
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  User
} from 'firebase/auth';
import { db, auth, googleProvider, handleFirestoreError, OperationType } from './firebase';
import { Carpet, AdminUser, SUPER_ADMIN_EMAIL, STORE_NUMBER, STORE_CITY } from './types';
import { initialCarpets } from './data/seedData';
import { Header } from './components/Header';
import { SearchView } from './components/SearchView';
import { WarehousePanel } from './components/WarehousePanel';
import { LoginModal } from './components/LoginModal';
import { BarcodeScannerModal } from './components/BarcodeScannerModal';
import { FiszkaModal } from './components/FiszkaModal';
import { FiszkiBatchModal, FiszkaItem } from './components/FiszkiBatchModal';
import { StockExpoModal } from './components/StockExpoModal';
import { ImportModal } from './components/ImportModal';
import { EditCarpetModal } from './components/EditCarpetModal';
import { AdminManagementModal } from './components/AdminManagementModal';
import { HelpModal } from './components/HelpModal';
import { ShieldAlert, LogOut, CheckCircle2 } from 'lucide-react';

export default function App() {
  const [currentView, setCurrentView] = useState<'search' | 'warehouse'>('search');
  const [carpets, setCarpets] = useState<Carpet[]>([]);
  const [admins, setAdmins] = useState<AdminUser[]>([]);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);

  // Search state
  const [searchQuery, setSearchQuery] = useState('');

  // Modals state
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isScannerModalOpen, setIsScannerModalOpen] = useState(false);
  const [fiszkaCarpet, setFiszkaCarpet] = useState<Carpet | null>(null);
  const [isFiszkiBatchModalOpen, setIsFiszkiBatchModalOpen] = useState(false);
  const [batchFiszkiItems, setBatchFiszkiItems] = useState<FiszkaItem[]>([]);
  const [isStockExpoOpen, setIsStockExpoOpen] = useState(false);
  const [isImportOpen, setIsImportOpen] = useState(false);
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isAdminManagementOpen, setIsAdminManagementOpen] = useState(false);

  // Edit carpet modal state
  const [isEditCarpetOpen, setIsEditCarpetOpen] = useState(false);
  const [carpetToEdit, setCarpetToEdit] = useState<Partial<Carpet> | null>(null);

  // Auth observer
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user);
      setAuthLoading(false);
    });
    return () => unsubscribe();
  }, []);

  // Real-time synchronization of Carpets from Firestore
  useEffect(() => {
    setIsSyncing(true);
    const unsubscribeCarpets = onSnapshot(
      collection(db, 'carpets'),
      (snapshot) => {
        const loadedCarpets: Carpet[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          loadedCarpets.push({
            id: docSnap.id,
            magazyn: Number(data.magazyn) || 1,
            podmagazyn: data.podmagazyn || 'A',
            miejsce: data.miejsce || `${data.magazyn} ${data.podmagazyn}`,
            nazwa: data.nazwa || '',
            ref: data.ref || '',
            ean: data.ean || '',
            dodano: data.dodano || '',
            updatedAt: data.updatedAt,
            updatedBy: data.updatedBy
          });
        });

        // If Firestore is empty on first ever run, seed with sample data
        if (loadedCarpets.length === 0) {
          setCarpets(initialCarpets);
        } else {
          setCarpets(loadedCarpets);
        }
        setIsSyncing(false);
      },
      (error) => {
        setIsSyncing(false);
        handleFirestoreError(error, OperationType.LIST, 'carpets');
      }
    );

    return () => unsubscribeCarpets();
  }, []);

  // Real-time synchronization of Admins from Firestore
  useEffect(() => {
    const unsubscribeAdmins = onSnapshot(
      collection(db, 'admins'),
      (snapshot) => {
        const loadedAdmins: AdminUser[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          loadedAdmins.push({
            email: data.email || docSnap.id,
            role: data.role || 'admin',
            addedBy: data.addedBy,
            createdAt: data.createdAt || ''
          });
        });
        setAdmins(loadedAdmins);
      },
      (error) => {
        // Unauthenticated users or non-admins might not read admins; log gently
        console.warn('Admins collection snapshot notice:', error.message);
      }
    );

    return () => unsubscribeAdmins();
  }, [currentUser]);

  // Authorization checks
  const isSuperAdmin = Boolean(
    currentUser?.email && currentUser.email.toLowerCase() === SUPER_ADMIN_EMAIL.toLowerCase()
  );

  const isAdmin = Boolean(
    isSuperAdmin ||
      (currentUser?.email &&
        admins.some((a) => a.email.toLowerCase() === currentUser.email?.toLowerCase()))
  );

  // Google Sign-In
  const handleGoogleSignIn = async () => {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      if (result.user) {
        setCurrentView('warehouse');
      }
    } catch (err) {
      console.error('Google Sign In Error:', err);
      throw err;
    }
  };

  // Logout
  const handleLogout = async () => {
    await signOut(auth);
    setCurrentView('search');
  };

  // Add / Edit Carpet
  const handleSaveCarpet = async (data: Partial<Carpet>) => {
    if (!isAdmin) {
      alert('Tylko uprawnieni administratorzy sklepu 038 mogą edytować magazyn.');
      return;
    }

    const docId = data.id || `mag_${data.magazyn}_${data.podmagazyn?.toUpperCase()}`;
    const payload = {
      magazyn: Number(data.magazyn),
      podmagazyn: String(data.podmagazyn).toUpperCase(),
      miejsce: data.miejsce || `${data.magazyn} ${data.podmagazyn}`,
      nazwa: data.nazwa || '',
      ref: data.ref || '',
      ean: data.ean || '',
      dodano: data.dodano || new Date().toISOString().split('T')[0],
      updatedAt: new Date().toISOString(),
      updatedBy: currentUser?.email || 'admin'
    };

    try {
      await setDoc(doc(db, 'carpets', docId), payload, { merge: true });
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `carpets/${docId}`);
    }
  };

  // Delete Carpet
  const handleDeleteCarpet = async (carpetId: string, name: string) => {
    if (!isAdmin) {
      alert('Brak uprawnień do usuwania pozycji z magazynu.');
      return;
    }

    if (window.confirm(`Czy na pewno chcesz usunąć pozycję:\n"${name}"?`)) {
      try {
        await deleteDoc(doc(db, 'carpets', carpetId));
      } catch (error) {
        handleFirestoreError(error, OperationType.DELETE, `carpets/${carpetId}`);
      }
    }
  };

  // Bulk Import
  const handleImportCarpets = async (incoming: Carpet[], mode: 'merge' | 'replace') => {
    if (!isAdmin) {
      throw new Error('Brak uprawnień do importowania bazy.');
    }

    try {
      const batch = writeBatch(db);

      if (mode === 'replace') {
        carpets.forEach((c) => {
          batch.delete(doc(db, 'carpets', c.id));
        });
      }

      incoming.forEach((c, idx) => {
        const docId = c.id || `mag_${c.magazyn}_${c.podmagazyn?.toUpperCase() || 'A'}`;
        const ref = doc(db, 'carpets', docId);
        batch.set(
          ref,
          {
            magazyn: Number(c.magazyn) || 1,
            podmagazyn: String(c.podmagazyn || 'A').toUpperCase(),
            miejsce: c.miejsce || `${c.magazyn} ${c.podmagazyn}`,
            nazwa: c.nazwa || '',
            ref: c.ref || '',
            ean: c.ean || '',
            dodano: c.dodano || new Date().toISOString().split('T')[0],
            updatedAt: new Date().toISOString(),
            updatedBy: currentUser?.email || 'import'
          },
          { merge: true }
        );
      });

      await batch.commit();
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, 'carpets/batch');
    }
  };

  // Super Admin: Add Admin Email
  const handleAddAdminEmail = async (email: string) => {
    if (!isSuperAdmin) {
      throw new Error('Tylko Super Admin może dodawać administratorów.');
    }
    const normalized = email.trim().toLowerCase();
    const docId = normalized;
    const payload: AdminUser = {
      email: normalized,
      role: 'admin',
      addedBy: currentUser?.email || SUPER_ADMIN_EMAIL,
      createdAt: new Date().toISOString().split('T')[0]
    };

    try {
      await setDoc(doc(db, 'admins', docId), payload);
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `admins/${docId}`);
    }
  };

  // Super Admin: Remove Admin Email
  const handleRemoveAdminEmail = async (email: string) => {
    if (!isSuperAdmin) {
      throw new Error('Tylko Super Admin może usuwać administratorów.');
    }
    const docId = email.trim().toLowerCase();
    try {
      await deleteDoc(doc(db, 'admins', docId));
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, `admins/${docId}`);
    }
  };

  // Scanner callback
  const handleScanSuccess = (decodedText: string) => {
    setSearchQuery(decodedText);
    setCurrentView('search');
  };

  // Open Add carpet dialog for specific slot
  const handleOpenAdd = (bay?: number, subBay?: string) => {
    if (!isAdmin) {
      setIsLoginModalOpen(true);
      return;
    }
    setCarpetToEdit({
      magazyn: bay || 1,
      podmagazyn: subBay || 'A',
      miejsce: bay ? `${bay} ${subBay || 'A'}` : '1 A',
      dodano: new Date().toISOString().split('T')[0]
    });
    setIsEditCarpetOpen(true);
  };

  const handleOpenEdit = (carpet: Carpet) => {
    if (!isAdmin) {
      setIsLoginModalOpen(true);
      return;
    }
    setCarpetToEdit(carpet);
    setIsEditCarpetOpen(true);
  };

  const handleOpenFiszkiBatch = (carpetToAdd?: Carpet) => {
    if (carpetToAdd && carpetToAdd.ref) {
      const cleanRef = carpetToAdd.ref.trim();
      setBatchFiszkiItems((prev) => {
        if (prev.some((i) => i.ref === cleanRef)) return prev;
        return [
          ...prev,
          {
            id: `${cleanRef}-${Date.now()}`,
            ref: cleanRef,
            nazwa: carpetToAdd.nazwa,
            miejsce: carpetToAdd.miejsce || `${carpetToAdd.magazyn}${carpetToAdd.podmagazyn}`,
            ean: carpetToAdd.ean,
            addedAt: Date.now()
          }
        ];
      });
    }
    setIsFiszkiBatchModalOpen(true);
  };

  return (
    <div className="min-h-screen bg-[#f3f4f6] text-gray-900 flex flex-col font-sans">
      {/* Top Header */}
      <Header
        currentView={currentView}
        setCurrentView={(view) => {
          if (view === 'warehouse' && (!currentUser || !isAdmin)) {
            setIsLoginModalOpen(true);
          } else {
            setCurrentView(view);
          }
        }}
        user={currentUser}
        isAdmin={isAdmin}
        isSuperAdmin={isSuperAdmin}
        onLoginClick={() => setIsLoginModalOpen(true)}
        onLogoutClick={handleLogout}
        onOpenSuperAdminModal={() => setIsAdminManagementOpen(true)}
        isSyncing={isSyncing}
      />

      {/* Unauthorized Warning Banner if logged in user is not admin */}
      {currentUser && !isAdmin && (
        <div className="bg-amber-100 border-b border-amber-300 px-4 py-3 text-xs sm:text-sm text-amber-950 flex flex-wrap items-center justify-between gap-3 shadow-2xs">
          <div className="flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-amber-700 shrink-0" />
            <div>
              Zalogowano jako: <strong className="font-mono">{currentUser.email}</strong>. Twoje konto nie zostało jeszcze dodane do listy administratorów Sklepu {STORE_NUMBER}. Skontaktuj się z Super Administratorem (<span className="font-mono font-bold">{SUPER_ADMIN_EMAIL}</span>).
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="px-3 py-1 bg-amber-200 hover:bg-amber-300 font-bold rounded-lg text-amber-900 transition-colors"
          >
            Wyloguj
          </button>
        </div>
      )}

      {/* Main View Area */}
      <main className="flex-1 pb-16">
        {currentView === 'search' ? (
          <SearchView
            carpets={carpets}
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
            onOpenScanner={() => setIsScannerModalOpen(true)}
            onOpenFiszkiModal={handleOpenFiszkiBatch}
            onPrintSingleFiszka={(c) => setFiszkaCarpet(c)}
            onOpenHelp={() => setIsHelpOpen(true)}
          />
        ) : (
          <WarehousePanel
            carpets={carpets}
            onAddProduct={(bay, subBay) => handleOpenAdd(bay, subBay)}
            onEditProduct={handleOpenEdit}
            onDeleteProduct={handleDeleteCarpet}
            onOpenImport={() => setIsImportOpen(true)}
            onOpenStockExpo={() => setIsStockExpoOpen(true)}
            onOpenHelp={() => setIsHelpOpen(true)}
            onLogout={handleLogout}
            onGenerateFiszka={(c) => handleOpenFiszkiBatch(c)}
            isSuperAdmin={isSuperAdmin}
            onOpenSuperAdminModal={() => setIsAdminManagementOpen(true)}
          />
        )}
      </main>

      {/* Modals */}
      <LoginModal
        isOpen={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onGoogleSignIn={handleGoogleSignIn}
      />

      <BarcodeScannerModal
        isOpen={isScannerModalOpen}
        onClose={() => setIsScannerModalOpen(false)}
        onScanSuccess={handleScanSuccess}
      />

      <FiszkiBatchModal
        isOpen={isFiszkiBatchModalOpen}
        onClose={() => setIsFiszkiBatchModalOpen(false)}
        carpets={carpets}
        initialItems={batchFiszkiItems}
        onItemsChange={setBatchFiszkiItems}
      />

      <FiszkaModal
        carpet={fiszkaCarpet}
        onClose={() => setFiszkaCarpet(null)}
      />

      <StockExpoModal
        carpets={carpets}
        isOpen={isStockExpoOpen}
        onClose={() => setIsStockExpoOpen(false)}
        onSelectSlot={(bay, sub) => {
          setCurrentView('warehouse');
          handleOpenAdd(bay, sub);
        }}
      />

      <ImportModal
        isOpen={isImportOpen}
        onClose={() => setIsImportOpen(false)}
        onImport={handleImportCarpets}
      />

      <EditCarpetModal
        isOpen={isEditCarpetOpen}
        onClose={() => {
          setIsEditCarpetOpen(false);
          setCarpetToEdit(null);
        }}
        carpetToEdit={carpetToEdit}
        onSave={handleSaveCarpet}
        existingMagazyny={Array.from(new Set(carpets.map((c) => c.magazyn)))}
      />

      <AdminManagementModal
        isOpen={isAdminManagementOpen}
        onClose={() => setIsAdminManagementOpen(false)}
        admins={admins}
        onAddAdmin={handleAddAdminEmail}
        onRemoveAdmin={handleRemoveAdminEmail}
        currentSuperAdminEmail={SUPER_ADMIN_EMAIL}
      />

      <HelpModal
        isOpen={isHelpOpen}
        onClose={() => setIsHelpOpen(false)}
      />
    </div>
  );
}
