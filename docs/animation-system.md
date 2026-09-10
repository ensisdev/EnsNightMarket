# Animation System

EnsNightMarket's animation stack turns a simple head shop into a small
cinematic: idle motion, reveal ceremonies, purchase feedback and close sweeps -
all tunable from `config.yml` and throttled by the performance system.

## Layer 1 - Idle Motion Patterns

While floating, each head runs an idle motion chosen per rarity:

```yaml
animation:
  motion-pattern:
    common: BOB
    uncommon: WAVE
    rare: ORBIT
    legendary: VORTEX
    mysterious: FIGURE_EIGHT
```

Available patterns include `BOB`, `WAVE`, `ORBIT`, `VORTEX`, `FIGURE_EIGHT`
(see `/nightmarket admin anim patterns` for the live list). Base settings:

```yaml
floating:
  bob-amplitude: 0.12
  bob-speed: 0.07
  rotation-speed: 1.2
```

## Layer 2 - Reveal Ceremony

Left-clicking an unrevealed head runs:

1. **Particle shape** - per-rarity shape from `animation.reveal-shape`:
   `SPHERE`, `RING`, `SPIRAL`, `SHOCKWAVE`, `BURST`, `HELIX` (list via
   `/nightmarket admin anim shapes`).
2. **Sound ladder** - `SoundBank.note()` plays ascending note-block tones
   (`cinematics.reveal-sound-ladder`).
3. **Flash** - FLASH particle burst (`cinematics.reveal-flash`).
4. **Eased item transform** - the head scales/swaps into the real reward item
   with the configured easing curve (`animation.reveal-easing`, e.g.
   `EASE_OUT_ELASTIC`); `animation.enable-advanced-reveal: false` = instant swap.
5. **Rarity bonuses** - hardcoded extras for specific rarity ids:
   - `legendary`: FLAME ring + TOTEM spiral.
   - `mysterious`: SOUL_FIRE_FLAME helix + ender dragon growl.

Reveal length comes from `animation.reveal-duration-ticks` (default 24).

## Layer 3 - Purchase Feedback

```yaml
purchase-feedback:
  title-enabled: true
  title-max-weight: 15.0   # titles only for rarities this rare
  sound-enabled: true
```

Plus purchase confetti (`cinematics.purchase-confetti`).

## Layer 4 - Spawn Ceremony and Close

- `cinematics.spawn-ceremony` + `spawn-stagger-ticks: 4` - heads spawn one by
  one (0-20 ticks apart) with sounds.
- `cinematics.close-animation` - closing sweep when the showcase ends.
- `cinematics.idle-aura` / `idle-twinkle` / `ambient-sound` - living-market feel.

## Debug Tools

```
/nightmarket admin anim preview <rarity>   # play a rarity's reveal here
/nightmarket admin anim stats              # timing + particle counters
/nightmarket admin anim quality HIGH       # live quality override
/nightmarket admin anim patterns           # list motion patterns
/nightmarket admin anim shapes             # list reveal shapes
/nightmarket admin anim reload             # clear all displays
```

## How It Stays Fast

Every animation passes through the performance governor:

- Quality preset (`performance.animation-quality`) scales particle counts.
- LOD (level of detail) reduces particles for distant viewers.
- `max-particles-per-player` is a hard budget.
- `animation-distance` culls effects beyond range.

See [Performance](performance.md) for tuning details.
