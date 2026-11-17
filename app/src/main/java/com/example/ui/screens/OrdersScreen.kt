package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UiState
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.PaymentStatus
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.formatCurrency
import com.example.ui.theme.BuyzoraEmerald
import com.example.ui.theme.BuyzoraEmeraldContainer
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OrdersScreen(
    ordersState: UiState<List<Order>>,
    onCancelOrder: (Order, String) -> Unit,
    onRequestReturn: (Order, String) -> Unit,
    onExploreShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cancelTarget by remember { mutableStateOf<Order?>(null) }
    var returnTarget by remember { mutableStateOf<Order?>(null) }
    var actionReason by remember { mutableStateOf("") }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        when (ordersState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(ordersState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val orders = ordersState.data
                if (orders.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("No orders yet", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your placed orders, live tracking updates, and easy return controls will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(onClick = onExploreShop) {
                            Text("Browse Products")
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.widthIn(max = 720.dp)
                    ) {
                        item {
                            Text(
                                text = "Orders & Tracking (${orders.size})",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        items(orders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                onCancelClick = {
                                    actionReason = "Changed my mind"
                                    cancelTarget = order
                                },
                                onReturnClick = {
                                    actionReason = ""
                                    returnTarget = order
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    cancelTarget?.let { order ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel ${order.orderNumber}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (order.paymentStatus == PaymentStatus.PAID.wireValue) {
                            "A full refund of ${formatCurrency(order.totalAmount)} will be initiated automatically to your original payment method."
                        } else {
                            "Your Cash on Delivery order will be cancelled immediately."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("Cancellation reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelOrder(order, actionReason)
                        cancelTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_cancel_order")
                ) {
                    Text("Cancel Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("Keep Order")
                }
            }
        )
    }

    returnTarget?.let { order ->
        AlertDialog(
            onDismissRequest = { returnTarget = null },
            title = { Text("Return & Refund (${order.orderNumber})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "14-day hassle-free return pickup. Refund of ${formatCurrency(order.totalAmount)} is initiated upon request.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("Reason for return (min 3 chars)") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("return_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (actionReason.trim().length >= 3) {
                            onRequestReturn(order, actionReason)
                            returnTarget = null
                        }
                    },
                    enabled = actionReason.trim().length >= 3,
                    modifier = Modifier.testTag("confirm_return_order")
                ) {
                    Text("Request Return")
                }
            },
            dismissButton = {
                TextButton(onClick = { returnTarget = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onCancelClick: () -> Unit,
    onReturnClick: () -> Unit
) {
    val status = OrderStatus.fromWire(order.status)
    val paymentStatus = PaymentStatus.fromWire(order.paymentStatus)
    val dateStr = remember(order.createdAt) {
        val date = order.createdAt?.toDate()
        if (date != null) SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date) else "Just now"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Placed $dateStr • ${order.paymentDetails}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OrderStatusChip(order.status)
            }

            HorizontalDivider()

            order.items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${item.quantity}x ${item.title}" +
                            (item.selectedVariant?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatCurrency(item.price * item.quantity),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Tracking Progress Bar for active fulfillment
            if (status.stepIndex >= 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tracking: ${order.trackingNumber}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Text(
                                text = order.estimatedDelivery,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OrderStatus.trackingSteps.forEach { step ->
                                val reached = status.stepIndex >= step.stepIndex
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = CircleShape
                                            )
                                    ) {
                                        if (reached) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = step.label,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when (step) {
                                            OrderStatus.PLACED -> "Placed"
                                            OrderStatus.CONFIRMED -> "Conf."
                                            OrderStatus.PACKED -> "Packed"
                                            OrderStatus.SHIPPED -> "Shipped"
                                            OrderStatus.OUT_FOR_DELIVERY -> "Out"
                                            OrderStatus.DELIVERED -> "Done"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (paymentStatus == PaymentStatus.REFUND_INITIATED || paymentStatus == PaymentStatus.REFUNDED) {
                Surface(
                    color = BuyzoraEmeraldContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${paymentStatus.label}: ${formatCurrency(order.refundAmount ?: order.totalAmount)}" +
                            (order.returnReason?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = BuyzoraEmerald,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Total: ${formatCurrency(order.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Payment: ${paymentStatus.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (status in listOf(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PACKED)) {
                        OutlinedButton(
                            onClick = onCancelClick,
                            modifier = Modifier.testTag("cancel_order_${order.id}")
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                    if (status == OrderStatus.DELIVERED) {
                        Button(
                            onClick = onReturnClick,
                            modifier = Modifier.testTag("return_order_${order.id}")
                        ) {
                            Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Return / Refund")
                        }
                    }
                }
            }
        }
    }
}
