package com.example.data

import com.example.model.Product
import com.example.model.StarterCatalog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CatalogRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    /**
     * Customer catalog stream: strictly includes .whereEqualTo("active", true) matching firestore.rules.
     * If Firestore has not been seeded yet by an admin, gracefully exposes StarterCatalog read-only items
     * while keeping real-time Firestore sync active once seeded.
     */
    fun observeActiveProducts(): Flow<List<Product>> = flow {
        requireUserId()
        emitAll(
            db.collection("products")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snapshot ->
                    val live = snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (live.isEmpty()) StarterCatalog.products else live
                }
        )
    }

    /**
     * Admin catalog stream: lists all products (including inactive) for management.
     */
    fun observeAllProductsForAdmin(): Flow<List<Product>> = flow {
        requireUserId()
        emitAll(
            db.collection("products")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun seedStarterCatalogIfEmpty(): Result<Int> {
        return try {
            requireUserId()
            val existing = db.collection("products").limit(1).get().await()
            if (!existing.isEmpty) {
                return Result.success(0)
            }
            val batch = db.batch()
            StarterCatalog.products.forEach { product ->
                val docRef = db.collection("products").document(product.id)
                batch.set(docRef, product.toWriteMap(isCreate = true))
            }
            batch.commit().await()
            Result.success(StarterCatalog.products.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products")
            Result.failure(e)
        }
    }

    suspend fun saveProduct(product: Product, isNew: Boolean): Result<Unit> {
        val docId = if (isNew || product.id.isBlank()) {
            db.collection("products").document().id
        } else {
            product.id
        }
        val path = "products/$docId"
        return try {
            requireUserId()
            require(product.name.trim().length >= 2) { "Product name must be at least 2 characters." }
            require(product.price > 0.0) { "Price must be greater than zero." }
            val docRef = db.collection("products").document(docId)
            if (isNew || product.id.isBlank()) {
                docRef.set(product.toWriteMap(isCreate = true)).await()
            } else {
                docRef.update(product.toWriteMap(isCreate = false)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank()) { "Product ID must not be blank." }
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
