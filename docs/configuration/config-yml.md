# config.yml Reference

Every key in `plugins/EnsNightMarket/config.yml` with its default and meaning.
Missing keys are auto-merged from defaults when the plugin starts.

## economy

```yaml
economy:
  provider: VAULT
```

| Key | Values | Description |
| --- | --- | --- |
| `provider` | `VAULT`, `PLAYER_POINTS` | Economy backend. Vault requires Vault + an economy plugin; `PLAYER_POINTS` uses PlayerPoints points. |

## language

```yaml
language: en_en
```

Active language file in `plugins/EnsNightMarket/lang/`. Supported out of the box:
`en_en`, `tr_tr`. Admins can also change this at runtime with
`/nightmarket admin lang <code>`.

## market

```yaml
market:
  slots: 6
  refresh-hours: 72
  unique-offers: true
```

| Key | Default | Description |
| --- | --- | --- |
| `slots` | 6 | Offers generated per roll (head count in the arc) |
| `refresh-hours` | 72 | Hours until a player's market auto-rerolls. The timer is persisted, so it survives restarts |
| `unique-offers` | true | Prevent the same offer appearing twice in one market |

## schedule

```yaml
schedule:
  enabled: false
  open-tick: 13000
  close-tick: 23000
```

Opens the market only during a Minecraft daytime window (0-23999 ticks).
When the window opens everyone gets a broadcast; when it closes, any open
showcases close automatically. `enabled: false` = always open.

## refresh (paid reroll)

```yaml
refresh:
  enabled: true
  price: 500.0
  cooldown-minutes: 30
```

`/nightmarket refresh` (alias `yenile`) charges `price` and rerolls the player's market; the player
must wait `cooldown-minutes` between paid rerolls. Cooldowns persist in
`cooldowns.yml`, so restarts do not reset them.

## interaction

```yaml
interaction:
  raycast-range: 6.5
  hit-radius: 1.5
```

| Key | Default | Description |
| --- | --- | --- |
| `raycast-range` | 6.5 | How far ahead a swing (left-click without hitting) can reveal a head |
| `hit-radius` | 1.5 | How close to the aim ray a head must be to count as targeted |

## updates

```yaml
updates:
  check: true
```

When enabled, the plugin checks GitHub releases once after startup and tells
admins on join when a newer version exists.

## session

```yaml
session:
  duration-minutes: 10
  follow-enabled: true
  follow-speed: 0.45
  follow-snap-distance: 12.0
  follow-move-threshold: 0.2
```

| Key | Default | Description |
| --- | --- | --- |
| `duration-minutes` | 10 | Showcase lifetime after opening; `0` = unlimited. Head entities are removed when it expires |
| `follow-enabled` | true | Heads follow the owner as they walk |
| `follow-speed` | 0.45 | Lerp factor; `0.1` = loose drift, `1.0` = instant |
| `follow-snap-distance` | 12.0 | If the target is farther than this (ender pearl, fall), heads teleport instead of sliding |
| `follow-move-threshold` | 0.2 | Ignore movement below this distance (prevents rotation-only jitter) |

## protection

```yaml
protection:
  enabled: false
  mode: BLACKLIST
  regions:
    - "spawn"
```

WorldGuard region control (ignored when WorldGuard is absent).

- `BLACKLIST`: market cannot open in the listed regions.
- `WHITELIST`: market can open only in the listed regions.

Additionally the custom state flag `ensnightmarket` can be set per-region
(`allow` / `deny` / `undefined`).


## floating

```yaml
floating:
  enabled: true
  ambient-particles: true
  scale: 1.55
  bob-amplitude: 0.12
  bob-speed: 0.07
  rotation-speed: 1.2
  view-range: 32.0
  interpolation-ticks: 3
  text-height: 0.85
  forward-distance: 4.5
  eye-height: 1.45
  spacing: 2.2
  layout-formation: ARC
  layout-arc-depth: 1.6
  layout-height-wave: 0.0
```

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | true | Master switch for the floating head system |
| `ambient-particles` | true | Passive particle aura around heads |
| `scale` | 1.55 | Head size (ItemDisplay transform scale) |
| `bob-amplitude` / `bob-speed` | 0.12 / 0.07 | Idle bobbing |
| `rotation-speed` | 1.2 | Idle spin speed |
| `view-range` | 32.0 | Display entity view distance |
| `interpolation-ticks` | 3 | Teleport interpolation smoothing |
| `text-height` | 0.85 | Hologram height above the head |
| `forward-distance` | 4.5 | How far in front of the player the arc spawns |
| `eye-height` | 1.45 | Vertical offset from feet |
| `spacing` | 2.2 | Horizontal distance between heads |
| `layout-formation` | ARC | Head arrangement in front of the player (`ARC` or `LINE`) |
| `layout-arc-depth` | 1.6 | Arc curvature depth |
| `layout-height-wave` | 0.0 | Vertical wave across the arc |

## animation

```yaml
animation:
  reveal-duration-ticks: 24
  enable-advanced-reveal: true
  reveal-easing: EASE_OUT_ELASTIC
  motion-pattern:
    common: BOB
    uncommon: WAVE
    rare: ORBIT
    legendary: VORTEX
    mysterious: FIGURE_EIGHT
  reveal-shape:
    common: SPHERE
    uncommon: RING
    rare: SPIRAL
    legendary: SHOCKWAVE
    mysterious: BURST
```

| Key | Description |
| --- | --- |
| `reveal-duration-ticks` | Reveal animation length |
| `enable-advanced-reveal` | false = instant item swap on reveal |
| `reveal-easing` | Any `EasingType`: `LINEAR`, `EASE_IN_*`, `EASE_OUT_*` (incl. `EASE_OUT_ELASTIC`, `EASE_OUT_BOUNCE`), `EASE_IN_OUT_*` |
| `motion-pattern.<rarity>` | Idle motion: `BOB`, `WAVE`, `ORBIT`, `VORTEX`, `FIGURE_EIGHT` and more (see [Animation System](../animation-system.md)) |
| `reveal-shape.<rarity>` | Reveal particle shape: `SPHERE`, `RING`, `SPIRAL`, `SHOCKWAVE`, `BURST`, `HELIX`, ... |

## purchase-feedback

```yaml
purchase-feedback:
  title-enabled: true
  title-max-weight: 15.0
  sound-enabled: true
```

Title + sound on successful purchase. The title only shows for rarities with
weight <= `title-max-weight` (keeps title noise low for common items).

## cinematics

```yaml
cinematics:
  enabled: true
  spawn-ceremony: true      # staggered, dramatic head spawn
  spawn-stagger-ticks: 4    # delay between heads (0-20)
  spawn-sound: true
  reveal-flash: true        # FLASH particle on reveal
  reveal-sound-ladder: true # ascending note ladder on reveal
  idle-aura: true           # colored dust aura around heads
  aura-max-weight: 4.0      # only rarities with weight <= this get the aura
  idle-twinkle: true
  close-animation: true     # closing sweep when the market closes
  purchase-confetti: true
  ambient-sound: false      # looping ambience while the market is open
```

## performance

```yaml
performance:
  animation-quality: HIGH
  enable-lod: true
  max-particles-per-player: 200
  animation-distance: 32.0
  world-blacklist:
    - "world_nether"
    - "world_the_end"
  async-storage: true
```

| Key | Default | Description |
| --- | --- | --- |
| `animation-quality` | HIGH | `LOW` / `MEDIUM` / `HIGH` / `ULTRA`; scales particle counts and tick rates |
| `enable-lod` | true | Level-of-detail: fewer particles for distant players |
| `max-particles-per-player` | 200 | Hard particle budget per player |
| `animation-distance` | 32.0 | Effects are skipped beyond this distance |
| `world-blacklist` | nether/end | Showcases cannot open in these worlds |
| `async-storage` | true | Database writes run on async threads |

## storage

```yaml
storage:
  type: SQLITE
  mysql:
    host: localhost
    port: 3306
    database: ensnightmarket
    username: root
    password: change-me
```

`SQLITE` (single-file `markets.db`) or `MYSQL` (also works with MariaDB).
See [Storage](../storage.md) for details.

