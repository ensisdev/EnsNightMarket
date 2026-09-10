package dev.ensisdev.ensnightmarket.admin

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.UUID

enum class AdminMenu {
    MAIN, OFFERS, OFFER_EDIT, RARITIES, RARITY_EDIT, RARITY_SELECT, DELETE_CONFIRM, SETTINGS, PLAYER_MARKET
}

class AdminHolder(
    val menu: AdminMenu,
    val page: Int = 0,
    val id: String = "",
    val target: UUID? = null
) : InventoryHolder {
    private lateinit var inventory: Inventory
    fun bind(inventory: Inventory) { this.inventory = inventory }
    override fun getInventory(): Inventory = inventory
}
