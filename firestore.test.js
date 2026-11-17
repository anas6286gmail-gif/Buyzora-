const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_UID = "admin_uid_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProductPayload(overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    name: "Organic Linen Resort Shirt",
    brand: "Atelier Loom",
    category: "Clothes",
    description: "Breathable 100% European flax linen shirt tailored for everyday comfort.",
    price: 49.99,
    originalPrice: 69.99,
    discountPercent: 29,
    rating: 4.8,
    reviewCount: 142,
    stock: 35,
    imageKey: "img_cat_fashion",
    featured: true,
    active: true,
    highlights: ["100% Flax Linen", "Pre-washed softness"],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validUserPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Alice Shopper",
    email: "alice@example.com",
    role: "customer",
    accountStatus: "active",
    privacyMarketingOptIn: false,
    twoFactorReminderEnabled: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validOrderPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    customerName: "Alice Shopper",
    customerEmail: "alice@example.com",
    orderNumber: "BZ-20261117-1001",
    items: [
      {
        productId: "prod_shirt_1",
        title: "Organic Linen Resort Shirt",
        price: 49.99,
        quantity: 1,
        imageKey: "img_cat_fashion",
        selectedVariant: "M",
      },
    ],
    subtotal: 49.99,
    discountAmount: 20.0,
    shippingFee: 0.0,
    totalAmount: 49.99,
    paymentMethod: "UPI",
    paymentDetails: "UPI: alice***@oksbi",
    paymentStatus: "PAID",
    shippingAddress: "221B Oakwood Avenue, Bengaluru",
    shippingPhone: "+91 9876543210",
    status: "PLACED",
    trackingNumber: "TRK-BZ-88412",
    estimatedDelivery: "Nov 20 - Nov 22",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").where("active", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("support_tickets").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
});

test("User profile isolation, role escalation prevention, and PII protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  // Self-assigned admin role must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, { role: "admin" }))
  );

  // Valid customer profile creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  // Owner can update allowed shipping & privacy fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phone: "+91 9988776655",
      defaultAddress: "42 Park Lane",
      city: "Mumbai",
      postalCode: "400001",
      privacyMarketingOptIn: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner cannot escalate role or accountStatus
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read & list must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Cart & Wishlist subcollections enforce owner-only access and bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("item_1").set({
      productId: "prod_1",
      title: "Hydra Boost Face Wash",
      brand: "PureBotanica",
      category: "Face Washes",
      price: 14.5,
      originalPrice: 19.0,
      discountPercent: 24,
      imageKey: "img_cat_beauty",
      quantity: 2,
      selectedVariant: "150ml",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Quantity > 99 rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("item_1").update({
      quantity: 120,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Wishlist add & list
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("prod_1").set({
      productId: "prod_1",
      title: "Hydra Boost Face Wash",
      brand: "PureBotanica",
      category: "Face Washes",
      price: 14.5,
      originalPrice: 19.0,
      discountPercent: 24,
      rating: 4.7,
      imageKey: "img_cat_beauty",
      createdAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("wishlist").get());

  // Cross-user cart/wishlist access rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("prod_1").get());
});

test("Products catalog enforces active filter for shoppers and verified admin for mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", { email: "anashr413@gmail.com", email_verified: false }).firestore();
  const verifiedAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "anashr413@gmail.com", email_verified: true }).firestore();

  // Non-admin & unverified-email spoofed admin cannot create products
  await assertFails(aliceDb.collection("products").doc("prod_1").set(validProductPayload()));
  await assertFails(spoofedAdminDb.collection("products").doc("prod_1").set(validProductPayload()));

  // Verified admin creates active and inactive products
  await assertSucceeds(verifiedAdminDb.collection("products").doc("prod_1").set(validProductPayload()));
  await assertSucceeds(
    verifiedAdminDb.collection("products").doc("prod_hidden").set(validProductPayload({ active: false }))
  );

  // Customer query with active == true succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("products").where("active", "==", true).get());
  await assertFails(aliceDb.collection("products").get());
  await assertFails(aliceDb.collection("products").doc("prod_hidden").get());

  // Admin can query all products and update price
  await assertSucceeds(verifiedAdminDb.collection("products").get());
  await assertSucceeds(
    verifiedAdminDb.collection("products").doc("prod_1").update({
      price: 44.99,
      discountPercent: 36,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Orders enforce ownership, filtered queries, cancel/return transitions, and admin fulfillment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "anashr413@gmail.com", email_verified: true }).firestore();

  // Alice creates order
  await assertSucceeds(aliceDb.collection("orders").doc("ord_1").set(validOrderPayload(ALICE_UID)));

  // Filtered query succeeds, unfiltered query fails for customer
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("orders").get());

  // Bob cannot read or cancel Alice's order
  await assertFails(bobDb.collection("orders").doc("ord_1").get());
  await assertFails(
    bobDb.collection("orders").doc("ord_1").update({
      status: "CANCELLED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice cannot mark her own order DELIVERED
  await assertFails(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "DELIVERED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice cancels PLACED order with refund initiated
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "CANCELLED",
      paymentStatus: "REFUND_INITIATED",
      returnReason: "Ordered wrong size",
      refundAmount: 49.99,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: Alice cannot modify CANCELLED order again
  await assertFails(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "RETURN_REQUESTED",
      returnReason: "Retry",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Order 2: Admin marks DELIVERED, Alice requests return
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_2").set(validOrderPayload(ALICE_UID, { orderNumber: "BZ-20261117-1002" }))
  );
  await assertSucceeds(
    adminDb.collection("orders").doc("ord_2").update({
      status: "DELIVERED",
      trackingNumber: "TRK-BZ-99999",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_2").update({
      status: "RETURN_REQUESTED",
      paymentStatus: "REFUND_INITIATED",
      returnReason: "Item fit slightly small",
      refundAmount: 49.99,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    adminDb.collection("orders").doc("ord_2").update({
      status: "REFUNDED",
      paymentStatus: "REFUNDED",
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Support tickets enforce ownership, prevent forged admin replies, and allow admin resolution", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "anashr413@gmail.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 1000);

  // Customer cannot create ticket with pre-populated adminReply
  await assertFails(
    aliceDb.collection("support_tickets").doc("tkt_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      customerEmail: "alice@example.com",
      topic: "Payment & Refund",
      subject: "Refund status",
      message: "Checking when refund arrives",
      status: "OPEN",
      adminReply: "Forged reply",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("support_tickets").doc("tkt_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      customerEmail: "alice@example.com",
      topic: "Payment & Refund",
      subject: "Refund status",
      message: "Checking when my UPI refund arrives",
      orderNumber: "BZ-20261117-1001",
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("support_tickets").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("support_tickets").get());
  await assertFails(bobDb.collection("support_tickets").doc("tkt_1").get());

  // Admin replies & resolves ticket
  await assertSucceeds(
    adminDb.collection("support_tickets").doc("tkt_1").update({
      status: "RESOLVED",
      adminReply: "Your refund has been processed and will reflect within 24 hours.",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
