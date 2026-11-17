package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.UiState
import com.example.model.Order
import com.example.model.SupportTicket
import com.example.model.TicketStatus
import com.example.model.TicketTopic
import com.example.model.UserProfile
import com.example.ui.theme.BuyzoraEmerald
import com.example.ui.theme.BuyzoraEmeraldContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportPrivacyScreen(
    userEmail: String,
    userName: String,
    isAdmin: Boolean,
    profileState: UiState<UserProfile?>,
    ordersState: UiState<List<Order>>,
    ticketsState: UiState<List<SupportTicket>>,
    onSaveProfile: (
        displayName: String,
        phone: String,
        address: String,
        city: String,
        postalCode: String,
        marketingOptIn: Boolean,
        twoFactorReminder: Boolean
    ) -> Unit,
    onSubmitTicket: (
        topic: String,
        subject: String,
        message: String,
        orderNumber: String?
    ) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val tickets = (ticketsState as? UiState.Success)?.data.orEmpty()

    var displayName by remember { mutableStateOf(userName) }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var marketingOptIn by remember { mutableStateOf(false) }
    var twoFactorReminder by remember { mutableStateOf(true) }
    var profileHydrated by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (!profileHydrated && profile != null) {
            displayName = profile.displayName.ifBlank { userName }
            phone = profile.phone.orEmpty()
            address = profile.defaultAddress.orEmpty()
            city = profile.city.orEmpty()
            postalCode = profile.postalCode.orEmpty()
            marketingOptIn = profile.privacyMarketingOptIn
            twoFactorReminder = profile.twoFactorReminderEnabled
            profileHydrated = true
        }
    }

    var selectedTopic by remember { mutableStateOf(TicketTopic.ORDER_ISSUE.label) }
    var topicExpanded by remember { mutableStateOf(false) }
    var selectedOrderNo by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var ticketError by remember { mutableStateOf<String?>(null) }

    val faqs = remember {
        listOf(
            "How secure are UPI, Card & Net Banking payments?" to "All checkout sessions are encrypted over 256-bit TLS. Card CVV and UPI PINs are never persisted in our database—only masked reference tokens (e.g. Card •••• 4242) are stored on your order receipt.",
            "How do 14-day returns and refunds work?" to "Open the Orders tab, select any Delivered order, and tap 'Return / Refund'. Refunds for prepaid UPI/Card/NetBanking orders are initiated immediately upon confirmation.",
            "Can I cancel an order after placing it?" to "Yes! Any order in Placed, Confirmed, or Packed status can be cancelled with one tap from the Orders tab."
        )
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 720.dp)
        ) {
            item {
                Text(
                    text = "Account, Privacy & Support",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Admin Console Entry (only visible to verified admins)
            if (isAdmin) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Store Admin Console",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Manage products, prices, orders, customers & tickets",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Button(
                                onClick = onOpenAdminPanel,
                                modifier = Modifier.testTag("open_admin_panel_button")
                            ) {
                                Text("Open")
                            }
                        }
                    }
                }
            }

            // Customer Profile & Saved Delivery Info
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Customer Profile & Address", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Default Street Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = postalCode,
                                onValueChange = { postalCode = it },
                                label = { Text("PIN / ZIP") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                onSaveProfile(
                                    displayName, phone, address, city, postalCode,
                                    marketingOptIn, twoFactorReminder
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_profile_button")
                        ) {
                            Text("Save Profile & Privacy Preferences")
                        }
                    }
                }
            }

            // Data Privacy & Security Shield
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Security, contentDescription = null, tint = BuyzoraEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Privacy & Data Security", style = MaterialTheme.typography.titleMedium)
                            }
                            Surface(color = BuyzoraEmeraldContainer, shape = RoundedCornerShape(999.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = BuyzoraEmerald, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Isolated Vault", style = MaterialTheme.typography.labelSmall, color = BuyzoraEmerald)
                                }
                            }
                        }

                        Text(
                            text = "Your profile, cart, wishlist, and order history are strictly scoped to your verified Google Identity UID.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Personalized Deal Alerts", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Receive curated discount offers (opt-in only)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = marketingOptIn,
                                onCheckedChange = { marketingOptIn = it }
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Account Sign-In Verification", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Enforce Google Identity verification on sensitive actions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = twoFactorReminder,
                                onCheckedChange = { twoFactorReminder = it }
                            )
                        }
                    }
                }
            }

            // Customer Support Desk
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("24/7 Customer Support", style = MaterialTheme.typography.titleMedium)
                        }

                        ExposedDropdownMenuBox(
                            expanded = topicExpanded,
                            onExpandedChange = { topicExpanded = !topicExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedTopic,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Support Topic") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = topicExpanded,
                                onDismissRequest = { topicExpanded = false }
                            ) {
                                TicketTopic.entries.forEach { topic ->
                                    DropdownMenuItem(
                                        text = { Text(topic.label) },
                                        onClick = {
                                            selectedTopic = topic.label
                                            topicExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (orders.isNotEmpty()) {
                            OutlinedTextField(
                                value = selectedOrderNo,
                                onValueChange = { selectedOrderNo = it },
                                label = { Text("Order # (Optional, e.g. ${orders.first().orderNumber})") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it; ticketError = null },
                            label = { Text("Subject") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_subject_input")
                        )

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it; ticketError = null },
                            label = { Text("How can our support team help?") },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_message_input")
                        )

                        if (ticketError != null) {
                            Text(
                                text = ticketError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = {
                                if (subject.trim().length < 3 || message.trim().length < 5) {
                                    ticketError = "Enter a subject (3+ chars) and message (5+ chars)."
                                    return@Button
                                }
                                onSubmitTicket(
                                    selectedTopic,
                                    subject.trim(),
                                    message.trim(),
                                    selectedOrderNo.trim().ifBlank { null }
                                )
                                subject = ""
                                message = ""
                                selectedOrderNo = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_ticket_button")
                        ) {
                            Text("Submit Support Ticket")
                        }
                    }
                }
            }

            if (tickets.isNotEmpty()) {
                item {
                    Text("My Support Tickets (${tickets.size})", style = MaterialTheme.typography.titleMedium)
                }
                items(tickets, key = { it.id }) { ticket ->
                    val status = TicketStatus.fromWire(ticket.status)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(ticket.subject, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Surface(
                                    color = if (status == TicketStatus.RESOLVED) BuyzoraEmeraldContainer else MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = status.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (status == TicketStatus.RESOLVED) BuyzoraEmerald else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${ticket.topic}${ticket.orderNumber?.let { " • $it" } ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(ticket.message, style = MaterialTheme.typography.bodySmall)
                            if (!ticket.adminReply.isNullOrBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Support Reply: ${ticket.adminReply}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BuyzoraEmerald
                                )
                            }
                        }
                    }
                }
            }

            // FAQs
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Help Center FAQs", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        faqs.forEachIndexed { idx, (q, a) ->
                            var expanded by remember { mutableStateOf(idx == 0) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded }
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(q, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null
                                    )
                                }
                                AnimatedVisibility(visible = expanded) {
                                    Text(
                                        text = a,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                            if (idx < faqs.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }

            // Sign Out
            item {
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out Safely")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
