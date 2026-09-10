# EnsNightMarket 2.1.0

A **per-player floating Night Market** for Paper, written in Kotlin. Type
`/nightmarket` and six floating heads appear in an arc in front of you —
visible only to you. **Left-click** reveals the reward, **right-click** buys it.

## Why EnsNightMarket

- **Truly personal** — every player rolls their own prices, discounts and
  stock on a persistent timer that survives restarts.
- **Cinematic by default** — staggered spawn ceremony, elastic reveal with
  particle shapes and ascending note ladders, idle auras, purchase confetti.
- **Five rarities out of the box** — Yaygın, Sıradışı, Nadir, Efsanevi,
  Gizemli — with weighted odds and unlimited custom rarities.
- **Server-owner friendly** — full in-game admin GUI, data-driven
  `commands.yml`, Vault or PlayerPoints, SQLite or MySQL, 18 PlaceholderAPI
  placeholders, WorldGuard regions.

## Quick Links

- [Introduction](docs/introduction.md) — what it is and how it works
- [Installation](docs/installation.md) — jar, dependencies, first start
- [Quick Start](docs/quick-start.md) — your first market in 5 minutes
- [Commands](docs/commands.md) / [Permissions](docs/permissions.md)
- [config.yml](docs/configuration/config-yml.md) /
  [rarities](docs/configuration/rarities.md) /
  [offers](docs/configuration/offers.md) /
  [commands.yml](docs/configuration/commands-yml.md) /
  [Languages](docs/configuration/languages.md)
- [Admin GUI](docs/admin-gui.md) / [Animation System](docs/animation-system.md) /
  [Performance](docs/performance.md) / [Building](docs/building.md) / [FAQ](docs/faq.md)

## Commands

`/nightmarket` (aliases: `enm`, `gecepazari`, `gece-pazari`, `night-market`)

| Command | Description |
| --- | --- |
| `/nightmarket` | Open your market (`open` / `ac`) |
| `/nightmarket close` (`kapat`) | Close your market |
| `/nightmarket refresh` (`yenile`) | Paid reroll (price + cooldown) |
| `/nightmarket admin [player]` | Admin GUI / inspect a player's market |
| `/nightmarket admin refresh <player>` | Free force-reroll |
| `/nightmarket admin reset <player>` | Full market wipe |
| `/nightmarket admin reload` | Reload everything |
| `/nightmarket admin lang [code]` | Switch language (`tr_tr` / `en_en`) |
| `/nightmarket admin anim <preview\|stats\|quality\|patterns\|shapes\|reload>` | Animation tools |
| `/nightmarket info` · `/nightmarket help` | Version info · help |

## Configuration

- `config.yml` — economy, language, market, schedule, refresh, session,
  protection, floating, animation, purchase feedback, cinematics, performance,
  storage.
- `rarities.yml` — rarity weights, colors, heads and effects.
- `offers.yml` — offers, pricing, discounts, stock, limits.
- `commands.yml` — command names, Turkish + English aliases, permissions.
- `lang/tr_tr.yml`, `lang/en_en.yml` — every message, MiniMessage + `%placeholders%`.

## Placeholders (`%ensnightmarket_<key>%`)

Market: `refresh`, `refresh_seconds`, `refresh_hours`, `offers`, `revealed`,
`unrevealed`, `remaining`, `purchased`, `purchases`, `economy`, `balance`.
Schedule: `schedule_open`, `schedule_enabled`. Session: `session_active`,
`session_remaining`, `session_remaining_seconds`. Refresh: `refresh_price`,
`refresh_enabled`.

## Custom Heads

Per rarity in `rarities.yml`:

```yaml
head:
  type: TEXTURE_VALUE
  value: "eyJ0ZXh0dXJlcyI6..." # minecraft-heads.com "Value" field
```

Types: `TEXTURE_VALUE` / `BASE64` (full Base64), `RAW` / `VALUE` /
`TEXTURE_ID` (bare id or URL, wrapped for you), `URL` / `TEXTURE_URL` (full
`textures.minecraft.net` URL), `HDB` (HeadDatabase id, needs HeadDatabase).

Blank or invalid values fall back to a plain player head. Values are cached —
after editing run `/nightmarket admin reload` and re-showcase. Once revealed,
the floating display swaps the rarity chest head for the actual reward item.

## Requirements

Paper 1.20+ (Paper or Spigot; Folia is not supported), Java 17, one economy (Vault + economy plugin,
or PlayerPoints). Optional: PlaceholderAPI, WorldGuard, Oraxen, ItemsAdder,
Nexo, MMOItems, HeadDatabase.

Build: `mvn clean package` → `target/EnsNightMarket-2.1.0.jar`.
Details in [Building](docs/building.md).

## License

Proprietary — see [LICENSE](LICENSE). © 2026 EnsisDev. All Rights Reserved.
