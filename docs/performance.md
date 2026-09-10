# Performance

EnsNightMarket ships a built-in performance governor for its particle-heavy
animation system.

## Quality Presets

```yaml
performance:
  animation-quality: HIGH
```

| Preset | Use |
| --- | --- |
| `LOW` | Weak hardware, huge player counts - minimal particles |
| `MEDIUM` | Balanced |
| `HIGH` | Default - full visuals |
| `ULTRA` | Showcase servers - maximum particle counts |

Change live with `/nightmarket admin anim quality <preset>`.

## Level of Detail (LOD)

`enable-lod: true` reduces particle counts and tick rates for viewers far from
the effects. Together with `animation-distance: 32.0`, distant players trigger
no particle work at all.

## Particle Budget

`max-particles-per-player: 200` caps how many particles are emitted per player
per frame budget, preventing rare-rarity reveals from spiking MSPT.

## Other Levers

- `floating.interpolation-ticks: 3` - lower = snappier head movement, higher
  teleport cost.
- `floating.view-range: 32.0` - display entity render distance.
- `performance.world-blacklist` - disable the plugin entirely in noisy worlds.
- `performance.async-storage: true` - keep database writes off the main thread.
- `cinematics.*` - disable individual ceremony effects that you do not need.

## Measuring

```
/nightmarket admin anim stats
```

prints average animation time, follow-tick time and particle totals - compare
before/after when tuning.

## Recommended Profiles

| Server | Settings |
| --- | --- |
| 100+ players, survival | `MEDIUM`, LOD on, `max-particles-per-player: 120` |
| 20-50 players | defaults (`HIGH`, LOD on) |
| Event/showcase server | `ULTRA`, LOD off, `animation-distance: 48` |
