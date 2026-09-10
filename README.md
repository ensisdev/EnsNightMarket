# EnsNightMarket 2.1.0

Per-player floating Night Market for Paper, written in Kotlin and built with Maven.
Type `/nightmarket` and six floating heads appear in front of you. Left-click
reveals, right-click buys. Nobody else can see your market.

## Features

- Paper 1.21+ build target, Java 17 bytecode.
- Per-player offers with persistent refresh timers.
- Five default rarities: Yaygın, Sıradışı, Nadir, Efsanevi, Gizemli.
- Unlimited custom rarities via `rarities.yml`.
- Weighted rarity generation.
- Base64, texture URL and HeadsDatabase (`HDB`) custom heads.
- ItemDisplay floating heads + TextDisplay holograms, visible only to the owner.
- Invisible Interaction hitboxes, so left and right clicks always register.
- Motion patterns per rarity (BOB, WAVE, ORBIT, FIGURE_EIGHT, WOBBLE, PULSE, SWAY, DRIFT, VORTEX).
- Reveal animation with particle shapes and sounds.
- Purchase feedback: sound for every buy, title screen for rare drops.
- Night schedule: market opens only between two Minecraft clock times, with broadcasts.
- Paid player refresh (`/nightmarket yenile`) with price and cooldown.
- Sessions: heads follow you, expire after a configurable time, close on teleport, world change or death.
- WorldGuard region protection (blacklist or whitelist mode, plus an `ensnightmarket` flag).
- Custom item rewards: Oraxen, ItemsAdder, Nexo, MMOItems (`oraxen:ID`, `itemsadder:ID`, `nexo:ID`, `mmoitems:TIP:ID`).
- Vault economy or PlayerPoints.
- SQLite or MySQL/MariaDB persistence with async writes.
- Stock, discounts, purchase limits and permissions.
- Transaction locking and inventory-fit checks to reduce dupe/race issues.
- PlaceholderAPI expansion.
- Admin GUI and admin commands.

## Commands

`/nightmarket` (aliases: `enm`, `gecepazari`, `gece-pazari`, `night-market`)
`/nightmarket kapat`
`/nightmarket yenile`
`/nightmarket admin <player>`
`/nightmarket refresh <player>`
`/nightmarket reroll <player>`
`/nightmarket reset <player>`
`/nightmarket reload`
`/nightmarket info`
`/nightmarket anim <preview|stats|quality|patterns|shapes|reload>`

## Configuration

- `config.yml` — economy, market, schedule, refresh, session, protection, floating, animation, performance, storage, purchase feedback and messages.
- `rarities.yml` — rarity weights, heads and effects.
- `offers.yml` — offers, pricing and stock.

## Placeholders (`%ensnightmarket_<key>%`)

Market: `refresh`, `refresh_seconds`, `refresh_hours`, `offers`, `revealed`,
`unrevealed`, `remaining`, `purchased`, `purchases`, `economy`, `balance`.
Schedule: `schedule_open`, `schedule_enabled`.
Session: `session_active`, `session_remaining`, `session_remaining_seconds`.
Refresh: `refresh_price`, `refresh_enabled`.

## Texture input

For a custom head, set in `rarities.yml` under each rarity:

```yaml
head:
  type: TEXTURE_VALUE
  value: "eyJ0ZXh0dXJlcyI6..." # minecraft-heads.com "Value" field
```

Supported `type` values:
- `TEXTURE_VALUE` / `BASE64` — the server-issued Base64 texture value (starts with `eyJ0ZXh0dXJlcyI6`), copied straight from minecraft-heads.com "Value".
- `RAW` / `VALUE` / `TEXTURE_ID` — just the texture id (`f18612fd...`) or the full URL; the plugin wraps it for you.
- `URL` / `TEXTURE_URL` — full `http(s)://textures.minecraft.net/texture/...` URL.
- `HDB` — HeadsDatabase head id (HeadDatabase plugin required).

Blank or invalid values fall back to a plain player head. Values are cached, so
changing them in `rarities.yml` requires `/nightmarket reload` plus re-showcasing
(`/nightmarket` or relog). Once an offer is revealed, the floating display
swaps the rarity chest head for the actual reward item.
