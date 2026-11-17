package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UiState
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.SupportTicket
import com.example.model.TicketStatus
import com.example.model.UserProfile
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.formatCurrency
import com.example.ui.theme.BuyzoraEmerald
import com.example.ui.theme.BuyzoraEmeraldContainer
import com.example.viewmodel.AdminViewModel
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val productsState by adminViewModel.allProductsState.collectAsStateWithLifecycle()
    val ordersState by adminViewModel.allOrdersState.collectAsStateWithLifecycle()
    val customersState by adminViewModel.allCustomersState.collectAsStateWithLifecycle()
    val ticketsState by adminViewModel.allTicketsState.collectAsStateWithLifecycle()
    val banner by adminViewModel.adminBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(banner) {
        banner?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearBanner()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var replyTicket by remember { mutableStateOf<SupportTicket?>(null) }

    val products = (productsState as? UiState.Success)?.data.orEmpty()
    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val customers = (customersState as? UiState.Success)?.data.orEmpty()
    val tickets = (ticketsState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buyzora Admin Console") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductDialog = true
                    },
                    modifier = Modifier.testTag("admin_add_product_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add product")
                }
            }
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // KPI Summary Strip
            val totalRevenue = orders.filter { it.status != OrderStatus.CANCELLED.wireValue && it.status != OrderStatus.REFUNDED.wireValue }
                .sumOf { it.totalAmount }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item { AdminMetricCard("Revenue", formatCurrency(totalRevenue)) }
                item { AdminMetricCard("Orders", orders.size.toString()) }
                item { AdminMetricCard("Products", products.size.toString()) }
                item { AdminMetricCard("Customers", customers.size.toString()) }
                item { AdminMetricCard("Open Tickets", tickets.count { it.status != TicketStatus.RESOLVED.wireValue }.toString()) }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Products") },
                    icon = { Icon(Icons.Filled.Inventory, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Orders") },
                    icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Customers") },
                    icon = { Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Support") },
                    icon = { Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> AdminProductsTab(
                        products = products,
                        onSeedCatalog = { adminViewModel.seedStarterCatalog() },
                        onEditProduct = { product ->
                            editingProduct = product
                            showProductDialog = true
                        },
                        onDeleteProduct = { adminViewModel.deleteProduct(it.id) }
                    )
                    1 -> AdminOrdersTab(
                        orders = orders,
                        onAdvanceStatus = { order, next -> adminViewModel.updateOrderStatus(order, next) }
                    )
                    2 -> AdminCustomersTab(
                        customers = customers,
                        onToggleStatus = { adminViewModel.toggleCustomerStatus(it) }
                    )
                    3 -> AdminSupportTab(
                        tickets = tickets,
                        onReplyClick = { replyTicket = it }
                    )
                }
            }
        }
    }

    if (showProductDialog) {
        ProductEditorDialog(
            initial = editingProduct,
            onDismiss = { showProductDialog = false },
            onSave = { product, isNew ->
                adminViewModel.saveProduct(product, isNew)
                showProductDialog = false
            }
        )
    }

    replyTicket?.let { ticket ->
        var replyText by remember(ticket.id) { mutableStateOf(ticket.adminReply.orEmpty()) }
        var resolved by remember(ticket.id) { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { replyTicket = null },
            title = { Text("Reply: ${ticket.subject}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customer: ${ticket.customerName} (${ticket.customerEmail})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(ticket.message, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Admin Reply") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mark Resolved", style = MaterialTheme.typography.titleSmall)
                        Switch(checked = resolved, onCheckedChange = { resolved = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.replyToTicket(
                            ticketId = ticket.id,
                            reply = replyText,
                            status = if (resolved) TicketStatus.RESOLVED else TicketStatus.IN_REVIEW
                        )
                        replyTicket = null
                    },
                    enabled = replyText.trim().isNotEmpty()
                ) {
                    Text("Send Reply")
                }
            },
            dismissButton = {
                TextButton(onClick = { replyTicket = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminMetricCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminProductsTab(
    products: List<Product>,
    onSeedCatalog: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.widthIn(max = 720.dp)
    ) {
        if (products.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text("No Firestore products yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(onClick = onSeedCatalog) {
                            Text("Seed 20 Starter Products")
                        }
                    }
                }
            }
        }
        items(products, key = { it.id }) { product ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${product.category} • ${formatCurrency(product.price)} • Stock: ${product.stock}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!product.active) {
                            Text("Hidden (Inactive)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Row {
                        IconButton(onClick = { onEditProduct(product) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit ${product.name}")
                        }
                        IconButton(onClick = { onDeleteProduct(product) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${product.name}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOrdersTab(
    orders: List<Order>,
    onAdvanceStatus: (Order, OrderStatus) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.widthIn(max = 720.dp)
    ) {
        items(orders, key = { it.id }) { order ->
            val current = OrderStatus.fromWire(order.status)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(order.orderNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${order.customerName} • ${formatCurrency(order.totalAmount)}", style = MaterialTheme.typography.bodySmall)
                        }
                        OrderStatusChip(order.status)
                    }
                    Text(
                        text = "${order.paymentDetails} • ${order.shippingAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val nextStep = when (current) {
                            OrderStatus.PLACED -> OrderStatus.CONFIRMED
                            OrderStatus.CONFIRMED -> OrderStatus.PACKED
                            OrderStatus.PACKED -> OrderStatus.SHIPPED
                            OrderStatus.SHIPPED -> OrderStatus.OUT_FOR_DELIVERY
                            OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.DELIVERED
                            OrderStatus.RETURN_REQUESTED -> OrderStatus.RETURNED
                            OrderStatus.RETURNED -> OrderStatus.REFUNDED
                            else -> null
                        }
                        if (nextStep != null) {
                            Button(onClick = { onAdvanceStatus(order, nextStep) }) {
                                Text("Mark ${nextStep.label}")
                            }
                        }
                        if (current == OrderStatus.RETURN_REQUESTED || current == OrderStatus.CANCELLED) {
                            OutlinedButton(onClick = { onAdvanceStatus(order, OrderStatus.REFUNDED) }) {
                                Text("Complete Refund")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCustomersTab(
    customers: List<UserProfile>,
    onToggleStatus: (UserProfile) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.widthIn(max = 720.dp)
    ) {
        items(customers, key = { it.id }) { customer ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customer.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(customer.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!customer.phone.isNullOrBlank()) {
                            Text(customer.phone, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedButton(onClick = { onToggleStatus(customer) }) {
                        Text(if (customer.accountStatus == "active") "Active" else "Suspended")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSupportTab(
    tickets: List<SupportTicket>,
    onReplyClick: (SupportTicket) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.widthIn(max = 720.dp)
    ) {
        items(tickets, key = { it.id }) { ticket ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(ticket.subject, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Surface(
                            color = if (ticket.status == TicketStatus.RESOLVED.wireValue) BuyzoraEmeraldContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = TicketStatus.fromWire(ticket.status).label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ticket.status == TicketStatus.RESOLVED.wireValue) BuyzoraEmerald else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text("${ticket.customerName} • ${ticket.topic}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text(ticket.message, style = MaterialTheme.typography.bodySmall)
                    if (!ticket.adminReply.isNullOrBlank()) {
                        HorizontalDivider()
                        Text("Reply: ${ticket.adminReply}", style = MaterialTheme.typography.bodySmall, color = BuyzoraEmerald)
                    }
                    Button(onClick = { onReplyClick(ticket) }) {
                        Text(if (ticket.adminReply.isNullOrBlank()) "Reply & Resolve" else "Update Reply")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorDialog(
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (Product, Boolean) -> Unit
) {
    val isNew = initial == null
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var brand by remember { mutableStateOf(initial?.brand.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: ProductCategory.CLOTHES.displayName) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var priceStr by remember { mutableStateOf(initial?.price?.toString().orEmpty()) }
    var originalPriceStr by remember { mutableStateOf(initial?.originalPrice?.toString().orEmpty()) }
    var stockStr by remember { mutableStateOf(initial?.stock?.toString() ?: "25") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }
    var featured by remember { mutableStateOf(initial?.featured ?: false) }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val imageKeyForCategory = when (ProductCategory.fromName(category)) {
        ProductCategory.CLOTHES, ProductCategory.SHOES, ProductCategory.FASHION_ACCESSORIES -> "img_cat_fashion"
        ProductCategory.ELECTRONICS, ProductCategory.SMARTPHONES, ProductCategory.GADGETS -> "img_cat_tech"
        ProductCategory.FACE_WASHES, ProductCategory.SKINCARE, ProductCategory.HAIRCARE -> "img_cat_beauty"
        else -> "img_hero_promo"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add Store Product" else "Edit Product") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, singleLine = true)
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, singleLine = true)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        ProductCategory.selectableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = { category = cat.displayName; categoryExpanded = false }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = originalPriceStr,
                        onValueChange = { originalPriceStr = it },
                        label = { Text("MRP ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Stock Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL (Optional)") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Featured Deal")
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Visible in Store")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                if (errorText != null) {
                    Text(errorText ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull()
                    val orig = originalPriceStr.toDoubleOrNull() ?: price
                    val stock = stockStr.toLongOrNull() ?: 0L
                    if (name.trim().length < 2 || brand.trim().isEmpty() || description.trim().length < 4 || price == null || price <= 0.0) {
                        errorText = "Fill in valid product name, brand, description (4+ chars), and price > 0."
                        return@Button
                    }
                    val safeOrig = maxOf(orig ?: price, price)
                    val discount = if (safeOrig > price) (((safeOrig - price) / safeOrig) * 100.0).roundToLong().coerceIn(0L, 95L) else 0L
                    val product = Product(
                        id = initial?.id.orEmpty(),
                        name = name.trim(),
                        description = description.trim(),
                        price = price,
                        category = category,
                        imageUrl = imageUrl.trim().ifBlank { null },
                        brand = brand.trim(),
                        originalPrice = safeOrig,
                        discountPercent = discount,
                        rating = initial?.rating ?: 4.8,
                        reviewCount = initial?.reviewCount ?: 12L,
                        stock = stock,
                        imageKey = initial?.imageKey ?: imageKeyForCategory,
                        featured = featured,
                        active = active,
                        highlights = initial?.highlights ?: listOf("Buyzora Verified", "14-Day Returnable")
                    )
                    onSave(product, isNew)
                }
            ) {
                Text(if (isNew) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
