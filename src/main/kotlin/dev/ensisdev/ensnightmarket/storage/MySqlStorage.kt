package dev.ensisdev.ensnightmarket.storage

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.market.*
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class MySqlStorage(private val plugin: EnsNightMarket) : Storage {
    private lateinit var connection: Connection

    override fun start() {
        connect()
    }

    private fun ensureOpen() {
        val alive = ::connection.isInitialized && runCatching { connection.isValid(2) }.getOrDefault(false)
        if (!alive) {
            runCatching { if (::connection.isInitialized) connection.close() }
            connect()
        }
    }

    private fun connect() {
        val c = plugin.config
        val host = c.getString("storage.mysql.host", "localhost")!!
        val port = c.getInt("storage.mysql.port", 3306)
        val database = c.getString("storage.mysql.database", "ensnightmarket")!!
        val user = c.getString("storage.mysql.username", "root")!!
        val password = c.getString("storage.mysql.password", "change-me")!!
        connection = DriverManager.getConnection("jdbc:mysql://$host:$port/$database?useSSL=false&characterEncoding=utf8&autoReconnect=true", user, password)
        connection.createStatement().use { st ->
            st.executeUpdate("CREATE TABLE IF NOT EXISTS ensnm_markets (uuid VARCHAR(36) PRIMARY KEY, expires BIGINT NOT NULL)")
            st.executeUpdate("""CREATE TABLE IF NOT EXISTS ensnm_offers (
                uuid VARCHAR(36) NOT NULL, slot INT NOT NULL, offer_id VARCHAR(128) NOT NULL, rarity VARCHAR(128) NOT NULL,
                price DOUBLE NOT NULL, original_price DOUBLE NOT NULL, discount INT NOT NULL, stock INT NOT NULL,
                revealed BOOLEAN NOT NULL, purchased BOOLEAN NOT NULL, purchases INT NOT NULL DEFAULT 0, PRIMARY KEY(uuid,slot))""")
            runCatching { st.executeUpdate("ALTER TABLE ensnm_offers ADD COLUMN purchases INT NOT NULL DEFAULT 0") }
        }
    }

    override fun load(uuid: UUID, definitions: Map<String, OfferDef>, rarities: Map<String, RarityDef>): PlayerMarket? {
        ensureOpen()
        connection.prepareStatement("SELECT expires FROM ensnm_markets WHERE uuid=?").use { ps ->
            ps.setString(1, uuid.toString()); ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                val expires = rs.getLong(1)
                if (expires <= System.currentTimeMillis()) { reset(uuid); return null }
                val offers = mutableListOf<MarketOffer>()
                connection.prepareStatement("SELECT slot,offer_id,rarity,price,original_price,discount,stock,revealed,purchased,purchases FROM ensnm_offers WHERE uuid=? ORDER BY slot").use { os ->
                    os.setString(1, uuid.toString()); os.executeQuery().use { r ->
                        while (r.next()) {
                            val def = definitions[r.getString("offer_id")] ?: continue
                            val rarity = rarities[r.getString("rarity")] ?: continue
                            offers += MarketOffer(uuid, r.getInt("slot"), def, rarity, r.getDouble("price"), r.getDouble("original_price"), r.getInt("discount"), r.getInt("stock"), r.getBoolean("revealed"), r.getBoolean("purchased"), r.getInt("purchases"))
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
            connection.prepareStatement("INSERT INTO ensnm_markets(uuid,expires) VALUES(?,?) ON DUPLICATE KEY UPDATE expires=VALUES(expires)").use { ps ->
                ps.setString(1, market.player.toString()); ps.setLong(2, market.expiresAt); ps.executeUpdate()
            }
            connection.prepareStatement("DELETE FROM ensnm_offers WHERE uuid=?").use { ps -> ps.setString(1, market.player.toString()); ps.executeUpdate() }
            connection.prepareStatement("INSERT INTO ensnm_offers(uuid,slot,offer_id,rarity,price,original_price,discount,stock,revealed,purchased,purchases) VALUES(?,?,?,?,?,?,?,?,?,?,?)").use { ps ->
                market.offers.forEach { o ->
                    ps.setString(1, market.player.toString()); ps.setInt(2, o.slot); ps.setString(3, o.definition.id); ps.setString(4, o.rarity.id)
                    ps.setDouble(5, o.price); ps.setDouble(6, o.originalPrice); ps.setInt(7, o.discount); ps.setInt(8, o.stock)
                    ps.setBoolean(9, o.revealed); ps.setBoolean(10, o.purchased); ps.setInt(11, o.purchases); ps.addBatch()
                }
                ps.executeBatch()
            }
            connection.commit()
        } catch (t: Throwable) { runCatching { connection.rollback() }; throw t } finally { connection.autoCommit = true }
    }

    @Synchronized override fun reset(uuid: UUID) {
        ensureOpen()
        connection.prepareStatement("DELETE FROM ensnm_offers WHERE uuid=?").use { it.setString(1, uuid.toString()); it.executeUpdate() }
        connection.prepareStatement("DELETE FROM ensnm_markets WHERE uuid=?").use { it.setString(1, uuid.toString()); it.executeUpdate() }
    }
    @Synchronized override fun resetAll() { ensureOpen(); connection.createStatement().use { it.executeUpdate("DELETE FROM ensnm_offers") }; connection.createStatement().use { it.executeUpdate("DELETE FROM ensnm_markets") } }
    override fun close() { if (::connection.isInitialized && !connection.isClosed) connection.close() }
}
