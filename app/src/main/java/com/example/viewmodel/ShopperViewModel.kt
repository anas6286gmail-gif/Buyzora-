package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminRepository
import com.example.data.CatalogRepository
import com.example.data.ShopperRepository
import com.example.data.UiState
import com.example.data.asUiState
import com.example.model.CartItem
import com.example.model.Order
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.SupportTicket
import com.example.model.UserProfile
import com.example.model.WishlistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProductSort(val label: String) {
    FEATURED("Featured"),
    PRICE_LOW("Price: Low-High"),
    PRICE_HIGH("Price: High-Low"),
    TOP_RATED("Top Rated"),
    BEST_DISCOUNT("Best Deals")
}

class ShopperViewModel(
    private val catalogRepository: CatalogRepository,
    private val shopperRepository: ShopperRepository,
    private val adminRepository: AdminRepository,
    private val currentUserId: String,
    private val currentUserName: String,
    private val currentUserEmail: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    val selectedCategory: StateFlow<ProductCategory> = _selectedCategory.asStateFlow()

    private val _selectedSort = MutableStateFlow(ProductSort.FEATURED)
    val selectedSort: StateFlow<ProductSort> = _selectedSort.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _lastPlacedOrder = MutableStateFlow<Order?>(null)
    val lastPlacedOrder: StateFlow<Order?> = _lastPlacedOrder.asStateFlow()

    val productsState: StateFlow<UiState<List<Product>>> =
        catalogRepository.observeActiveProducts()
            .asUiState(TAG, "products")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val filteredProducts: StateFlow<UiState<List<Product>>> = combine(
        productsState,
        _searchQuery,
        _selectedCategory,
        _selectedSort
    ) { state, query, category, sort ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val trimmed = query.trim().lowercase()
                val filtered = state.data.filter { product ->
                    val matchesCategory = category == ProductCategory.ALL ||
                        product.category.equals(category.displayName, ignoreCase = true)
                    val matchesQuery = trimmed.isEmpty() ||
                        product.name.lowercase().contains(trimmed) ||
                        product.brand.lowercase().contains(trimmed) ||
                        product.category.lowercase().contains(trimmed) ||
                        product.description.lowercase().contains(trimmed)
                    matchesCategory && matchesQuery
                }
                val sorted = when (sort) {
                    ProductSort.FEATURED -> filtered.sortedWith(
                        compareByDescending<Product> { it.featured }.thenByDescending { it.rating }
                    )
                    ProductSort.PRICE_LOW -> filtered.sortedBy { it.price }
                    ProductSort.PRICE_HIGH -> filtered.sortedByDescending { it.price }
                    ProductSort.TOP_RATED -> filtered.sortedByDescending { it.rating }
                    ProductSort.BEST_DISCOUNT -> filtered.sortedByDescending { it.discountPercent }
                }
                UiState.Success(sorted)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        shopperRepository.observeUserProfile(currentUserId)
            .asUiState(TAG, "users/$currentUserId")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val cartState: StateFlow<UiState<List<CartItem>>> =
        shopperRepository.observeCart(currentUserId)
            .asUiState(TAG, "users/$currentUserId/cart")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val wishlistState: StateFlow<UiState<List<WishlistItem>>> =
        shopperRepository.observeWishlist(currentUserId)
            .asUiState(TAG, "users/$currentUserId/wishlist")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val ordersState: StateFlow<UiState<List<Order>>> =
        shopperRepository.observeOrders(currentUserId)
            .asUiState(TAG, "orders")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val ticketsState: StateFlow<UiState<List<SupportTicket>>> =
        shopperRepository.observeSupportTickets(currentUserId)
            .asUiState(TAG, "support_tickets")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    init {
        viewModelScope.launch {
            shopperRepository.ensureUserProfile()
            val admin = adminRepository.checkIsAdmin()
            _isAdmin.value = admin
            if (admin) {
                catalogRepository.seedStarterCatalogIfEmpty()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: ProductCategory) {
        _selectedCategory.value = category
    }

    fun selectSort(sort: ProductSort) {
        _selectedSort.value = sort
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun clearLastPlacedOrder() {
        _lastPlacedOrder.value = null
    }

    fun addToCart(product: Product, variant: String? = null) {
        viewModelScope.launch {
            val result = shopperRepository.addToCart(currentUserId, product, variant)
            _statusBanner.value = result.fold(
                onSuccess = { "${product.name} added to bag" },
                onFailure = { it.localizedMessage ?: "Failed to add item to bag" }
            )
        }
    }

    fun updateCartQuantity(cartItemId: String, newQuantity: Long) {
        viewModelScope.launch {
            val result = shopperRepository.updateCartQuantity(currentUserId, cartItemId, newQuantity)
            result.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not update quantity"
            }
        }
    }

    fun removeFromCart(cartItemId: String) {
        viewModelScope.launch {
            val result = shopperRepository.removeFromCart(currentUserId, cartItemId)
            result.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not remove item"
            }
        }
    }

    fun toggleWishlist(product: Product, isCurrentlyWishlisted: Boolean) {
        viewModelScope.launch {
            val result = shopperRepository.toggleWishlist(currentUserId, product, isCurrentlyWishlisted)
            _statusBanner.value = result.fold(
                onSuccess = {
                    if (isCurrentlyWishlisted) "Removed from Wishlist" else "Saved to Wishlist"
                },
                onFailure = { it.localizedMessage ?: "Wishlist update failed" }
            )
        }
    }

    fun removeWishlistItem(item: WishlistItem) {
        val proxyProduct = Product(
            id = item.productId,
            name = item.title,
            brand = item.brand,
            category = item.category,
            price = item.price,
            originalPrice = item.originalPrice,
            discountPercent = item.discountPercent,
            rating = item.rating,
            imageKey = item.imageKey,
            imageUrl = item.imageUrl
        )
        toggleWishlist(proxyProduct, isCurrentlyWishlisted = true)
    }

    fun moveWishlistToCart(item: WishlistItem) {
        val proxyProduct = Product(
            id = item.productId,
            name = item.title,
            brand = item.brand,
            category = item.category,
            price = item.price,
            originalPrice = item.originalPrice,
            discountPercent = item.discountPercent,
            rating = item.rating,
            imageKey = item.imageKey,
            imageUrl = item.imageUrl
        )
        viewModelScope.launch {
            val addResult = shopperRepository.addToCart(currentUserId, proxyProduct, null)
            if (addResult.isSuccess) {
                shopperRepository.toggleWishlist(currentUserId, proxyProduct, currentlyWishlisted = true)
                _statusBanner.value = "${item.title} moved to bag"
            } else {
                _statusBanner.value = addResult.exceptionOrNull()?.localizedMessage ?: "Failed to move item"
            }
        }
    }

    fun placeOrder(
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        paymentDetails: String,
        shippingAddress: String,
        shippingPhone: String,
        promoDiscount: Double
    ) {
        viewModelScope.launch {
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?.takeIf { it.isNotBlank() } ?: currentUserName
            val result = shopperRepository.placeOrder(
                userId = currentUserId,
                customerName = profileName,
                customerEmail = currentUserEmail,
                cartItems = cartItems,
                paymentMethod = paymentMethod,
                paymentDetails = paymentDetails,
                shippingAddress = shippingAddress,
                shippingPhone = shippingPhone,
                promoDiscount = promoDiscount
            )
            result.fold(
                onSuccess = { order ->
                    _lastPlacedOrder.value = order
                    _statusBanner.value = "Order ${order.orderNumber} placed successfully!"
                },
                onFailure = {
                    _statusBanner.value = it.localizedMessage ?: "Checkout failed"
                }
            )
        }
    }

    fun cancelOrder(order: Order, reason: String) {
        viewModelScope.launch {
            val result = shopperRepository.cancelOrder(currentUserId, order, reason)
            _statusBanner.value = result.fold(
                onSuccess = { "Order ${order.orderNumber} cancelled" },
                onFailure = { it.localizedMessage ?: "Could not cancel order" }
            )
        }
    }

    fun requestReturn(order: Order, reason: String) {
        viewModelScope.launch {
            val result = shopperRepository.requestOrderReturn(currentUserId, order, reason)
            _statusBanner.value = result.fold(
                onSuccess = { "Return & refund initiated for ${order.orderNumber}" },
                onFailure = { it.localizedMessage ?: "Could not submit return request" }
            )
        }
    }

    fun updateProfile(
        displayName: String,
        phone: String,
        address: String,
        city: String,
        postalCode: String,
        marketingOptIn: Boolean,
        twoFactorReminder: Boolean
    ) {
        viewModelScope.launch {
            val result = shopperRepository.updateProfile(
                userId = currentUserId,
                displayName = displayName,
                phone = phone,
                defaultAddress = address,
                city = city,
                postalCode = postalCode,
                privacyMarketingOptIn = marketingOptIn,
                twoFactorReminderEnabled = twoFactorReminder
            )
            _statusBanner.value = result.fold(
                onSuccess = { "Profile & privacy preferences saved" },
                onFailure = { it.localizedMessage ?: "Failed to update profile" }
            )
        }
    }

    fun submitSupportTicket(
        topic: String,
        subject: String,
        message: String,
        orderNumber: String?
    ) {
        viewModelScope.launch {
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?.takeIf { it.isNotBlank() } ?: currentUserName
            val result = shopperRepository.submitSupportTicket(
                userId = currentUserId,
                customerName = profileName,
                customerEmail = currentUserEmail,
                topic = topic,
                subject = subject,
                message = message,
                orderNumber = orderNumber
            )
            _statusBanner.value = result.fold(
                onSuccess = { "Support ticket submitted. Our team will respond shortly." },
                onFailure = { it.localizedMessage ?: "Failed to submit ticket" }
            )
        }
    }

    private companion object {
        const val TAG = "ShopperVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
