const admin = require("firebase-admin");

/**
 * Serwis odświeżania i weryfikacji danych produktów Leroy Merlin
 */
async function syncProductData(productId) {
  const db = admin.firestore();
  const productRef = db.collection("dywany").doc(productId);
  const doc = await productRef.get();

  if (!doc.exists) {
    return { success: false, message: "Produkt nie istnieje" };
  }

  const data = doc.data();
  // Symulacja pobrania aktualnej ceny i dostępności z API Leroy Merlin
  const currentPrice = data.price || 299.0;
  
  // Zapisz historię ceny
  await db.collection("product_price_history").add({
    lmNumber: data.lmNumber,
    price: currentPrice,
    date: admin.firestore.FieldValue.serverTimestamp()
  });

  await productRef.update({
    lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
    status: "AVAILABLE"
  });

  return { success: true, updatedPrice: currentPrice };
}

module.exports = {
  syncProductData
};
