# Installation

## 1. Download

Get `EnsNightMarket-2.1.0.jar` from your build (`mvn clean package` produces
`target/EnsNightMarket-2.1.0.jar`) or from your distribution channel.

## 2. Server Requirements

| Software | Notes |
| --- | --- |
| **Paper 1.20+** | Recommended. Spigot-compatible fallbacks are built in; Folia is supported. |
| **Java 17+** | Required by modern Paper builds. |

## 3. Economy Provider

EnsNightMarket needs **one** economy backend. Pick one:

### Option A - Vault (recommended)

1. Install [Vault](https://www.spigotmc.org/resources/vault.34315/).
2. Install a Vault economy (EssentialsX Economy, CMI, XConomy, etc.).
3. Keep `economy.provider: VAULT` in `config.yml` (default).

### Option B - PlayerPoints

1. Install [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/).
2. Set `economy.provider: PLAYER_POINTS` in `config.yml`.

> If no economy is found, the plugin still starts but logs a warning and all
> purchases fail until an economy is available.

## 4. Optional Integrations

All of these are detected automatically via reflection - install any of them and
the matching feature just works:

| Plugin | Unlocks |
| --- | --- |
| PlaceholderAPI | `%ensnightmarket_...%` placeholders |
| WorldGuard | Region black/whitelist + `ensnightmarket` flag |
| Oraxen | `oraxen:<item_id>` rewards |
| ItemsAdder | `itemsadder:<item_id>` (or `ia:`) rewards |
| Nexo | `nexo:<item_id>` rewards |
| MMOItems | `mmoitems:<TYPE>:<item_id>` rewards |
| HeadDatabase | `hdb:<head_id>` custom heads |

## 5. Install the Plugin

1. Drop the jar into your server's `plugins/` folder.
2. Restart the server (a full restart is safer than `/reload`).
3. On first start the plugin creates:

```
plugins/EnsNightMarket/
  config.yml      # main settings
  rarities.yml    # rarity definitions
  offers.yml      # offer definitions
  commands.yml    # command layout
  lang/
    tr_tr.yml     # Turkish messages (fallback)
    en_en.yml     # English messages
  markets.db      # SQLite storage (default)
```

Missing keys in an existing `config.yml` are auto-merged from defaults on start.

## 6. Verify

In the console you should see:

```
EnsNightMarket 2.1.0 enabled - 6 offers - 5 rarities - VAULT
Advanced animation system enabled with LOD support.
```

Then join and run `/nightmarket`.

## Troubleshooting Install

| Symptom | Fix |
| --- | --- |
| `No usable economy provider found` | Install Vault + an economy, or PlayerPoints, and set `economy.provider` accordingly. |
| Offer skipped warning `custom item could not be resolved` | The provider plugin (Oraxen/IA/Nexo/MMOItems) is missing or the item ID is wrong. |
| Heads render as plain player heads | Texture value is invalid; see [Custom Heads](custom-heads.md). |
| Purchases always fail | Check `economy.provider` matches what you installed. |
