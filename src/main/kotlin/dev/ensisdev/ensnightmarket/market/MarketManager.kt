package dev.ensisdev.ensnightmarket.market

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.storage.Storage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.round
import kotlin.random.Random

class MarketManager(private val plugin: EnsNightMarket, private var storage: Storage) {
    val rarityDefinitions = ConcurrentHashMap<String, RarityDef>()
    val offerDefinitions = ConcurrentHashMap<String, OfferDef>()
    private val markets = ConcurrentHashMap<UUID, PlayerMarket>()
    private val locks = ConcurrentHashMap.newKeySet<UUID>()

    @Synchronized fun reloadStorage(newStorage: Storage) {
        storage = newStorage
        markets.clear()
    }

    @Synchronized fun loadDefinitions() {
        rarityDefinitions.clear(); offerDefinitions.clear()
        plugin.configs.rarities.getConfigurationSection("rarities")?.getKeys(false)?.forEach { id ->
            val p = plugin.configs.rarities.getConfigurationSection("rarities.$id") ?: return@forEach
            val e = p.getConfigurationSection("effects")
            rarityDefinitions[id] = RarityDef(
                id = id,
                displayName = p.getString("display-name", id)!!,
                weight = p.getDouble("weight", 1.0),
                color = p.getString("color", "#FFFFFF")!!,
                headType = p.getString("head.type", "TEXTURE_VALUE")!!,
                headValue = p.getString("head.value", "")!!,
                particle = e?.getString("particle", "END_ROD") ?: "END_ROD",
                particleCount = e?.getInt("particle-count", 3) ?: 3,
                sound = e?.getString("sound", "BLOCK_NOTE_BLOCK_PLING") ?: "BLOCK_NOTE_BLOCK_PLING",
                pitch = e?.getDouble("pitch", 1.0)?.toFloat() ?: 1f,
                floatingScale = p.getDouble("floating.scale", 1.55).toFloat(),
                aura = p.getBoolean("floating.aura", true)
            )
        }
        plugin.configs.offers.getConfigurationSection("offers")?.getKeys(false)?.forEach { id ->
            val p = plugin.configs.offers.getConfigurationSection("offers.$id") ?: return@forEach
            val material = p.getString("material", "STONE")!!
            if (dev.ensisdev.ensnightmarket.items.CustomItems.knownPrefix(material) &&
                dev.ensisdev.ensnightmarket.items.CustomItems.resolve(material, 1) == null) {
                plugin.logger.warning("Offer '$id' skipped: custom item '$material' could not be resolved. Is the provider plugin installed?")
                return@forEach
            }
            offerDefinitions[id] = OfferDef(
                id, p.getString("display-name", id)!!, material,
                p.getInt("amount", 1).coerceIn(1, 64), p.getDouble("base-price", 100.0).coerceAtLeast(0.0),
                p.getInt("discount-min", 10), p.getInt("discount-max", 50), p.getInt("stock-min", 1).coerceAtLeast(0),
                p.getInt("stock-max", 2).coerceAtLeast(p.getInt("stock-min", 1)),
                p.getString("rarity", "random")!!, p.getString("permission"), p.getInt("max-purchases", 0),
                p.getStringList("lore"), p.getBoolean("enabled", true)
            )
        }
    }

    @Synchronized fun getOrCreate(uuid: UUID): PlayerMarket {
        markets[uuid]?.let { if (it.expiresAt > System.currentTimeMillis()) return it }
        storage.load(uuid, offerDefinitions, rarityDefinitions)?.let { markets[uuid] = it; return it }
        return refresh(uuid)
    }

    fun generate(uuid: UUID): PlayerMarket {
        val pool = offerDefinitions.values.filter { it.enabled }
        check(pool.isNotEmpty()) { "No enabled offers configured in offers.yml" }
        check(rarityDefinitions.isNotEmpty()) { "No rarities configured in rarities.yml" }
        val now = System.currentTimeMillis()
        val hours = plugin.config.getLong("market.refresh-hours", 72).coerceAtLeast(1)
        val count = plugin.config.getInt("market.slots", 6).coerceIn(1, 54)
        val unique = plugin.config.getBoolean("market.unique-offers", true)
        val available = pool.toMutableList()
        val offers = mutableListOf<MarketOffer>()
        repeat(count) { slot ->
            val def = if (unique && available.isNotEmpty()) available.removeAt(Random.nextInt(available.size)) else pool.random()
            val rarity = if (def.rarity.equals("random", true)) chooseWeightedRarity() else rarityDefinitions[def.rarity] ?: chooseWeightedRarity()
            val minD = def.discountMin.coerceIn(0, 100)
            val maxD = def.discountMax.coerceIn(minD, 100)
            val discount = Random.nextInt(minD, maxD + 1)
            val price = round(def.basePrice * (100 - discount) / 100.0 * 100) / 100.0
            val stock = Random.nextInt(def.stockMin.coerceAtLeast(0), def.stockMax.coerceAtLeast(def.stockMin) + 1)
            offers += MarketOffer(uuid, slot, def, rarity, price, def.basePrice, discount, stock)
        }
        return PlayerMarket(uuid, now + hours * 3_600_000L, offers)
    }

    @Synchronized fun refresh(uuid: UUID): PlayerMarket {
        val m = generate(uuid)
        markets[uuid] = m
        storage.save(m)
        return m
    }

    @Synchronized fun reset(uuid: UUID) { markets.remove(uuid); storage.reset(uuid) }
    @Synchronized fun resetAll() { markets.clear(); storage.resetAll() }
    fun cached(uuid: UUID): PlayerMarket? = markets[uuid]

    @Synchronized fun reveal(uuid: UUID, slot: Int): MarketOffer? {
        val m = getOrCreate(uuid); val o = m.offers.getOrNull(slot) ?: return null
        if (!o.revealed) { o.revealed = true; save(m) }
        return o
    }

    @Synchronized fun purchase(uuid: UUID, slot: Int, playerHasPermission: (String) -> Boolean, charge: (Double) -> Boolean, give: (ItemStack) -> Boolean): PurchaseResult {
        if (!locks.add(uuid)) return PurchaseResult(false, "busy")
        try {
            val market = getOrCreate(uuid)
            val offer = market.offers.getOrNull(slot) ?: return PurchaseResult(false, "not-found")
            if (!offer.revealed) return PurchaseResult(false, "not-revealed")
            if (offer.stock <= 0) return PurchaseResult(false, "sold-out")
            offer.definition.permission?.takeIf { it.isNotBlank() }?.let { if (!playerHasPermission(it)) return PurchaseResult(false, "permission") }
            if (offer.definition.maxPurchases > 0 && offer.purchases >= offer.definition.maxPurchases) return PurchaseResult(false, "limit")
            val stack = createReward(offer)
            if (!giveFits(stack, uuid)) return PurchaseResult(false, "inventory")
            if (!charge(offer.price)) return PurchaseResult(false, "insufficient")
            if (!give(stack)) {
                plugin.economy.deposit(uuid, offer.price)
                return PurchaseResult(false, "inventory")
            }
            offer.stock--
            offer.purchases++
            if (offer.stock <= 0) offer.purchased = true
            save(market)
            return PurchaseResult(true)
        } finally { locks.remove(uuid) }
    }

    private fun giveFits(stack: ItemStack, uuid: UUID): Boolean {
        val player = plugin.server.getPlayer(uuid) ?: return false
        var remaining = stack.amount
        player.inventory.storageContents.forEach { existing ->
            if (existing == null) return@forEach
            if (!existing.isSimilar(stack)) return@forEach
            remaining -= (existing.maxStackSize - existing.amount).coerceAtLeast(0)
        }
        val empty = player.inventory.storageContents.count { it == null }
        remaining -= empty * stack.maxStackSize
        return remaining <= 0
    }

    fun createReward(o: MarketOffer): ItemStack {
        val base = dev.ensisdev.ensnightmarket.items.CustomItems.resolve(o.definition.material, o.definition.amount)
            ?: ItemStack(Material.STONE)
        val item = base.clone()
        item.amount = o.definition.amount.coerceIn(1, item.maxStackSize.coerceAtLeast(1))
        val meta = item.itemMeta
        if (meta != null) {
            val T = dev.ensisdev.ensnightmarket.util.Text
            val C = dev.ensisdev.ensnightmarket.util.Compat
            C.setName(meta, T.component(o.definition.displayName), T.legacy(o.definition.displayName))
            val lore = o.definition.lore.toMutableList()
            lore += ""
            lore += dev.ensisdev.ensnightmarket.lang.Lang.get("reward-price", mapOf("%price%" to plugin.economy.format(o.price), "%discount%" to "${o.discount}"))
            lore += dev.ensisdev.ensnightmarket.lang.Lang.get("reward-brand")
            C.setLore(meta, lore.map(T::component), lore.map(T::legacy))
            meta.persistentDataContainer.set(NamespacedKey(plugin, "ensnm_offer"), PersistentDataType.STRING, o.definition.id)
            item.itemMeta = meta
        }
        return item
    }

    private fun chooseWeightedRarity(): RarityDef {
        val valid = rarityDefinitions.values.filter { it.weight > 0.0 }
        if (valid.isEmpty()) return rarityDefinitions.values.first()
        var roll = Random.nextDouble(valid.sumOf { it.weight })
        for (r in valid) { roll -= r.weight; if (roll <= 0) return r }
        return valid.last()
    }

    @Synchronized fun save(market: PlayerMarket) { markets[market.player] = market; storage.save(market) }

    @Synchronized fun dropCache() { markets.clear() }
}
