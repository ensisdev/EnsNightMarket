package dev.ensisdev.ensnightmarket.storage

import dev.ensisdev.ensnightmarket.market.OfferDef
import dev.ensisdev.ensnightmarket.market.PlayerMarket
import dev.ensisdev.ensnightmarket.market.RarityDef
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

interface Storage : AutoCloseable {
    fun start()
    fun load(uuid: UUID, definitions: Map<String, OfferDef>, rarities: Map<String, RarityDef>): PlayerMarket?
    fun save(market: PlayerMarket)
    fun reset(uuid: UUID)
    fun resetAll()
}

class AsyncStorage(private val delegate: Storage, private val plugin: JavaPlugin) : Storage {
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "EnsNightMarket-Storage").apply { isDaemon = true }
    }

    override fun start() {
        CompletableFuture.runAsync({ delegate.start() }, executor).join()
    }

    override fun load(uuid: UUID, definitions: Map<String, OfferDef>, rarities: Map<String, RarityDef>): PlayerMarket? {
        return CompletableFuture.supplyAsync({ delegate.load(uuid, definitions, rarities) }, executor).join()
    }

    override fun save(market: PlayerMarket) {
        CompletableFuture.runAsync({
            runCatching { delegate.save(market) }
                .onFailure { plugin.logger.warning("Market save failed for ${market.player}: ${it.message}") }
        }, executor)
    }

    override fun reset(uuid: UUID) {
        CompletableFuture.runAsync({
            runCatching { delegate.reset(uuid) }
                .onFailure { plugin.logger.warning("Market reset failed for $uuid: ${it.message}") }
        }, executor)
    }

    override fun resetAll() {
        CompletableFuture.runAsync({
            runCatching { delegate.resetAll() }
                .onFailure { plugin.logger.warning("Market resetAll failed: ${it.message}") }
        }, executor)
    }

    override fun close() {
        executor.shutdown()
        runCatching { executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS) }
        delegate.close()
    }
}
