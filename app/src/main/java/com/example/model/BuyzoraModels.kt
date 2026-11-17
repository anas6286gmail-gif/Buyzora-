package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ProductCategory(val displayName: String, val groupLabel: String) {
    ALL("All", "Explore"),
    CLOTHES("Clothes", "Fashion"),
    SHOES("Shoes", "Fashion"),
    FASHION_ACCESSORIES("Fashion Accessories", "Fashion"),
    ELECTRONICS("Electronics", "Tech"),
    SMARTPHONES("Smartphones", "Tech"),
    GADGETS("Gadgets", "Tech"),
    FACE_WASHES("Face Washes", "Beauty"),
    SKINCARE("Skincare", "Beauty"),
    HAIRCARE("Haircare", "Beauty"),
    EVERYDAY_ESSENTIALS("Everyday Essentials", "Daily");

    companion object {
        val selectableCategories: List<ProductCategory> = entries.filter { it != ALL }
        fun fromName(name: String): ProductCategory =
            entries.firstOrNull { it.displayName == name || it.name == name } ?: EVERYDAY_ESSENTIALS
    }
}

enum class PaymentMethod(val wireValue: String, val label: String, val subtitle: String) {
    UPI("UPI", "UPI Instant Pay", "GPay, PhonePe, Paytm & BHIM"),
    CARD("CARD", "Debit / Credit Card", "Visa, Mastercard & RuPay (Tokenized)"),
    NET_BANKING("NET_BANKING", "Net Banking", "All major banks supported"),
    COD("COD", "Cash on Delivery", "Pay safely at your doorstep");

    companion object {
        fun fromWire(value: String): PaymentMethod =
            entries.firstOrNull { it.wireValue == value } ?: UPI
    }
}

enum class OrderStatus(val wireValue: String, val label: String, val stepIndex: Int) {
    PLACED("PLACED", "Order Placed", 0),
    CONFIRMED("CONFIRMED", "Confirmed", 1),
    PACKED("PACKED", "Packed", 2),
    SHIPPED("SHIPPED", "Shipped", 3),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "Out for Delivery", 4),
    DELIVERED("DELIVERED", "Delivered", 5),
    CANCELLED("CANCELLED", "Cancelled", -1),
    RETURN_REQUESTED("RETURN_REQUESTED", "Return Requested", -2),
    RETURNED("RETURNED", "Returned", -3),
    REFUNDED("REFUNDED", "Refunded", -4);

    companion object {
        val trackingSteps = listOf(PLACED, CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED)
        fun fromWire(value: String): OrderStatus =
            entries.firstOrNull { it.wireValue == value } ?: PLACED
    }
}

enum class PaymentStatus(val wireValue: String, val label: String) {
    PAID("PAID", "Paid"),
    COD_PENDING("COD_PENDING", "Pay on Delivery"),
    REFUND_INITIATED("REFUND_INITIATED", "Refund Initiated"),
    REFUNDED("REFUNDED", "Refunded");

    companion object {
        fun fromWire(value: String): PaymentStatus =
            entries.firstOrNull { it.wireValue == value } ?: PAID
    }
}

enum class TicketTopic(val label: String) {
    ORDER_ISSUE("Order Issue"),
    PAYMENT_REFUND("Payment & Refund"),
    RETURN_REQUEST("Return Request"),
    PRODUCT_INQUIRY("Product Inquiry"),
    ACCOUNT_PRIVACY("Account & Privacy");

    companion object {
        fun fromLabel(label: String): TicketTopic =
            entries.firstOrNull { it.label == label } ?: ORDER_ISSUE
    }
}

enum class TicketStatus(val wireValue: String, val label: String) {
    OPEN("OPEN", "Open"),
    IN_REVIEW("IN_REVIEW", "In Review"),
    RESOLVED("RESOLVED", "Resolved");

    companion object {
        fun fromWire(value: String): TicketStatus =
            entries.firstOrNull { it.wireValue == value } ?: OPEN
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String? = null,
    val defaultAddress: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val role: String = "customer",
    val accountStatus: String = "active",
    val privacyMarketingOptIn: Boolean = false,
    val twoFactorReminderEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("displayName", displayName.ifBlank { "Buyzora Shopper" }.take(80))
        put("email", email.ifBlank { "shopper@buyzora.app" }.take(160))
        phone?.takeIf { it.isNotBlank() }?.let { put("phone", it.take(25)) }
        defaultAddress?.takeIf { it.isNotBlank() }?.let { put("defaultAddress", it.take(300)) }
        city?.takeIf { it.isNotBlank() }?.let { put("city", it.take(80)) }
        postalCode?.takeIf { it.isNotBlank() }?.let { put("postalCode", it.take(20)) }
        put("role", "customer")
        put("accountStatus", "active")
        put("privacyMarketingOptIn", privacyMarketingOptIn)
        put("twoFactorReminderEnabled", twoFactorReminderEnabled)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = ProductCategory.EVERYDAY_ESSENTIALS.displayName,
    val imageUrl: String? = null,
    val brand: String = "",
    val originalPrice: Double = 0.0,
    val discountPercent: Long = 0L,
    val rating: Double = 4.5,
    val reviewCount: Long = 0L,
    val stock: Long = 0L,
    val imageKey: String = "img_hero_promo",
    val featured: Boolean = false,
    val active: Boolean = true,
    val highlights: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("name", name.trim().take(120))
        put("brand", brand.trim().take(80))
        put("category", category)
        put("description", description.trim().take(1000))
        put("price", price)
        put("originalPrice", maxOf(originalPrice, price))
        put("discountPercent", discountPercent.coerceIn(0L, 95L))
        put("rating", rating.coerceIn(0.0, 5.0))
        put("reviewCount", reviewCount.coerceAtLeast(0L))
        put("stock", stock.coerceAtLeast(0L))
        put("imageKey", imageKey.ifBlank { "img_hero_promo" }.take(64))
        imageUrl?.takeIf { it.isNotBlank() }?.let { put("imageUrl", it.trim().take(500)) }
        put("featured", featured)
        put("active", active)
        put("highlights", highlights.take(6))
        if (isCreate) {
            put("createdAt", FieldValue.serverTimestamp())
        }
        put("updatedAt", FieldValue.serverTimestamp())
    }

    fun defaultVariants(): List<String> = when (category) {
        ProductCategory.CLOTHES.displayName -> listOf("S", "M", "L", "XL")
        ProductCategory.SHOES.displayName -> listOf("US 7", "US 8", "US 9", "US 10", "US 11")
        ProductCategory.SMARTPHONES.displayName -> listOf("128GB", "256GB", "512GB")
        ProductCategory.FACE_WASHES.displayName,
        ProductCategory.SKINCARE.displayName,
        ProductCategory.HAIRCARE.displayName -> listOf("100ml", "200ml")
        else -> listOf("Standard")
    }
}

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val title: String = "",
    val brand: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val discountPercent: Long = 0L,
    val imageKey: String = "img_hero_promo",
    val imageUrl: String? = null,
    val quantity: Long = 1L,
    val selectedVariant: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("productId", productId)
        put("title", title.take(120))
        put("brand", brand.take(80))
        put("category", category.take(60))
        put("price", price)
        put("originalPrice", originalPrice)
        put("discountPercent", discountPercent.coerceIn(0L, 95L))
        put("imageKey", imageKey.take(64))
        imageUrl?.takeIf { it.isNotBlank() }?.let { put("imageUrl", it.take(500)) }
        put("quantity", quantity.coerceIn(1L, 99L))
        selectedVariant?.takeIf { it.isNotBlank() }?.let { put("selectedVariant", it.take(40)) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val title: String = "",
    val brand: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val discountPercent: Long = 0L,
    val rating: Double = 4.5,
    val imageKey: String = "img_hero_promo",
    val imageUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("productId", productId)
        put("title", title.take(120))
        put("brand", brand.take(80))
        put("category", category.take(60))
        put("price", price)
        put("originalPrice", originalPrice)
        put("discountPercent", discountPercent.coerceIn(0L, 95L))
        put("rating", rating.coerceIn(0.0, 5.0))
        put("imageKey", imageKey.take(64))
        imageUrl?.takeIf { it.isNotBlank() }?.let { put("imageUrl", it.take(500)) }
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class OrderItem(
    val productId: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val quantity: Long = 1L,
    val imageKey: String = "img_hero_promo",
    val selectedVariant: String? = null
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("productId", productId)
        put("title", title.take(120))
        put("price", price)
        put("quantity", quantity.coerceIn(1L, 99L))
        put("imageKey", imageKey.take(64))
        selectedVariant?.takeIf { it.isNotBlank() }?.let { put("selectedVariant", it.take(40)) }
    }
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val orderNumber: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val shippingFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = PaymentMethod.UPI.wireValue,
    val paymentDetails: String = "",
    val paymentStatus: String = PaymentStatus.PAID.wireValue,
    val shippingAddress: String = "",
    val shippingPhone: String = "",
    val status: String = OrderStatus.PLACED.wireValue,
    val trackingNumber: String = "",
    val estimatedDelivery: String = "",
    val returnReason: String? = null,
    val refundAmount: Double? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("customerName", customerName.ifBlank { "Buyzora Customer" }.take(80))
        put("customerEmail", customerEmail.ifBlank { "customer@buyzora.app" }.take(160))
        put("orderNumber", orderNumber.take(40))
        put("items", items.take(20).map { it.toMap() })
        put("subtotal", subtotal)
        put("discountAmount", discountAmount)
        put("shippingFee", shippingFee)
        put("totalAmount", totalAmount)
        put("paymentMethod", paymentMethod)
        put("paymentDetails", paymentDetails.take(120))
        put("paymentStatus", paymentStatus)
        put("shippingAddress", shippingAddress.take(300))
        put("shippingPhone", shippingPhone.take(25))
        put("status", OrderStatus.PLACED.wireValue)
        put("trackingNumber", trackingNumber.take(60))
        put("estimatedDelivery", estimatedDelivery.take(80))
        returnReason?.takeIf { it.isNotBlank() }?.let { put("returnReason", it.take(300)) }
        refundAmount?.let { put("refundAmount", it) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val topic: String = TicketTopic.ORDER_ISSUE.label,
    val subject: String = "",
    val message: String = "",
    val orderNumber: String? = null,
    val status: String = TicketStatus.OPEN.wireValue,
    val adminReply: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("customerName", customerName.ifBlank { "Buyzora Customer" }.take(80))
        put("customerEmail", customerEmail.ifBlank { "customer@buyzora.app" }.take(160))
        put("topic", topic)
        put("subject", subject.trim().take(120))
        put("message", message.trim().take(1000))
        orderNumber?.takeIf { it.isNotBlank() }?.let { put("orderNumber", it.trim().take(40)) }
        put("status", TicketStatus.OPEN.wireValue)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

object StarterCatalog {
    val products: List<Product> = listOf(
        Product(
            id = "bz_cloth_01",
            name = "Atelier Linen Resort Shirt",
            brand = "Loom & Co.",
            category = ProductCategory.CLOTHES.displayName,
            description = "Tailored 100% breathable European flax linen shirt with mother-of-pearl buttons and relaxed drape.",
            price = 48.00,
            originalPrice = 68.00,
            discountPercent = 29,
            rating = 4.8,
            reviewCount = 318,
            stock = 42,
            imageKey = "img_cat_fashion",
            featured = true,
            active = true,
            highlights = listOf("100% European Flax Linen", "Pre-washed softness", "Breathable weave")
        ),
        Product(
            id = "bz_cloth_02",
            name = "Merino Knit Crewneck Pullover",
            brand = "Nordic Weave",
            category = ProductCategory.CLOTHES.displayName,
            description = "Ultra-fine extrafine merino wool pullover engineered for effortless layering across seasons.",
            price = 74.00,
            originalPrice = 98.00,
            discountPercent = 24,
            rating = 4.7,
            reviewCount = 194,
            stock = 28,
            imageKey = "img_cat_fashion",
            featured = false,
            active = true,
            highlights = listOf("19.5 Micron Merino", "Anti-pilling finish", "Machine washable")
        ),
        Product(
            id = "bz_shoe_01",
            name = "Court Leather Minimalist Sneakers",
            brand = "Stride Atelier",
            category = ProductCategory.SHOES.displayName,
            description = "Full-grain Italian calfskin sneakers with recycled OrthoLite memory foam footbed and stitched cupsole.",
            price = 112.00,
            originalPrice = 149.00,
            discountPercent = 25,
            rating = 4.9,
            reviewCount = 542,
            stock = 25,
            imageKey = "img_cat_fashion",
            featured = true,
            active = true,
            highlights = listOf("Full-grain calfskin", "OrthoLite insole", "Hand-stitched sole")
        ),
        Product(
            id = "bz_shoe_02",
            name = "AeroPulse Cloud Running Shoes",
            brand = "Veloce",
            category = ProductCategory.SHOES.displayName,
            description = "Responsive nitrogen-infused midsole runners with engineered breathable mesh upper for daily miles.",
            price = 94.00,
            originalPrice = 129.00,
            discountPercent = 27,
            rating = 4.6,
            reviewCount = 267,
            stock = 36,
            imageKey = "img_cat_fashion",
            featured = false,
            active = true,
            highlights = listOf("Nitrogen foam cushion", "238g ultralight", "Wet-grip outsole")
        ),
        Product(
            id = "bz_acc_01",
            name = "Riviera Polarized Acetate Sunglasses",
            brand = "Solis Optics",
            category = ProductCategory.FASHION_ACCESSORIES.displayName,
            description = "Hand-polished bio-acetate frames with UV400 polarized Carl Zeiss lenses and leather travel case.",
            price = 64.00,
            originalPrice = 89.00,
            discountPercent = 28,
            rating = 4.8,
            reviewCount = 185,
            stock = 30,
            imageKey = "img_hero_promo",
            featured = true,
            active = true,
            highlights = listOf("UV400 Polarized", "Bio-acetate frame", "Includes leather case")
        ),
        Product(
            id = "bz_acc_02",
            name = "Heritage Chronograph Steel Watch",
            brand = "Chronos Studio",
            category = ProductCategory.FASHION_ACCESSORIES.displayName,
            description = "Sapphire crystal 40mm stainless steel timepiece with sunray champagne dial and 5ATM water resistance.",
            price = 158.00,
            originalPrice = 219.00,
            discountPercent = 28,
            rating = 4.9,
            reviewCount = 112,
            stock = 14,
            imageKey = "img_cat_fashion",
            featured = false,
            active = true,
            highlights = listOf("Sapphire crystal", "316L Stainless Steel", "5ATM waterproof")
        ),
        Product(
            id = "bz_phone_01",
            name = "Aether Pro 5G Titanium (256GB)",
            brand = "Aether Mobile",
            category = ProductCategory.SMARTPHONES.displayName,
            description = "6.7-inch 120Hz LTPO OLED flagship with aerospace titanium frame, 50MP triple periscope camera, and all-day battery.",
            price = 899.00,
            originalPrice = 1049.00,
            discountPercent = 14,
            rating = 4.9,
            reviewCount = 812,
            stock = 18,
            imageKey = "img_cat_tech",
            featured = true,
            active = true,
            highlights = listOf("120Hz LTPO OLED", "50MP Periscope Cam", "65W Fast Charge")
        ),
        Product(
            id = "bz_phone_02",
            name = "Nova Lite 5G Smartphone",
            brand = "Aether Mobile",
            category = ProductCategory.SMARTPHONES.displayName,
            description = "Vibrant 6.5-inch AMOLED smartphone with OIS dual camera, 5000mAh battery, and clean OS experience.",
            price = 429.00,
            originalPrice = 499.00,
            discountPercent = 14,
            rating = 4.6,
            reviewCount = 390,
            stock = 32,
            imageKey = "img_cat_tech",
            featured = false,
            active = true,
            highlights = listOf("5000mAh Battery", "OIS 48MP Sensor", "Dual 5G SIM")
        ),
        Product(
            id = "bz_elec_01",
            name = "Studio ANC Wireless Headphones",
            brand = "Klang Audio",
            category = ProductCategory.ELECTRONICS.displayName,
            description = "40mm custom planar drivers with hybrid adaptive active noise cancellation and 45-hour USB-C battery life.",
            price = 189.00,
            originalPrice = 249.00,
            discountPercent = 24,
            rating = 4.8,
            reviewCount = 634,
            stock = 22,
            imageKey = "img_hero_promo",
            featured = true,
            active = true,
            highlights = listOf("Adaptive ANC -45dB", "45h Playback", "Multipoint Bluetooth 5.4")
        ),
        Product(
            id = "bz_elec_02",
            name = "Obsidian True Wireless Earbuds Pro",
            brand = "Klang Audio",
            category = ProductCategory.ELECTRONICS.displayName,
            description = "Spatial audio earbuds with Qi wireless charging case, IPX5 sweat resistance, and crystal-clear 6-mic calls.",
            price = 119.00,
            originalPrice = 159.00,
            discountPercent = 25,
            rating = 4.7,
            reviewCount = 478,
            stock = 40,
            imageKey = "img_cat_tech",
            featured = false,
            active = true,
            highlights = listOf("Spatial Audio", "Qi Wireless Case", "IPX5 Water Resistant")
        ),
        Product(
            id = "bz_gad_01",
            name = "Pulse Titanium Smartwatch Series 4",
            brand = "Aether Wear",
            category = ProductCategory.GADGETS.displayName,
            description = "Dual-band GPS fitness and health smartwatch with sapphire glass, ECG sensor, and 7-day battery autonomy.",
            price = 249.00,
            originalPrice = 319.00,
            discountPercent = 22,
            rating = 4.8,
            reviewCount = 295,
            stock = 19,
            imageKey = "img_cat_tech",
            featured = true,
            active = true,
            highlights = listOf("Dual-band GPS", "Sapphire Glass", "7-Day Battery")
        ),
        Product(
            id = "bz_gad_02",
            name = "MagDock 3-in-1 GaN Travel Charger",
            brand = "VoltCraft",
            category = ProductCategory.GADGETS.displayName,
            description = "Foldable magnetic Qi2 charging station for smartphone, watch, and earbuds powered by 65W GaN tech.",
            price = 54.00,
            originalPrice = 75.00,
            discountPercent = 28,
            rating = 4.7,
            reviewCount = 211,
            stock = 55,
            imageKey = "img_cat_tech",
            featured = false,
            active = true,
            highlights = listOf("Qi2 15W Magnetic", "Foldable Travel Form", "Surge Protected")
        ),
        Product(
            id = "bz_fw_01",
            name = "Ceramide Amino Gentle Face Wash",
            brand = "PureBotanica",
            category = ProductCategory.FACE_WASHES.displayName,
            description = "pH-balanced sulfate-free daily gel cleanser with 5 essential ceramides, green tea, and niacinamide.",
            price = 16.50,
            originalPrice = 22.00,
            discountPercent = 25,
            rating = 4.9,
            reviewCount = 920,
            stock = 80,
            imageKey = "img_cat_beauty",
            featured = true,
            active = true,
            highlights = listOf("pH 5.5 Balanced", "Fragrance & Sulfate Free", "Dermatologist Tested")
        ),
        Product(
            id = "bz_fw_02",
            name = "Salicylic & BHA Clarifying Cleanser",
            brand = "DermaLeaf",
            category = ProductCategory.FACE_WASHES.displayName,
            description = "2% encapsulated salicylic acid + zinc PCA foaming wash that unclogs pores without stripping skin barrier.",
            price = 18.00,
            originalPrice = 24.00,
            discountPercent = 25,
            rating = 4.7,
            reviewCount = 412,
            stock = 65,
            imageKey = "img_cat_beauty",
            featured = false,
            active = true,
            highlights = listOf("2% Encapsulated BHA", "Non-comedogenic", "Oil-control Zinc")
        ),
        Product(
            id = "bz_skin_01",
            name = "Vitamin C 15% + Ferulic Glow Serum",
            brand = "PureBotanica",
            category = ProductCategory.SKINCARE.displayName,
            description = "Stabilized L-Ascorbic Acid brightening serum with vitamin E and hyaluronic acid in UV-protective amber glass.",
            price = 34.00,
            originalPrice = 46.00,
            discountPercent = 26,
            rating = 4.9,
            reviewCount = 745,
            stock = 48,
            imageKey = "img_cat_beauty",
            featured = true,
            active = true,
            highlights = listOf("15% L-Ascorbic Acid", "0.5% Ferulic Acid", "Vegan & Cruelty-Free")
        ),
        Product(
            id = "bz_skin_02",
            name = "Barrier Repair Peptide Cloud Cream",
            brand = "DermaLeaf",
            category = ProductCategory.SKINCARE.displayName,
            description = "Weightless whipped moisturizer infused with squalane, panthenol B5, and biomimetic peptides for 48h hydration.",
            price = 29.00,
            originalPrice = 38.00,
            discountPercent = 24,
            rating = 4.8,
            reviewCount = 356,
            stock = 52,
            imageKey = "img_cat_beauty",
            featured = false,
            active = true,
            highlights = listOf("48h Moisture Lock", "Plant Squalane", "Sensitive Skin Safe")
        ),
        Product(
            id = "bz_hair_01",
            name = "Silk Keratin & Argan Repair Mask",
            brand = "BotaniSilk",
            category = ProductCategory.HAIRCARE.displayName,
            description = "Deep-conditioning botanical hair mask with cold-pressed Moroccan argan oil and plant keratin to restore shine.",
            price = 26.00,
            originalPrice = 35.00,
            discountPercent = 26,
            rating = 4.8,
            reviewCount = 289,
            stock = 44,
            imageKey = "img_cat_beauty",
            featured = true,
            active = true,
            highlights = listOf("Paraben & Silicone Free", "Color-Safe Formula", "Cold-Pressed Argan")
        ),
        Product(
            id = "bz_hair_02",
            name = "Rosemary & Biotin Scalp Tonic Shampoo",
            brand = "BotaniSilk",
            category = ProductCategory.HAIRCARE.displayName,
            description = "Invigorating botanical shampoo formulated with organic rosemary extract, biotin, and caffeine to boost volume.",
            price = 21.00,
            originalPrice = 28.00,
            discountPercent = 25,
            rating = 4.7,
            reviewCount = 330,
            stock = 60,
            imageKey = "img_cat_beauty",
            featured = false,
            active = true,
            highlights = listOf("Organic Rosemary", "Strengthening Biotin", "Microbiome Friendly")
        ),
        Product(
            id = "bz_ess_01",
            name = "Vacuum Insulated Matte Tumbler (750ml)",
            brand = "Kanso Living",
            category = ProductCategory.EVERYDAY_ESSENTIALS.displayName,
            description = "Double-wall 18/8 stainless steel insulated bottle keeping beverages cold 24 hours or piping hot 12 hours.",
            price = 28.00,
            originalPrice = 36.00,
            discountPercent = 22,
            rating = 4.9,
            reviewCount = 610,
            stock = 75,
            imageKey = "img_hero_promo",
            featured = true,
            active = true,
            highlights = listOf("24h Cold / 12h Hot", "BPA-Free Leakproof", "Cup-holder friendly")
        ),
        Product(
            id = "bz_ess_02",
            name = "Organic Bamboo Bath Towel Set (4-Pack)",
            brand = "Kanso Living",
            category = ProductCategory.EVERYDAY_ESSENTIALS.displayName,
            description = "650 GSM plush bamboo-cotton blend towels engineered for fast drying and hotel-spa softness.",
            price = 44.00,
            originalPrice = 59.00,
            discountPercent = 25,
            rating = 4.8,
            reviewCount = 274,
            stock = 38,
            imageKey = "img_cat_fashion",
            featured = false,
            active = true,
            highlights = listOf("650 GSM Plush", "OEKO-TEX Certified", "Quick-dry weave")
        )
    )
}
