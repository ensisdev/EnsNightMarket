# Architecture

A map of the source tree for developers (`src/main/kotlin/dev/ensisdev/ensnightmarket/`).

## High-Level Flow

```
/nightmarket
   |
NightMarketCommand (registry-driven from commands.yml)
   |
MarketManager.getOrCreate(uuid)  --> roll offers (weighted rarities, discounts, stock)
   |
FloatingDisplayManager.show(player)  --> per-player ItemDisplay + TextDisplay + Interaction
   |                                      entities (owner-only viewers), arc layout
PlayerInteractListener
   |-- left click --> reveal ceremony (RevealAnimator, ParticleShapes, SoundBank)
   |-- right click -> purchase pipeline (stock/permission/limit/economy) in per-player lock
   |
Storage (SQLite/MySQL, async) persists the market
```

## Packages

| Package | Role |
| --- | --- |
| `market` | `MarketManager`, `MarketOffer`, rarity-weighted rolling, purchase pipeline |
| `display` | `FloatingDisplayManager` (spawn, arc layout, follow tick), `RevealAnimator` |
| `animation` | `MotionPatterns`, `Easing` / `EasingType`, `ParticleShapes`, `SoundBank`, LOD governor |
| `heads` | `HeadFactory` - texture type resolution (Base64/URL/HDB), head caching |
| `storage` | SQLite/MySQL backends, async writes |
| `economy` | `EconomyManager` - Vault / PlayerPoints abstraction |
| `schedule` | `ScheduleManager` - Minecraft-time night window + broadcasts |
| `protection` | WorldGuard hook: region black/whitelist + `ensnightmarket` flag |
| `placeholder` | `PlaceholderHook` - reflection-based PlaceholderAPI expansion |
| `config` | Config loading with auto-merge of missing keys |
| `commands` | Command registry reading `commands.yml` into actions |
| `gui` | Admin GUI pages (offers, rarities, settings, player market) |
| `lang` | `Lang` - language file access with MiniMessage |
| `util` | `Msgs`, misc helpers |

## Notable Design Choices

- **Per-player, owner-only displays:** each head is an `ItemDisplay` entity with
  its `TextDisplay` hologram and invisible `Interaction` hitbox; viewers are
  restricted to the owner, so markets are private.
- **Persistence-first:** refresh timers, discounts, stock and purchase counts
  live in storage, not memory - restart-safe by design.
- **Reflection integrations:** PlaceholderAPI, WorldGuard, Oraxen/IA/Nexo/
  MMOItems/HeadDatabase are all optional; missing plugins degrade gracefully.
- **Paper + Spigot + Folia:** Adventure Components on Paper with legacy string
  fallbacks; schedulers are wrapped for Folia compatibility.
- **Registry-driven commands:** `commands.yml` maps names to actions, so
  servers customize commands without code changes.
