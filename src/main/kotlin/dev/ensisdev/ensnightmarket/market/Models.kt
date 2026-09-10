package dev.ensisdev.ensnightmarket.market

import java.util.UUID

data class RarityDef(
    val id: String,
    val displayName: String,
    val weight: Double,
    val color: String,
    val headType: String,
    val headValue: String,
    val particle: String,
    val particleCount: Int,
    val sound: String,
    val pitch: Float,
    val floatingScale: Float = 1.55f,
    val aura: Boolean = true
)

data class OfferDef(
    val id: String,
    val displayName: String,
    val material: String,
    val amount: Int,
    val basePrice: Double,
    val discountMin: Int,
    val discountMax: Int,
    val stockMin: Int,
    val stockMax: Int,
    val rarity: String,
    val permission: String? = null,
    val maxPurchases: Int = 0,
    val lore: List<String> = emptyList(),
    val enabled: Boolean = true
)

data class MarketOffer(
    val player: UUID,
    val slot: Int,
    val definition: OfferDef,
    val rarity: RarityDef,
    val price: Double,
    val originalPrice: Double,
    val discount: Int,
    var stock: Int,
    var revealed: Boolean = false,
    var purchased: Boolean = false,
    var purchases: Int = 0
)

data class PlayerMarket(val player: UUID, val expiresAt: Long, val offers: MutableList<MarketOffer>)

data class PurchaseResult(val success: Boolean, val reason: String? = null)
