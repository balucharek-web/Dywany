const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { syncProductData } = require("./leroyProductService");

admin.initializeApp();

/**
 * Cykliczne zadanie Cloud Functions: odświeżanie danych produktów
 */
exports.scheduledProductSync = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async (context) => {
    const db = admin.firestore();
    const snapshot = await db.collection("dywany").get();

    for (const doc of snapshot.docs) {
      try {
        await syncProductData(doc.id);
      } catch (e) {
        console.error(`Błąd synchronizacji produktu ${doc.id}:`, e);
      }
    }
    return null;
  });

/**
 * Wyzwalacz: automatyczny zapis do historii przy zmianie stanu pałąka
 */
exports.onPalekUpdated = functions.firestore
  .document("palki/{palekId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    // Wykryj zmianę
    if (JSON.stringify(before.slotA) !== JSON.stringify(after.slotA) ||
        JSON.stringify(before.slotB) !== JSON.stringify(after.slotB)) {
      console.log(`Zaktualizowano pałąk ${after.number} przez ${after.updatedBy}`);
    }
    return null;
  });
