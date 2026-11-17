package com.example.data

import com.example.model.CartItem
import com.example.model.Order
import com.example.model.OrderItem
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.Product
import com.example.model.SupportTicket
import com.example.model.UserProfile
import com.example.model.WishlistItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt
import kotlin.random.Random

class ShopperRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    suspend fun ensureUserProfile(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/${user.uid}"
        return try {
            val docRef = db.collection("users").document(user.uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val profile = UserProfile(
                    userId = user.uid,
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Buyzora Shopper",
                    email = user.email ?: "shopper@buyzora.app"
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        phone: String?,
        defaultAddress: String?,
        city: String?,
        postalCode: String?,
        privacyMarketingOptIn: Boolean,
        twoFactorReminderEnabled: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user profile update." }
            require(displayName.trim().isNotEmpty()) { "Name cannot be empty." }
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName.trim().take(80),
                "privacyMarketingOptIn" to privacyMarketingOptIn,
                "twoFactorReminderEnabled" to twoFactorReminderEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            updates["phone"] = (phone?.trim() ?: "").take(25)
            updates["defaultAddress"] = (defaultAddress?.trim() ?: "").take(300)
            updates["city"] = (city?.trim() ?: "").take(80)
            updates["postalCode"] = (postalCode?.trim() ?: "").take(20)

            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId).collection("cart")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CartItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        )
    }

    suspend fun addToCart(userId: String, product: Product, variant: String?): Result<Unit> {
        val safeVariant = variant?.takeIf { it.isNotBlank() } ?: product.defaultVariants().firstOrNull()
        val cartDocId = buildString {
            append(product.id.replace(Regex("[^a-zA-Z0-9_-]"), "_"))
            if (!safeVariant.isNullOrBlank()) {
                append("_")
                append(safeVariant.replace(Regex("[^a-zA-Z0-9_-]"), ""))
            }
        }.take(120)
        val path = "users/$userId/cart/$cartDocId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user cart write." }
            val docRef = db.collection("users").document(userId).collection("cart").document(cartDocId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val currentQty = existing.getLong("quantity") ?: 1L
                val nextQty = (currentQty + 1L).coerceAtMost(99L)
                docRef.update(
                    mapOf(
                        "quantity" to nextQty,
                        "price" to product.price,
                        "originalPrice" to product.originalPrice,
                        "discountPercent" to product.discountPercent,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val cartItem = CartItem(
                    productId = product.id,
                    title = product.name,
                    brand = product.brand,
                    category = product.category,
                    price = product.price,
                    originalPrice = product.originalPrice,
                    discountPercent = product.discountPercent,
                    imageKey = product.imageKey,
                    imageUrl = product.imageUrl,
                    quantity = 1L,
                    selectedVariant = safeVariant
                )
                docRef.set(cartItem.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(userId: String, cartItemId: String, newQuantity: Long): Result<Unit> {
        val path = "users/$userId/cart/$cartItemId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(userId).collection("cart").document(cartItemId)
            if (newQuantity <= 0L) {
                docRef.delete().await()
            } else {
                docRef.update(
                    mapOf(
                        "quantity" to newQuantity.coerceIn(1L, 99L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(userId: String, cartItemId: String): Result<Unit> {
        val path = "users/$userId/cart/$cartItemId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(userId).collection("cart").document(cartItemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId).collection("wishlist")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WishlistItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        )
    }

    suspend fun toggleWishlist(userId: String, product: Product, currentlyWishlisted: Boolean): Result<Unit> {
        val safeProductId = product.id.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(120)
        val path = "users/$userId/wishlist/$safeProductId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(userId).collection("wishlist").document(safeProductId)
            if (currentlyWishlisted) {
                docRef.delete().await()
            } else {
                val item = WishlistItem(
                    productId = product.id,
                    title = product.name,
                    brand = product.brand,
                    category = product.category,
                    price = product.price,
                    originalPrice = product.originalPrice,
                    discountPercent = product.discountPercent,
                    rating = product.rating,
                    imageKey = product.imageKey,
                    imageUrl = product.imageUrl
                )
                docRef.set(item.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeOrders(userId: String): Flow<List<Order>> = flow {
        requireUserId()
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        )
    }

    suspend fun placeOrder(
        userId: String,
        customerName: String,
        customerEmail: String,
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        paymentDetails: String,
        shippingAddress: String,
        shippingPhone: String,
        promoDiscount: Double
    ): Result<Order> {
        val docRef = db.collection("orders").document()
        val path = docRef.path
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(cartItems.isNotEmpty()) { "Cart cannot be empty." }
            require(shippingAddress.trim().length >= 5) { "Please enter a valid delivery address." }
            require(shippingPhone.trim().length >= 6) { "Please enter a valid contact phone number." }

            val subtotalRaw = cartItems.sumOf { it.price * it.quantity }
            val subtotal = (subtotalRaw * 100.0).roundToInt() / 100.0
            val discount = promoDiscount.coerceIn(0.0, subtotal)
            val shippingFee = if ((subtotal - discount) >= 50.0) 0.0 else 4.99
            val total = ((subtotal - discount + shippingFee) * 100.0).roundToInt() / 100.0

            val orderNumber = "BZ-${Random.nextInt(100000, 999999)}"
            val trackingNumber = "TRK-BZ-${Random.nextInt(10000, 99999)}"
            val paymentStatus = if (paymentMethod == PaymentMethod.COD) {
                PaymentStatus.COD_PENDING.wireValue
            } else {
                PaymentStatus.PAID.wireValue
            }

            val orderItems = cartItems.take(20).map {
                OrderItem(
                    productId = it.productId,
                    title = it.title,
                    price = it.price,
                    quantity = it.quantity,
                    imageKey = it.imageKey,
                    selectedVariant = it.selectedVariant
                )
            }

            val order = Order(
                id = docRef.id,
                userId = userId,
                customerName = customerName,
                customerEmail = customerEmail,
                orderNumber = orderNumber,
                items = orderItems,
                subtotal = subtotal,
                discountAmount = discount,
                shippingFee = shippingFee,
                totalAmount = total,
                paymentMethod = paymentMethod.wireValue,
                paymentDetails = paymentDetails,
                paymentStatus = paymentStatus,
                shippingAddress = shippingAddress.trim(),
                shippingPhone = shippingPhone.trim(),
                status = OrderStatus.PLACED.wireValue,
                trackingNumber = trackingNumber,
                estimatedDelivery = "Arriving in 2-4 business days"
            )

            val batch = db.batch()
            batch.set(docRef, order.toCreateMap())
            cartItems.forEach { item ->
                if (item.id.isNotBlank()) {
                    val cartRef = db.collection("users").document(userId).collection("cart").document(item.id)
                    batch.delete(cartRef)
                }
            }
            batch.commit().await()
            Result.success(order)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(userId: String, order: Order, reason: String): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId && order.userId == uid)
            val nextPaymentStatus = if (order.paymentStatus == PaymentStatus.PAID.wireValue) {
                PaymentStatus.REFUND_INITIATED.wireValue
            } else {
                order.paymentStatus
            }
            val updates = buildMap<String, Any> {
                put("status", OrderStatus.CANCELLED.wireValue)
                put("paymentStatus", nextPaymentStatus)
                put("returnReason", reason.ifBlank { "Cancelled by customer" }.take(300))
                if (order.paymentStatus == PaymentStatus.PAID.wireValue) {
                    put("refundAmount", order.totalAmount)
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("orders").document(order.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun requestOrderReturn(userId: String, order: Order, reason: String): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId && order.userId == uid)
            require(reason.trim().length >= 3) { "Please provide a brief return reason (min 3 characters)." }
            val nextPaymentStatus = if (order.paymentStatus == PaymentStatus.PAID.wireValue) {
                PaymentStatus.REFUND_INITIATED.wireValue
            } else {
                order.paymentStatus
            }
            val updates = mapOf(
                "status" to OrderStatus.RETURN_REQUESTED.wireValue,
                "paymentStatus" to nextPaymentStatus,
                "returnReason" to reason.trim().take(300),
                "refundAmount" to order.totalAmount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("orders").document(order.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeSupportTickets(userId: String): Flow<List<SupportTicket>> = flow {
        requireUserId()
        emitAll(
            db.collection("support_tickets")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupportTicket::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        )
    }

    suspend fun submitSupportTicket(
        userId: String,
        customerName: String,
        customerEmail: String,
        topic: String,
        subject: String,
        message: String,
        orderNumber: String?
    ): Result<Unit> {
        val docRef = db.collection("support_tickets").document()
        val path = docRef.path
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(subject.trim().length >= 3) { "Subject must be at least 3 characters." }
            require(message.trim().length >= 5) { "Message must be at least 5 characters." }
            val ticket = SupportTicket(
                userId = userId,
                customerName = customerName,
                customerEmail = customerEmail,
                topic = topic,
                subject = subject,
                message = message,
                orderNumber = orderNumber
            )
            docRef.set(ticket.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
