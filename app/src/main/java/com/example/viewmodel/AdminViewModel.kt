package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminRepository
import com.example.data.CatalogRepository
import com.example.data.UiState
import com.example.data.asUiState
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.SupportTicket
import com.example.model.TicketStatus
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val catalogRepository: CatalogRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _adminBanner = MutableStateFlow<String?>(null)
    val adminBanner: StateFlow<String?> = _adminBanner.asStateFlow()

    val allProductsState: StateFlow<UiState<List<Product>>> =
        catalogRepository.observeAllProductsForAdmin()
            .asUiState(TAG, "products")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val allOrdersState: StateFlow<UiState<List<Order>>> =
        adminRepository.observeAllOrders()
            .asUiState(TAG, "orders")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val allCustomersState: StateFlow<UiState<List<UserProfile>>> =
        adminRepository.observeAllCustomers()
            .asUiState(TAG, "users")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val allTicketsState: StateFlow<UiState<List<SupportTicket>>> =
        adminRepository.observeAllSupportTickets()
            .asUiState(TAG, "support_tickets")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun clearBanner() {
        _adminBanner.value = null
    }

    fun seedStarterCatalog() {
        viewModelScope.launch {
            val res = catalogRepository.seedStarterCatalogIfEmpty()
            _adminBanner.value = res.fold(
                onSuccess = { count ->
                    if (count > 0) "Seeded $count catalog products to Firestore"
                    else "Catalog already populated"
                },
                onFailure = { it.localizedMessage ?: "Failed to seed catalog" }
            )
        }
    }

    fun saveProduct(product: Product, isNew: Boolean) {
        viewModelScope.launch {
            val res = catalogRepository.saveProduct(product, isNew)
            _adminBanner.value = res.fold(
                onSuccess = { if (isNew) "Product added to store" else "Product updated" },
                onFailure = { it.localizedMessage ?: "Failed to save product" }
            )
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            val res = catalogRepository.deleteProduct(productId)
            _adminBanner.value = res.fold(
                onSuccess = { "Product removed" },
                onFailure = { it.localizedMessage ?: "Failed to delete product" }
            )
        }
    }

    fun updateOrderStatus(
        order: Order,
        newStatus: OrderStatus,
        trackingNumber: String? = null,
        estimatedDelivery: String? = null
    ) {
        viewModelScope.launch {
            val res = adminRepository.updateOrderStatus(order, newStatus, trackingNumber, estimatedDelivery)
            _adminBanner.value = res.fold(
                onSuccess = { "Order ${order.orderNumber} updated to ${newStatus.label}" },
                onFailure = { it.localizedMessage ?: "Failed to update order" }
            )
        }
    }

    fun toggleCustomerStatus(customer: UserProfile) {
        val nextStatus = if (customer.accountStatus == "active") "suspended" else "active"
        viewModelScope.launch {
            val res = adminRepository.updateCustomerStatus(customer.userId, nextStatus)
            _adminBanner.value = res.fold(
                onSuccess = { "${customer.displayName} marked $nextStatus" },
                onFailure = { it.localizedMessage ?: "Failed to update customer" }
            )
        }
    }

    fun replyToTicket(ticketId: String, reply: String, status: TicketStatus) {
        viewModelScope.launch {
            val res = adminRepository.replyToTicket(ticketId, reply, status)
            _adminBanner.value = res.fold(
                onSuccess = { "Reply sent & ticket marked ${status.label}" },
                onFailure = { it.localizedMessage ?: "Failed to send reply" }
            )
        }
    }

    private companion object {
        const val TAG = "AdminVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
