package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UiState
import com.example.model.CartItem
import com.example.model.PaymentMethod
import com.example.model.UserProfile
import com.example.ui.components.ProductArtwork
import com.example.ui.components.formatCurrency
import com.example.ui.theme.BuyzoraEmerald
import com.example.ui.theme.BuyzoraEmeraldContainer
import kotlin.math.roundToInt

@Composable
fun CartCheckoutScreen(
    cartState: UiState<List<CartItem>>,
    profileState: UiState<UserProfile?>,
    onUpdateQuantity: (String, Long) -> Unit,
    onRemoveItem: (String) -> Unit,
    onPlaceOrder: (
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        paymentDetails: String,
        shippingAddress: String,
        shippingPhone: String,
        promoDiscount: Double
    ) -> Unit,
    onExploreShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    var addressInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var initializedFromProfile by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (!initializedFromProfile && profile != null) {
            val combinedAddress = listOfNotNull(
                profile.defaultAddress?.takeIf { it.isNotBlank() },
                profile.city?.takeIf { it.isNotBlank() },
                profile.postalCode?.takeIf { it.isNotBlank() }
            ).joinToString(", ")
            if (combinedAddress.isNotBlank()) addressInput = combinedAddress
            if (!profile.phone.isNullOrBlank()) phoneInput = profile.phone
            initializedFromProfile = true
        }
    }

    var promoCode by remember { mutableStateOf("") }
    var appliedPromo by remember { mutableStateOf<String?>(null) }
    var promoError by remember { mutableStateOf<String?>(null) }

    var selectedPayment by remember { mutableStateOf(PaymentMethod.UPI) }
    var upiId by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf("HDFC Bank") }
    var checkoutError by remember { mutableStateOf<String?>(null) }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        when (cartState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(cartState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val items = cartState.data
                if (items.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Your shopping bag is empty", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add clothes, gadgets, skincare, or everyday essentials to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onExploreShop,
                            modifier = Modifier.testTag("empty_cart_shop_button")
                        ) {
                            Text("Start Shopping")
                        }
                    }
                } else {
                    val subtotal = (items.sumOf { it.price * it.quantity } * 100.0).roundToInt() / 100.0
                    val promoDiscount = when (appliedPromo) {
                        "BUYZORA15" -> ((subtotal * 0.15) * 100.0).roundToInt() / 100.0
                        "WELCOME10" -> if (subtotal >= 50.0) 10.0 else 0.0
                        else -> 0.0
                    }
                    val shippingFee = if ((subtotal - promoDiscount) >= 50.0) 0.0 else 4.99
                    val totalAmount = ((subtotal - promoDiscount + shippingFee) * 100.0).roundToInt() / 100.0

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.widthIn(max = 720.dp)
                    ) {
                        item {
                            Text(
                                text = "Shopping Bag (${items.sumOf { it.quantity }})",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        items(items, key = { it.id }) { item ->
                            CartItemRow(
                                item = item,
                                onDecrement = { onUpdateQuantity(item.id, item.quantity - 1L) },
                                onIncrement = { onUpdateQuantity(item.id, item.quantity + 1L) },
                                onRemove = { onRemoveItem(item.id) }
                            )
                        }

                        // Promo Code Section
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalOffer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Apply Promo Code", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = promoCode,
                                            onValueChange = { promoCode = it.uppercase(); promoError = null },
                                            placeholder = { Text("Try BUYZORA15 or WELCOME10") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("promo_code_input")
                                        )
                                        Button(
                                            onClick = {
                                                val code = promoCode.trim().uppercase()
                                                when (code) {
                                                    "BUYZORA15" -> {
                                                        appliedPromo = code
                                                        promoError = null
                                                    }
                                                    "WELCOME10" -> {
                                                        if (subtotal >= 50.0) {
                                                            appliedPromo = code
                                                            promoError = null
                                                        } else {
                                                            promoError = "WELCOME10 requires $50+ order"
                                                        }
                                                    }
                                                    else -> promoError = "Invalid code. Try BUYZORA15"
                                                }
                                            },
                                            modifier = Modifier.testTag("apply_promo_button")
                                        ) {
                                            Text("Apply")
                                        }
                                    }
                                    if (appliedPromo != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp)
                                        ) {
                                            Text(
                                                text = "Applied $appliedPromo (-${formatCurrency(promoDiscount)})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = BuyzoraEmerald
                                            )
                                            TextButton(onClick = { appliedPromo = null; promoCode = "" }) {
                                                Text("Remove")
                                            }
                                        }
                                    }
                                    if (promoError != null) {
                                        Text(
                                            text = promoError ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Delivery Details
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalShipping,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Delivery Address", style = MaterialTheme.typography.titleMedium)
                                    }
                                    OutlinedTextField(
                                        value = addressInput,
                                        onValueChange = { addressInput = it; checkoutError = null },
                                        label = { Text("Full Street Address, City & PIN/ZIP") },
                                        minLines = 2,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkout_address_input")
                                    )
                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { phoneInput = it; checkoutError = null },
                                        label = { Text("Delivery Phone Number") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkout_phone_input")
                                    )
                                }
                            }
                        }

                        // Payment Method Selection
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Payment Method", style = MaterialTheme.typography.titleMedium)
                                        Surface(
                                            color = BuyzoraEmeraldContainer,
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Lock,
                                                    contentDescription = null,
                                                    tint = BuyzoraEmerald,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "TLS Secured",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BuyzoraEmerald
                                                )
                                            }
                                        }
                                    }

                                    PaymentMethod.entries.forEach { method ->
                                        val isSelected = selectedPayment == method
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedPayment = method; checkoutError = null }
                                                .testTag("payment_method_${method.wireValue}")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(10.dp)
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedPayment = method; checkoutError = null }
                                                )
                                                Icon(
                                                    imageVector = when (method) {
                                                        PaymentMethod.UPI -> Icons.Filled.QrCode2
                                                        PaymentMethod.CARD -> Icons.Filled.CreditCard
                                                        PaymentMethod.NET_BANKING -> Icons.Filled.AccountBalance
                                                        PaymentMethod.COD -> Icons.Filled.Payments
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(method.label, style = MaterialTheme.typography.titleSmall)
                                                    Text(
                                                        text = method.subtitle,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    when (selectedPayment) {
                                        PaymentMethod.UPI -> {
                                            OutlinedTextField(
                                                value = upiId,
                                                onValueChange = { upiId = it; checkoutError = null },
                                                label = { Text("Enter UPI ID (e.g. name@oksbi)") },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("upi_id_input")
                                            )
                                        }
                                        PaymentMethod.CARD -> {
                                            OutlinedTextField(
                                                value = cardNumber,
                                                onValueChange = {
                                                    cardNumber = it.filter { ch -> ch.isDigit() }.take(16)
                                                    checkoutError = null
                                                },
                                                label = { Text("16-Digit Card Number (Tokenized)") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("card_number_input")
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedTextField(
                                                    value = cardExpiry,
                                                    onValueChange = { cardExpiry = it.take(5); checkoutError = null },
                                                    label = { Text("MM/YY") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = cardCvv,
                                                    onValueChange = {
                                                        cardCvv = it.filter { ch -> ch.isDigit() }.take(4)
                                                        checkoutError = null
                                                    },
                                                    label = { Text("CVV (Never stored)") },
                                                    singleLine = true,
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        PaymentMethod.NET_BANKING -> {
                                            val banks = listOf("HDFC Bank", "ICICI Bank", "SBI", "Axis Bank", "Kotak")
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                banks.take(3).forEach { bank ->
                                                    FilterChip(
                                                        selected = selectedBank == bank,
                                                        onClick = { selectedBank = bank },
                                                        label = { Text(bank) }
                                                    )
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                banks.drop(3).forEach { bank ->
                                                    FilterChip(
                                                        selected = selectedBank == bank,
                                                        onClick = { selectedBank = bank },
                                                        label = { Text(bank) }
                                                    )
                                                }
                                            }
                                        }
                                        PaymentMethod.COD -> {
                                            Text(
                                                text = "Pay via cash or UPI QR at delivery. Zero advance fee.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Order Summary & Place Order
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text("Order Summary", style = MaterialTheme.typography.titleMedium)
                                    SummaryRow("Subtotal", formatCurrency(subtotal))
                                    if (promoDiscount > 0.0) {
                                        SummaryRow("Promo Savings", "-${formatCurrency(promoDiscount)}", highlight = true)
                                    }
                                    SummaryRow(
                                        "Express Shipping",
                                        if (shippingFee == 0.0) "FREE" else formatCurrency(shippingFee),
                                        highlight = shippingFee == 0.0
                                    )
                                    HorizontalDivider()
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Total Payable", style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = formatCurrency(totalAmount),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (checkoutError != null) {
                                        Text(
                                            text = checkoutError ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            if (addressInput.trim().length < 5) {
                                                checkoutError = "Please enter a complete delivery address."
                                                return@Button
                                            }
                                            if (phoneInput.trim().length < 6) {
                                                checkoutError = "Please enter a valid contact phone number."
                                                return@Button
                                            }
                                            val paymentDetails = when (selectedPayment) {
                                                PaymentMethod.UPI -> {
                                                    if (!upiId.contains("@") || upiId.trim().length < 5) {
                                                        checkoutError = "Enter a valid UPI ID (e.g. alex@oksbi)."
                                                        return@Button
                                                    }
                                                    val prefix = upiId.trim().substringBefore("@").take(3)
                                                    val suffix = upiId.trim().substringAfter("@")
                                                    "UPI: $prefix***@$suffix"
                                                }
                                                PaymentMethod.CARD -> {
                                                    if (cardNumber.length < 12 || cardExpiry.length < 4 || cardCvv.length < 3) {
                                                        checkoutError = "Enter valid card number, expiry (MM/YY), and CVV."
                                                        return@Button
                                                    }
                                                    "Card •••• ${cardNumber.takeLast(4)}"
                                                }
                                                PaymentMethod.NET_BANKING -> "NetBanking: $selectedBank"
                                                PaymentMethod.COD -> "Cash on Delivery"
                                            }
                                            onPlaceOrder(
                                                items,
                                                selectedPayment,
                                                paymentDetails,
                                                addressInput.trim(),
                                                phoneInput.trim(),
                                                promoDiscount
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("place_order_button")
                                    ) {
                                        Text(
                                            text = if (selectedPayment == PaymentMethod.COD) {
                                                "Confirm COD Order • ${formatCurrency(totalAmount)}"
                                            } else {
                                                "Pay Securely • ${formatCurrency(totalAmount)}"
                                            },
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            ProductArtwork(
                imageKey = item.imageKey,
                imageUrl = item.imageUrl,
                category = item.category,
                contentDescription = item.title,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.brand.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.selectedVariant.isNullOrBlank()) {
                    Text(
                        text = "Option: ${item.selectedVariant}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatCurrency(item.price * item.quantity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onDecrement,
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .size(36.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("cart_dec_${item.id}")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = item.quantity.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        OutlinedButton(
                            onClick = onIncrement,
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .size(36.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("cart_inc_${item.id}")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase quantity", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("cart_remove_${item.id}")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove item")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) BuyzoraEmerald else MaterialTheme.colorScheme.onSurface
        )
    }
}
