const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { fetchFromLeroyMerlin } = require('./lmCatalogFetcher');

admin.initializeApp();
const db = admin.firestore();

const ROOT_SUPER_ADMIN_EMAIL = 'baluch.arek@gmail.com';

/**
 * Cloud Function: getProductByEAN(ean)
 * 1. Accepts full EAN code (NEVER truncated!)
 * 2. Checks Firestore for existing product
 * 3. If fresh (<24h), returns cached product
 * 4. If missing or stale, fetches fresh data from Leroy Merlin
 * 5. Saves to Firestore (protecting localPrice and localPriceOverride)
 * 6. Returns product
 */
exports.getProductByEAN = functions.https.onCall(async (data, context) => {
  const ean = (data.ean || '').trim();
  if (!ean) {
    throw new functions.https.HttpsError('invalid-argument', 'Parametr EAN jest wymagany.');
  }

  // 1. Query Firestore
  const snap = await db.collection('products').where('ean', '==', ean).limit(1).get();
  const now = Date.now();
  const maxAge = 24 * 3600 * 1000;

  if (!snap.empty) {
    const doc = snap.docs[0];
    const existing = doc.data();
    if (existing.updatedAt && (now - existing.updatedAt < maxAge)) {
      return { id: doc.id, ...existing };
    }
  }

  // 2. Fetch fresh data from LM
  const freshData = await fetchFromLeroyMerlin(ean, 'EAN');
  if (!freshData) {
    throw new functions.https.HttpsError('not-found', `Nie znaleziono produktu dla EAN: ${ean}`);
  }

  let docId = snap.empty ? `prod_${ean}` : snap.docs[0].id;
  const docRef = db.collection('products').doc(docId);

  // Preserve local price override if existing
  if (!snap.empty) {
    const existing = snap.docs[0].data();
    if (existing.localPriceOverride) {
      freshData.localPrice = existing.localPrice;
      freshData.localPriceOverride = true;
    }
  }

  await docRef.set({ id: docId, ...freshData }, { merge: true });
  return { id: docId, ...freshData };
});

/**
 * Cloud Function: getProductByLMNumber(lmSystemNumber)
 */
exports.getProductByLMNumber = functions.https.onCall(async (data, context) => {
  const lmSystemNumber = (data.lmSystemNumber || '').trim();
  if (!lmSystemNumber) {
    throw new functions.https.HttpsError('invalid-argument', 'Parametr lmSystemNumber jest wymagany.');
  }

  const snap = await db.collection('products').where('lmSystemNumber', '==', lmSystemNumber).limit(1).get();
  const now = Date.now();
  const maxAge = 24 * 3600 * 1000;

  if (!snap.empty) {
    const doc = snap.docs[0];
    const existing = doc.data();
    if (existing.updatedAt && (now - existing.updatedAt < maxAge)) {
      return { id: doc.id, ...existing };
    }
  }

  const freshData = await fetchFromLeroyMerlin(lmSystemNumber, 'LM');
  if (!freshData) {
    throw new functions.https.HttpsError('not-found', `Nie znaleziono produktu dla numeru LM: ${lmSystemNumber}`);
  }

  let docId = snap.empty ? `prod_${lmSystemNumber}` : snap.docs[0].id;
  const docRef = db.collection('products').doc(docId);

  if (!snap.empty) {
    const existing = snap.docs[0].data();
    if (existing.localPriceOverride) {
      freshData.localPrice = existing.localPrice;
      freshData.localPriceOverride = true;
    }
  }

  await docRef.set({ id: docId, ...freshData }, { merge: true });
  return { id: docId, ...freshData };
});

/**
 * Scheduled Cloud Function: Runs periodically (default every 6 hours)
 * Checks syncIntervalHours setting (6, 12, 24, 48, 168)
 * Updates online prices and metadata from LM without touching localPrice if localPriceOverride is active.
 */
exports.scheduledUpdateProducts = functions.pubsub.schedule('every 6 hours').onRun(async (context) => {
  console.log('Starting scheduledUpdateProducts cron job...');

  // Read settings
  const settingsDoc = await db.collection('settings').doc('general').get();
  const settings = settingsDoc.exists ? settingsDoc.data() : { syncIntervalHours: 24, lastAutoSync: 0 };
  const intervalMs = (settings.syncIntervalHours || 24) * 3600 * 1000;
  const now = Date.now();

  const productsSnap = await db.collection('products').get();
  const batch = db.batch();
  let updatedCount = 0;

  for (const doc of productsSnap.docs) {
    const prod = doc.data();
    const lastUpdate = prod.updatedAt || 0;

    if (now - lastUpdate >= intervalMs) {
      // Re-fetch online info
      const fresh = await fetchFromLeroyMerlin(prod.ean || prod.lmSystemNumber, prod.ean ? 'EAN' : 'LM');
      if (fresh) {
        const updatePayload = {
          onlinePrice: fresh.onlinePrice,
          imageUrl: fresh.imageUrl,
          name: fresh.name,
          updatedAt: now
        };
        // CRITICAL: NEVER overwrite localPrice when localPriceOverride is true!
        if (!prod.localPriceOverride) {
          updatePayload.localPrice = fresh.onlinePrice;
        }
        batch.update(doc.reference, updatePayload);
        updatedCount++;
      }
    }
  }

  if (updatedCount > 0) {
    await batch.commit();
    console.log(`Successfully updated ${updatedCount} products from Leroy Merlin catalog.`);
  }

  await db.collection('settings').doc('general').set({ lastAutoSync: now }, { merge: true });
});
