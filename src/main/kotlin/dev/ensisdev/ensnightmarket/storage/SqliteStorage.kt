package dev.ensisdev.ensnightmarket.storage

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.market.*
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class SqliteStorage(private val plugin: EnsNightMarket) : Storage {
    private lateinit var connection: Connection

    override fun start() {
        open()
    }

    private fun dbFile() = File(plugin.dataFolder, "markets.db")

    private fun open() {
        plugin.dataFolder.mkdirs()
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile().absolutePath}")
        createTables(connection)
    }

    private fun ensureOpen() {
        if (!::connection.isInitialized || runCatching { connection.isClosed }.getOrDefault(true)) open()
    }

    private fun createTables(c: Connection) {
        c.createStatement().use { st ->
            st.executeUpdate("CREATE TABLE IF NOT EXISTS markets (uuid TEXT PRIMARY KEY, expires INTEGER NOT NULL)")
            st.executeUpdate("""CREATE TABLE IF NOT EXISTS market_offers (
                uuid TEXT NOT NULL, slot INTEGER NOT NULL, offer_id TEXT NOT NULL, rarity TEXT NOT NULL,
                price REAL NOT NULL, original_price REAL NOT NULL, discount INTEGER NOT NULL, stock INTEGER NOT NULL,
                revealed INTEGER NOT NULL, purchased INTEGER NOT NULL, purchases INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid, slot))""")
            runCatching { st.executeUpdate("ALTER TABLE market_offers ADD COLUMN purchases INTEGER NOT NULL DEFAULT 0") }
        }
    }

    override fun load(uuid: UUID, definitions: Map<String, OfferDef>, rarities: Map<String, RarityDef>): PlayerMarket? {
        ensureOpen()
        connection.prepareStatement("SELECT expires FROM markets WHERE uuid=?").use { ps ->
            ps.setString(1, uuid.toString())
            ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                val expires = rs.getLong(1)
                if (expires <= System.currentTimeMillis()) {
                    reset(uuid)
                    return null
                }
                val offers = mutableListOf<MarketOffer>()
                connection.prepareStatement("SELECT slot,offer_id,rarity,price,original_price,discount,stock,revealed,purchased,purchases FROM market_offers WHERE uuid=? ORDER BY slot").use { os ->
                    os.setString(1, uuid.toString())
                    os.executeQuery().use { or ->
                        while (or.next()) {
                            val def = definitions[or.getString("offer_id")] ?: continue
                            val rarity = rarities[or.getString("rarity")] ?: continue
                            offers += MarketOffer(uuid, or.getInt("slot"), def, rarity, or.getDouble("price"), or.getDouble("original_price"), or.getInt("discount"), or.getInt("stock"), or.getInt("revealed") == 1, or.getInt("purchased") == 1, or.getInt("purchases"))
                        }
                    }
                }
                return if (offers.isEmpty()) null else PlayerMarket(uuid, expires, offers)
            }
        }
    }

    @Synchronized override fun save(market: PlayerMarket) {
        ensureOpen()
        connection.autoCommit = false
        try {
            connection.prepareStatement("INSERT OR REPLACE INTO markets(uuid,expires) VALUES(?,?)").use { ps ->
                ps.setString(1, market.player.toString()); ps.setLong(2, market.expiresAt); ps.executeUpdate()
            }
            connection.prepareStatement("DELETE FROM market_offers WHERE uuid=?").use { ps ->
                ps.setString(1, market.player.toString()); ps.executeUpdate()
            }
            connection.prepareStatement("INSERT INTO market_offers(uuid,slot,offer_id,rarity,price,original_price,discount,stock,revealed,purchased,purchases) VALUES(?,?,?,?,?,?,?,?,?,?,?)").use { ps ->
                market.offers.forEach { o ->
                    ps.setString(1, market.player.toString()); ps.setInt(2, o.slot); ps.setString(3, o.definition.id); ps.setString(4, o.rarity.id)
                    ps.setDouble(5, o.price); ps.setDouble(6, o.originalPrice); ps.setInt(7, o.discount); ps.setInt(8, o.stock)
                    ps.setInt(9, if (o.revealed) 1 else 0); ps.setInt(10, if (o.purchased) 1 else 0); ps.setInt(11, o.purchases); ps.addBatch()
                }
                ps.executeBatch()
            }
            connection.commit()
        } catch (t: Throwable) {
            runCatching { connection.rollback() }
            throw t
        } finally { connection.autoCommit = true }
    }

    @Synchronized override fun reset(uuid: UUID) {
        if (!::connection.isInitialized || connection.isClosed) return
        connection.prepareStatement("DELETE FROM market_offers WHERE uuid=?").use { it.setString(1, uuid.toString()); it.executeUpdate() }
        connection.prepareStatement("DELETE FROM markets WHERE uuid=?").use { it.setString(1, uuid.toString()); it.executeUpdate() }
    }

    @Synchronized override fun resetAll() {
        if (!::connection.isInitialized || connection.isClosed) return
        connection.createStatement().use { it.executeUpdate("DELETE FROM market_offers") }
        connection.createStatement().use { it.executeUpdate("DELETE FROM markets") }
    }

    override fun close() { if (::connection.isInitialized && !connection.isClosed) connection.close() }
}
