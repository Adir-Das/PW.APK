const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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
  }
});

test("Unauthenticated user: cannot read workspaces", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("workspaces").doc("ws_1").get());
});

test("Authenticated user: can create workspace", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("workspaces").doc("ws_1").set({
      name: "My Personal Wallet",
      ownerId: ALICE_UID,
      currency: "BDT",
      description: "Default workspace",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Non-member: cannot read workspace", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("workspaces").doc("ws_1").set({
      name: "Alice Wallet",
      ownerId: ALICE_UID,
      currency: "BDT",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("workspaces").doc("ws_1").get());
});

test("Owner: can add account and transaction", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("workspaces").doc("ws_1").set({
      name: "Alice Wallet",
      ownerId: ALICE_UID,
      currency: "BDT",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("workspaces").doc("ws_1").collection("accounts").doc("acc_1").set({
      workspaceId: "ws_1",
      name: "Dutch-Bangla Bank",
      type: "BANK",
      openingBalance: 20000,
      currentBalance: 20000,
      status: "ACTIVE",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("workspaces").doc("ws_1").collection("transactions").doc("tx_1").set({
      workspaceId: "ws_1",
      type: "INCOME",
      title: "Salary November",
      amount: 50000,
      accountId: "acc_1",
      date: "2026-11-08",
      createdBy: ALICE_UID,
      createdAt: new Date(),
    })
  );
});
