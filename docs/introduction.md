# Introduction

EnsNightMarket is a **per-player floating Night Market** plugin for Paper servers,
written in Kotlin. Type `/nightmarket` and six floating custom heads appear in an
arc in front of you. **Left-click** a head to reveal its reward, **right-click** to
buy it. Every player gets their own private market with their own prices, discounts
and stock — nobody else can see your market.

## Highlights

- **Per-player markets** - your deals are yours alone, with persistent refresh timers that survive restarts.
- **Five default rarities** (Common, Uncommon, Rare, Legendary, Mysterious) plus unlimited custom rarities.
- **Weighted rarity rolls** - control the odds per rarity with weights.
- **Custom heads** - Base64 textures, texture URLs or HeadDatabase IDs.
- **Reveal animation** - particle shapes, sound ladders, easing curves and a title screen for rare drops.
- **Motion patterns** - each rarity floats with its own idle motion (BOB, WAVE, ORBIT, VORTEX, FIGURE_EIGHT...).
- **Night schedule** - open the market only between two Minecraft clock times, with automatic broadcasts.
- **Paid reroll** - players pay to refresh their market, with price and cooldown.
- **Sessions** - heads follow you around, expire after a configurable time, close on teleport, world change or death.
- **WorldGuard integration** - blacklist/whitelist regions plus an `ensnightmarket` region flag.
- **Custom item rewards** - Oraxen, ItemsAdder, Nexo, MMOItems and HeadDatabase.
- **Vault or PlayerPoints** economy, SQLite or MySQL/MariaDB storage with async writes.
- **PlaceholderAPI expansion** with 17 placeholders.
- **Full admin GUI** - manage offers, rarities, settings and player markets in-game.
- **Paper-first, Spigot-friendly** - Adventure Components on Paper, legacy fallback on Spigot, no crashes.

## How It Works

1. A player runs `/nightmarket`.
2. The plugin checks the night schedule and WorldGuard region rules.
3. Six offers are rolled from the weighted rarity pool (or loaded from storage if the player still has a live market).
4. `ItemDisplay` heads spawn in an arc in front of the player, each with a `TextDisplay` hologram and an invisible `Interaction` hitbox - all visible **only to the owner**.
5. Left-clicking an unrevealed head plays the reveal animation and shows the actual reward item.
6. Right-clicking a revealed head runs the purchase pipeline: stock check, permission check, purchase limit, inventory-fit check, economy charge, reward handout - all inside a per-player transaction lock.

## Requirements

| Requirement | Version |
| --- | --- |
| Server | Paper (or Spigot-compatible) **1.20+**, Folia supported |
| Java | 17+ (bundled with modern server builds) |
| Economy | Vault + an economy plugin, **or** PlayerPoints |
| Optional | PlaceholderAPI, WorldGuard, Oraxen, ItemsAdder, Nexo, MMOItems, HeadDatabase |

## Next Steps

- [Installation](installation.md) - drop the jar in, install dependencies.
- [Quick Start](quick-start.md) - your first market in 5 minutes.
- [Commands](commands.md) and [Permissions](permissions.md) reference.

## Links

- **Version:** 2.1.0
- **Author:** EnsisDev
- **License:** MIT
