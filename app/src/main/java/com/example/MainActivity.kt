package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AdminRepository
import com.example.data.CatalogRepository
import com.example.data.ShopperRepository
import com.example.data.UiState
import com.example.data.authStateFlow
import com.example.data.signOut
import com.example.model.StarterCatalog
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.HomeShopScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.SupportPrivacyScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.BuyzoraTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.ShopperViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object BuyzoraRoutes {
    const val SHOP = "shop"
    const val BAG = "bag"
    const val ORDERS = "orders"
    const val ACCOUNT = "account"
    const val WISHLIST = "wishlist"
    const val ADMIN = "admin"
    const val PRODUCT_DETAIL = "product/{productId}"

    fun productDetail(productId: String): String = "product/$productId"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomTabs = listOf(
    BottomNavTab(BuyzoraRoutes.SHOP, "Shop", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    BottomNavTab(BuyzoraRoutes.BAG, "Bag", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
    BottomNavTab(BuyzoraRoutes.ORDERS, "Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    BottomNavTab(BuyzoraRoutes.ACCOUNT, "Account", Icons.Filled.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuyzoraTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateFlow updates automatically */ })
    } else {
        key(user.uid) {
            BuyzoraAuthenticatedShell(
                currentUserId = user.uid,
                currentUserName = user.displayName ?: user.email?.substringBefore("@") ?: "Buyzora Shopper",
                currentUserEmail = user.email ?: "shopper@buyzora.app"
            )
        }
    }
}

@Composable
private fun BuyzoraAuthenticatedShell(
    currentUserId: String,
    currentUserName: String,
    currentUserEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val shopperViewModel: ShopperViewModel = viewModel(
        key = "shopper_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ShopperViewModel(
                    catalogRepository = CatalogRepository(db),
                    shopperRepository = ShopperRepository(db),
                    adminRepository = AdminRepository(db),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    currentUserEmail = currentUserEmail
                )
            }
        }
    )

    val filteredProductsState by shopperViewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProductsState by shopperViewModel.productsState.collectAsStateWithLifecycle()
    val searchQuery by shopperViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by shopperViewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSort by shopperViewModel.selectedSort.collectAsStateWithLifecycle()
    val cartState by shopperViewModel.cartState.collectAsStateWithLifecycle()
    val wishlistState by shopperViewModel.wishlistState.collectAsStateWithLifecycle()
    val ordersState by shopperViewModel.ordersState.collectAsStateWithLifecycle()
    val ticketsState by shopperViewModel.ticketsState.collectAsStateWithLifecycle()
    val profileState by shopperViewModel.userProfileState.collectAsStateWithLifecycle()
    val isAdmin by shopperViewModel.isAdmin.collectAsStateWithLifecycle()
    val statusBanner by shopperViewModel.statusBanner.collectAsStateWithLifecycle()
    val lastPlacedOrder by shopperViewModel.lastPlacedOrder.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            shopperViewModel.clearStatusBanner()
        }
    }

    LaunchedEffect(lastPlacedOrder) {
        if (lastPlacedOrder != null) {
            shopperViewModel.clearLastPlacedOrder()
            navController.navigate(BuyzoraRoutes.ORDERS) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val cartItemsCount = ((cartState as? UiState.Success)?.data?.sumOf { it.quantity } ?: 0L).toInt()
    val wishlistItems = (wishlistState as? UiState.Success)?.data.orEmpty()
    val wishlistedIds = remember(wishlistItems) { wishlistItems.map { it.productId }.toSet() }
    val showTopLevelNav = currentRoute in bottomTabs.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (tab.route == BuyzoraRoutes.BAG && cartItemsCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartItemsCount.toString()) } }) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (tab.route == BuyzoraRoutes.BAG && cartItemsCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartItemsCount.toString()) } }) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = BuyzoraRoutes.SHOP
                    ) {
                        composable(BuyzoraRoutes.SHOP) {
                            HomeShopScreen(
                                productsState = filteredProductsState,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                selectedSort = selectedSort,
                                wishlistedIds = wishlistedIds,
                                wishlistCount = wishlistItems.size,
                                onSearchQueryChange = shopperViewModel::updateSearchQuery,
                                onSelectCategory = shopperViewModel::selectCategory,
                                onSelectSort = shopperViewModel::selectSort,
                                onProductClick = { product ->
                                    navController.navigate(BuyzoraRoutes.productDetail(product.id))
                                },
                                onToggleWishlist = shopperViewModel::toggleWishlist,
                                onQuickAdd = { product -> shopperViewModel.addToCart(product) },
                                onOpenWishlist = { navController.navigate(BuyzoraRoutes.WISHLIST) }
                            )
                        }

                        composable(
                            route = BuyzoraRoutes.PRODUCT_DETAIL,
                            arguments = listOf(navArgument("productId") { type = NavType.StringType })
                        ) { entry ->
                            val productId = entry.arguments?.getString("productId").orEmpty()
                            val catalogList = (allProductsState as? UiState.Success)?.data
                                ?: StarterCatalog.products
                            val product = catalogList.firstOrNull { it.id == productId }
                                ?: StarterCatalog.products.firstOrNull { it.id == productId }
                                ?: catalogList.first()
                            val isWishlisted = wishlistedIds.contains(product.id)

                            ProductDetailScreen(
                                product = product,
                                isWishlisted = isWishlisted,
                                onBack = { navController.popBackStack() },
                                onToggleWishlist = { shopperViewModel.toggleWishlist(product, isWishlisted) },
                                onAddToCart = { variant -> shopperViewModel.addToCart(product, variant) },
                                onBuyNow = { variant ->
                                    shopperViewModel.addToCart(product, variant)
                                    navController.navigate(BuyzoraRoutes.BAG) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable(BuyzoraRoutes.WISHLIST) {
                            WishlistScreen(
                                wishlistState = wishlistState,
                                onBack = { navController.popBackStack() },
                                onMoveToCart = shopperViewModel::moveWishlistToCart,
                                onRemove = shopperViewModel::removeWishlistItem,
                                onOpenProduct = { productId ->
                                    navController.navigate(BuyzoraRoutes.productDetail(productId))
                                }
                            )
                        }

                        composable(BuyzoraRoutes.BAG) {
                            CartCheckoutScreen(
                                cartState = cartState,
                                profileState = profileState,
                                onUpdateQuantity = shopperViewModel::updateCartQuantity,
                                onRemoveItem = shopperViewModel::removeFromCart,
                                onPlaceOrder = shopperViewModel::placeOrder,
                                onExploreShop = {
                                    navController.navigate(BuyzoraRoutes.SHOP) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable(BuyzoraRoutes.ORDERS) {
                            OrdersScreen(
                                ordersState = ordersState,
                                onCancelOrder = shopperViewModel::cancelOrder,
                                onRequestReturn = shopperViewModel::requestReturn,
                                onExploreShop = {
                                    navController.navigate(BuyzoraRoutes.SHOP) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable(BuyzoraRoutes.ACCOUNT) {
                            SupportPrivacyScreen(
                                userEmail = currentUserEmail,
                                userName = currentUserName,
                                isAdmin = isAdmin,
                                profileState = profileState,
                                ordersState = ordersState,
                                ticketsState = ticketsState,
                                onSaveProfile = shopperViewModel::updateProfile,
                                onSubmitTicket = shopperViewModel::submitSupportTicket,
                                onOpenAdminPanel = {
                                    if (isAdmin) {
                                        navController.navigate(BuyzoraRoutes.ADMIN)
                                    }
                                },
                                onSignOut = {
                                    signOut(credentialManager = credentialManager, scope = scope)
                                }
                            )
                        }

                        composable(BuyzoraRoutes.ADMIN) {
                            if (isAdmin) {
                                val adminViewModel: AdminViewModel = viewModel(
                                    key = "admin_$currentUserId",
                                    factory = viewModelFactory {
                                        initializer {
                                            val app = checkNotNull(this[APPLICATION_KEY])
                                            val databaseId = app.getString(R.string.firestore_database_id)
                                            val db = FirebaseFirestore.getInstance(databaseId)
                                            AdminViewModel(
                                                catalogRepository = CatalogRepository(db),
                                                adminRepository = AdminRepository(db)
                                            )
                                        }
                                    }
                                )
                                AdminPanelScreen(
                                    adminViewModel = adminViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
