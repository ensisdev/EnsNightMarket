# Quick Start

Your first market in five minutes.

## 1. Open the Market

Join your server and type:

```
/nightmarket
```

Six floating heads appear in an arc in front of you. Each head shows a mystery
label like `??? - Legendary - Left-click to reveal`.

## 2. Interact

| Action | Result |
| --- | --- |
| **Left-click** a head | Reveals the reward with a particle + sound animation |
| **Right-click** a revealed head | Buys it (checks stock, permission, limit, inventory space and balance) |

While the market is open the heads **follow you** as you walk. They close on
teleport, world change, death, or when the session timer expires.

## 3. Reroll Your Deals

Do not like your offers? Pay to reroll:

```
/nightmarket refresh
```

(`yenile` works too — every command ships with a Turkish alias. See
[Commands](commands.md).)

Price and cooldown come from `refresh.price` and `refresh.cooldown-minutes`
in `config.yml` (defaults: 500, 30 min).

## 4. Open the Admin GUI

As an operator:

```
/nightmarket admin
```

From here you can:

- **Offers** - list, add from hand, edit price/stock/lore, enable/disable, delete.
- **Rarities** - create, edit weights, colors, particles, sounds, head textures.
- **Settings** - edit the main numbers from `config.yml` in-game.
- **Player Market** - inspect any player's market, reroll / reveal all / reset it.

## 5. Adjust the Look

Open `plugins/EnsNightMarket/config.yml`:

```yaml
floating:
  scale: 1.55            # head size
  spacing: 2.2           # distance between heads
  layout-formation: ARC  # ARC or LINE
animation:
  reveal-easing: EASE_OUT_ELASTIC
  motion-pattern:
    legendary: VORTEX    # how legendary heads float
cinematics:
  spawn-ceremony: true   # staggered spawn with sounds
```

Then `/nightmarket reload` and run `/nightmarket` again.

## 6. Night Schedule (optional)

Want the market to only exist at night in Minecraft time?

```yaml
schedule:
  enabled: true
  open-tick: 13000   # ~night
  close-tick: 23000
```

When the window opens/closes everyone gets a broadcast, and open markets close
automatically.

## Typical Server Setup

- **Survival / SMP:** defaults are fine. Enable the schedule for atmosphere.
- **Skyblock:** raise `refresh.price` and lower stock ranges for scarcity.
- **Ranks / crates server:** give rare rarities `weight` close to 0 and use custom
  Oraxen/ItemsAdder rewards as jackpot offers.

Next: read [Commands](commands.md) and [Permissions](permissions.md), then dive
into [config.yml](configuration/config-yml.md) for every option.
