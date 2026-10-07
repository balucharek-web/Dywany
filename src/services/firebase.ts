import { initializeApp } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut as firebaseSignOut,
  onAuthStateChanged,
  User
} from 'firebase/auth';
import {
  getFirestore,
  collection,
  doc,
  onSnapshot,
  setDoc,
  updateDoc,
  serverTimestamp,
  getDocs,
  limit,
  query,
  Firestore
} from 'firebase/firestore';
import { CarpetSlot, UserProfile } from '../types';
import { KNOWN_LEROY_PRODUCTS } from './leroyMerlin';

const firebaseConfig = {
  apiKey: "AIzaSyBfmbYlKbCZu81TsjSoCuHixI0Bdxpeh8Y",
  authDomain: "gen-lang-client-0789992790.firebaseapp.com",
  projectId: "gen-lang-client-0789992790",
  storageBucket: "gen-lang-client-0789992790.firebasestorage.app",
  appId: "1:448858732402:android:351f0044e201fe470a0feb"
};

const FIRESTORE_DATABASE_ID = "ai-studio-android-dywanmag-8b679ca7-c217-41bb-9c0a-86c3b19f66d5";

let app: any;
let auth: any;
let db: Firestore | null = null;

try {
  app = initializeApp(firebaseConfig);
  auth = getAuth(app);
  db = getFirestore(app, FIRESTORE_DATABASE_ID);
} catch (e) {
  console.warn("Firebase initialization notice:", e);
}

const STORAGE_KEY = 'dywanmag_slots_cache';

export function getInitialSlots(): CarpetSlot[] {
  try {
    const cached = localStorage.getItem(STORAGE_KEY);
    if (cached) {
      const parsed = JSON.parse(cached);
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed;
      }
    }
  } catch (e) {
    console.error("Error reading cache", e);
  }

  // Generate standard 20 racks (40 slots: 1a..20b)
  const slots: CarpetSlot[] = [];
  const initialProducts = KNOWN_LEROY_PRODUCTS;

  let productIdx = 0;
  for (let rack = 1; rack <= 20; rack++) {
    for (const letter of ['a', 'b']) {
      const slotId = `${rack}${letter}`;
      const isOccupied = productIdx < initialProducts.length;

      if (isOccupied) {
        const prod = initialProducts[productIdx];
        slots.push({
          slotId,
          rackNumber: rack,
          slotLetter: letter,
          occupied: true,
          productName: prod.name,
          ean: prod.ean,
          referenceNumber: prod.referenceNumber,
          eslCode: `ESL-${slotId.toUpperCase()}`,
          price: prod.price,
          imageUrl: prod.imageUrl,
          updatedBy: 'system@dywanmag.pl',
          updatedAt: new Date().toISOString(),
          createdAt: new Date().toISOString()
        });
        productIdx++;
      } else {
        slots.push({
          slotId,
          rackNumber: rack,
          slotLetter: letter,
          occupied: false,
          productName: '',
          ean: '',
          referenceNumber: '',
          eslCode: `ESL-${slotId.toUpperCase()}`,
          price: '',
          imageUrl: '',
          updatedBy: 'system@dywanmag.pl',
          updatedAt: new Date().toISOString(),
          createdAt: new Date().toISOString()
        });
      }
    }
  }

  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(slots));
  } catch (e) {}

  return slots;
}

export function saveSlotsToStorage(slots: CarpetSlot[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(slots));
  } catch (e) {}
}

// Subscribe to Auth state
export function subscribeToAuth(callback: (user: UserProfile | null) => void) {
  if (!auth) {
    callback(null);
    return () => {};
  }
  return onAuthStateChanged(auth, (user: User | null) => {
    if (user) {
      callback({
        uid: user.uid,
        displayName: user.displayName,
        email: user.email,
        photoURL: user.photoURL
      });
    } else {
      callback(null);
    }
  });
}

export async function loginWithGoogle(): Promise<UserProfile> {
  if (!auth) throw new Error("Auth service unavailable");
  const provider = new GoogleAuthProvider();
  const result = await signInWithPopup(auth, provider);
  return {
    uid: result.user.uid,
    displayName: result.user.displayName,
    email: result.user.email,
    photoURL: result.user.photoURL
  };
}

export async function logoutUser(): Promise<void> {
  if (!auth) return;
  await firebaseSignOut(auth);
}

// Subscribe to real-time slots collection
export function subscribeToSlots(
  onUpdate: (slots: CarpetSlot[]) => void,
  onError?: (err: any) => void
) {
  if (!db) {
    onUpdate(getInitialSlots());
    return () => {};
  }

  const slotsCol = collection(db, 'slots');
  const unsubscribe = onSnapshot(
    slotsCol,
    (snapshot) => {
      if (!snapshot.empty) {
        const slots: CarpetSlot[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          slots.push({
            slotId: docSnap.id,
            rackNumber: Number(data.rackNumber) || 1,
            slotLetter: data.slotLetter || 'a',
            occupied: Boolean(data.occupied),
            productName: data.productName || '',
            ean: data.ean || '',
            referenceNumber: data.referenceNumber || '',
            eslCode: data.eslCode || '',
            price: data.price || '',
            imageUrl: data.imageUrl || '',
            updatedBy: data.updatedBy || '',
            updatedAt: data.updatedAt,
            createdAt: data.createdAt
          });
        });

        slots.sort((a, b) => {
          if (a.rackNumber !== b.rackNumber) return a.rackNumber - b.rackNumber;
          return a.slotLetter.localeCompare(b.slotLetter);
        });

        saveSlotsToStorage(slots);
        onUpdate(slots);
      } else {
        // Fallback to local initialized slots
        onUpdate(getInitialSlots());
      }
    },
    (error) => {
      console.warn("Firestore snapshot listener notification:", error);
      if (onError) onError(error);
      onUpdate(getInitialSlots());
    }
  );

  return unsubscribe;
}

// Save or update carpet slot
export async function saveCarpetSlot(
  slot: {
    slotId: string;
    rackNumber: number;
    slotLetter: string;
    productName: string;
    ean: string;
    referenceNumber: string;
    eslCode: string;
    price: string;
    imageUrl: string;
  },
  userEmail: string
): Promise<void> {
  // Update local cache immediately for responsive UX
  const cached = getInitialSlots();
  const idx = cached.findIndex(s => s.slotId === slot.slotId);
  const now = new Date().toISOString();
  const updatedSlot: CarpetSlot = {
    slotId: slot.slotId,
    rackNumber: slot.rackNumber,
    slotLetter: slot.slotLetter,
    occupied: true,
    productName: slot.productName,
    ean: slot.ean,
    referenceNumber: slot.referenceNumber,
    eslCode: slot.eslCode,
    price: slot.price,
    imageUrl: slot.imageUrl,
    updatedBy: userEmail,
    updatedAt: now,
    createdAt: idx >= 0 ? cached[idx].createdAt || now : now
  };

  if (idx >= 0) {
    cached[idx] = updatedSlot;
  } else {
    cached.push(updatedSlot);
  }
  saveSlotsToStorage(cached);

  // Sync to Firestore
  if (db && auth?.currentUser) {
    try {
      const docRef = doc(db, 'slots', slot.slotId);
      await setDoc(docRef, {
        slotId: slot.slotId,
        rackNumber: slot.rackNumber,
        slotLetter: slot.slotLetter,
        occupied: true,
        productName: slot.productName,
        ean: slot.ean,
        referenceNumber: slot.referenceNumber,
        eslCode: slot.eslCode,
        price: slot.price,
        imageUrl: slot.imageUrl,
        updatedBy: userEmail,
        updatedAt: serverTimestamp(),
        createdAt: idx >= 0 && cached[idx].createdAt ? cached[idx].createdAt : serverTimestamp()
      }, { merge: true });
    } catch (err) {
      console.warn("Firestore save fallback to local:", err);
    }
  }
}

// Clear carpet slot (free up rack location)
export async function clearCarpetSlot(slotId: string, userEmail: string): Promise<void> {
  const cached = getInitialSlots();
  const target = cached.find(s => s.slotId === slotId);
  if (target) {
    target.occupied = false;
    target.productName = '';
    target.ean = '';
    target.referenceNumber = '';
    target.eslCode = `ESL-${slotId.toUpperCase()}`;
    target.price = '';
    target.imageUrl = '';
    target.updatedBy = userEmail;
    target.updatedAt = new Date().toISOString();
    saveSlotsToStorage(cached);
  }

  if (db && auth?.currentUser) {
    try {
      const docRef = doc(db, 'slots', slotId);
      await updateDoc(docRef, {
        occupied: false,
        productName: '',
        ean: '',
        referenceNumber: '',
        eslCode: `ESL-${slotId.toUpperCase()}`,
        price: '',
        imageUrl: '',
        updatedBy: userEmail,
        updatedAt: serverTimestamp()
      });
    } catch (err) {
      console.warn("Firestore clear fallback to local:", err);
    }
  }
}

// Move carpet from source to target slot
export async function moveCarpetSlot(
  sourceSlotId: string,
  targetSlotId: string,
  targetRack: number,
  targetLetter: string,
  userEmail: string
): Promise<void> {
  const cached = getInitialSlots();
  const source = cached.find(s => s.slotId === sourceSlotId);
  if (!source) throw new Error("Nie znaleziono źródłowego miejsca dywanu");

  await saveCarpetSlot({
    slotId: targetSlotId,
    rackNumber: targetRack,
    slotLetter: targetLetter,
    productName: source.productName,
    ean: source.ean,
    referenceNumber: source.referenceNumber,
    eslCode: source.eslCode.includes(sourceSlotId.toUpperCase()) ? `ESL-${targetSlotId.toUpperCase()}` : source.eslCode,
    price: source.price,
    imageUrl: source.imageUrl
  }, userEmail);

  await clearCarpetSlot(sourceSlotId, userEmail);
}

// Seed initial 20 racks (40 slots) in Firestore if collection is empty
export async function seedFirestoreIfEmpty(userEmail: string): Promise<void> {
  if (!db || !auth?.currentUser) return;
  try {
    const checkQuery = query(collection(db, 'slots'), limit(1));
    const snapshot = await getDocs(checkQuery);
    if (!snapshot.empty) return;

    const initialSlots = getInitialSlots();
    for (const slot of initialSlots) {
      const docRef = doc(db, 'slots', slot.slotId);
      await setDoc(docRef, {
        ...slot,
        updatedBy: userEmail,
        updatedAt: serverTimestamp(),
        createdAt: serverTimestamp()
      });
    }
  } catch (err) {
    console.warn("Seed firestore check:", err);
  }
}
