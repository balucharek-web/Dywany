const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SUPER_ADMIN_EMAIL = "baluch.arek@gmail.com";
const SUPER_ADMIN_UID = "super_admin_arek";
const ADMIN_UID = "admin_jan";
const USER_UID = "user_adam";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();

    // Pre-seed an admin and super-admin user in admin context
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection("users").doc(SUPER_ADMIN_UID).set({
        userId: SUPER_ADMIN_UID,
        email: SUPER_ADMIN_EMAIL,
        role: "SUPER_ADMIN",
        active: true,
      });
      await db.collection("users").doc(ADMIN_UID).set({
        userId: ADMIN_UID,
        email: "jan.kowalski@leroy.pl",
        role: "ADMIN",
        active: true,
      });
      await db.collection("users").doc(USER_UID).set({
        userId: USER_UID,
        email: "adam.nowak@gmail.com",
        role: "USER",
        active: true,
      });
    });
  }
});

test("Public access: unauthenticated user can read products, poles, and assignments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("products").get());
  await assertSucceeds(unauthDb.collection("poles").get());
  await assertSucceeds(unauthDb.collection("displayAssignments").get());
  await assertSucceeds(unauthDb.collection("settings").doc("config").get());
});

test("Public access: unauthenticated user cannot write products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").doc("p1").set({
    productId: "p1",
    name: "Dywan Agnella",
    ean: "5901234567890",
    lmSystemNumber: "12345678"
  }));
});

test("Public access: unauthenticated user cannot read users or auditLogs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("auditLogs").get());
});

test("Regular USER: cannot write products or modify pole assignments", async () => {
  const userDb = testEnv.authenticatedContext(USER_UID, { email: "adam.nowak@gmail.com" }).firestore();
  await assertFails(userDb.collection("products").doc("p2").set({
    productId: "p2",
    name: "Dywan Berbère",
    ean: "5909876543210",
    lmSystemNumber: "87654321"
  }));
  await assertFails(userDb.collection("displayAssignments").doc("pos_1_A").set({
    assignmentId: "pos_1_A",
    poleNumber: 1,
    position: "A"
  }));
});

test("Regular USER: cannot elevate role to ADMIN", async () => {
  const userDb = testEnv.authenticatedContext(USER_UID, { email: "adam.nowak@gmail.com" }).firestore();
  await assertFails(userDb.collection("users").doc(USER_UID).update({
    role: "ADMIN"
  }));
});

test("ADMIN: can create products, manage poles and display assignments", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "jan.kowalski@leroy.pl" }).firestore();
  await assertSucceeds(adminDb.collection("products").doc("p1").set({
    productId: "p1",
    name: "Dywan Agnella Eco",
    ean: "5901234567890",
    lmSystemNumber: "12345678"
  }));
  await assertSucceeds(adminDb.collection("poles").doc("pole_1").set({
    poleId: "pole_1",
    number: 1
  }));
  await assertSucceeds(adminDb.collection("displayAssignments").doc("pos_1_A").set({
    assignmentId: "pos_1_A",
    poleNumber: 1,
    position: "A",
    productId: "p1",
    price: 199.0
  }));
});

test("SUPER_ADMIN (baluch.arek@gmail.com): can manage users and roles", async () => {
  const superAdminDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL }).firestore();
  await assertSucceeds(superAdminDb.collection("users").doc("new_admin").set({
    userId: "new_admin",
    email: "nowy.admin@leroy.pl",
    role: "ADMIN",
    active: true
  }));
});
