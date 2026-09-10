package dev.ensisdev.ensnightmarket.economy

import dev.ensisdev.ensnightmarket.EnsNightMarket
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Reflection-only Vault entegrasyonu: Vault kurulu olmayan sunucuda
 * NoClassDefFoundError fýrlatmaz. PlayerPoints zaten reflection ile.
 */
class EconomyManager(private val plugin: EnsNightMarket) {
    enum class Provider { VAULT, PLAYER_POINTS, NONE }
    private var vault: Any? = null
    private var pointsApi: Any? = null
    private var provider = Provider.NONE

    init { refresh() }

    fun refresh() {
        vault = null; pointsApi = null; provider = Provider.NONE
        when ((plugin.config.getString("economy.provider", "VAULT") ?: "VAULT").uppercase()) {
            "VAULT" -> {
                vault = resolveVault()
                if (vault != null) provider = Provider.VAULT
            }
            "PLAYER_POINTS" -> {
                val pp = Bukkit.getPluginManager().getPlugin("PlayerPoints")
                pointsApi = runCatching { pp?.javaClass?.methods?.firstOrNull { it.name == "getAPI" && it.parameterCount == 0 }?.invoke(pp) }.getOrNull()
                if (pointsApi != null) provider = Provider.PLAYER_POINTS
            }
        }
    }

    private fun resolveVault(): Any? = runCatching {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null
        val economyClass = Class.forName("net.milkbowl.vault.economy.Economy")
        val reg = Bukkit.getServicesManager().getRegistration(economyClass) ?: return null
        reg.provider
    }.getOrNull()

    fun providerName() = provider.name
    fun available() = provider != Provider.NONE
    fun balance(player: Player): Double = balance(player.uniqueId)
    fun balance(uuid: UUID): Double = when (provider) {
        Provider.VAULT -> vaultBalance(Bukkit.getOfflinePlayer(uuid))
        Provider.PLAYER_POINTS -> pointsLook(uuid).toDouble()
        Provider.NONE -> 0.0
    }
    fun has(player: Player, amount: Double) = balance(player) + 1.0E-9 >= amount

    fun withdraw(player: Player, amount: Double): Boolean {
        if (amount < 0 || !has(player, amount)) return false
        return when (provider) {
            Provider.VAULT -> vaultWithdraw(player, amount)
            Provider.PLAYER_POINTS -> pointsTake(player.uniqueId, amount.toInt())
            Provider.NONE -> false
        }
    }

    fun deposit(uuid: UUID, amount: Double): Boolean {
        if (amount < 0) return false
        return when (provider) {
            Provider.VAULT -> vaultDeposit(Bukkit.getOfflinePlayer(uuid), amount)
            Provider.PLAYER_POINTS -> pointsGive(uuid, amount.toInt())
            Provider.NONE -> false
        }
    }

    private fun vaultBalance(target: OfflinePlayer): Double = runCatching {
        val v = vault ?: return 0.0
        v.javaClass.getMethod("getBalance", OfflinePlayer::class.java).invoke(v, target) as? Number ?: 0.0
    }.getOrDefault(0.0).toDouble()

    private fun vaultWithdraw(player: Player, amount: Double): Boolean = runCatching {
        val v = vault ?: return false
        val resp = v.javaClass.getMethod("withdrawPlayer", Player::class.java, Double::class.javaPrimitiveType)
            .invoke(v, player, amount)
        resp.javaClass.getMethod("transactionSuccess").invoke(resp) as? Boolean ?: false
    }.getOrDefault(false)

    private fun vaultDeposit(target: OfflinePlayer, amount: Double): Boolean = runCatching {
        val v = vault ?: return false
        val resp = v.javaClass.getMethod("depositPlayer", OfflinePlayer::class.java, Double::class.javaPrimitiveType)
            .invoke(v, target, amount)
        resp.javaClass.getMethod("transactionSuccess").invoke(resp) as? Boolean ?: false
    }.getOrDefault(false)

    fun format(amount: Double): String = if (provider == Provider.PLAYER_POINTS) {
        dev.ensisdev.ensnightmarket.lang.Lang.get("economy-points", mapOf("%n%" to "${amount.toInt()}"))
    } else {
        "%.2f".format(amount)
    }

    private fun pointsLook(uuid: UUID): Int = runCatching {
        pointsApi?.javaClass?.methods?.firstOrNull { it.name == "look" && it.parameterCount == 1 }?.invoke(pointsApi, uuid) as? Number ?: 0
    }.getOrDefault(0).toInt()

    private fun pointsTake(uuid: UUID, amount: Int): Boolean = runCatching {
        val m = pointsApi?.javaClass?.methods?.firstOrNull { it.name == "take" && it.parameterCount == 2 } ?: return false
        (m.invoke(pointsApi, uuid, amount) as? Boolean) ?: false
    }.getOrDefault(false)

    private fun pointsGive(uuid: UUID, amount: Int): Boolean = runCatching {
        val m = pointsApi?.javaClass?.methods?.firstOrNull { it.name == "give" && it.parameterCount == 2 } ?: return false
        (m.invoke(pointsApi, uuid, amount) as? Boolean) ?: false
    }.getOrDefault(false)
}
