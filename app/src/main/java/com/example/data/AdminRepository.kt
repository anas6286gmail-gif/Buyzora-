package com.example.data

import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.PaymentStatus
import com.example.model.SupportTicket
import com.example.model.TicketStatus
import com.example.model.UserProfile
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

private const val BOOTSTRAP_ADMIN_EMAIL = "anashr413@gmail.com"

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    suspend fun checkIsAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified) {
            return true
        }
        return try {
            db.collection("admins").document(user.uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    fun observeAllOrders(): Flow<List<Order>> = flow {
        requireUserId()
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        )
    }

    fun observeAllCustomers(): Flow<List<UserProfile>> = flow {
        requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        )
    }

    fun observeAllSupportTickets(): Flow<List<SupportTicket>> = flow {
        requireUserId()
        emitAll(
            db.collection("support_tickets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupportTicket::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        )
    }

    suspend fun updateOrderStatus(
        order: Order,
        newStatus: OrderStatus,
        trackingNumber: String? = null,
        estimatedDelivery: String? = null
    ): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            requireUserId()
            val nextPaymentStatus = when (newStatus) {
                OrderStatus.REFUNDED -> PaymentStatus.REFUNDED.wireValue
                OrderStatus.CANCELLED, OrderStatus.RETURN_REQUESTED -> {
                    if (order.paymentStatus == PaymentStatus.PAID.wireValue) {
                        PaymentStatus.REFUND_INITIATED.wireValue
                    } else order.paymentStatus
                }
                else -> order.paymentStatus
            }
            val updates = buildMap<String, Any> {
                put("status", newStatus.wireValue)
                put("paymentStatus", nextPaymentStatus)
                trackingNumber?.takeIf { it.isNotBlank() }?.let { put("trackingNumber", it.take(60)) }
                estimatedDelivery?.takeIf { it.isNotBlank() }?.let { put("estimatedDelivery", it.take(80)) }
                if (newStatus == OrderStatus.REFUNDED) {
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

    suspend fun updateCustomerStatus(userId: String, accountStatus: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId()
            db.collection("users").document(userId).update(
                mapOf(
                    "accountStatus" to accountStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun replyToTicket(ticketId: String, reply: String, status: TicketStatus): Result<Unit> {
        val path = "support_tickets/$ticketId"
        return try {
            requireUserId()
            require(reply.trim().isNotEmpty()) { "Reply cannot be empty." }
            db.collection("support_tickets").document(ticketId).update(
                mapOf(
                    "adminReply" to reply.trim().take(1000),
                    "status" to status.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
