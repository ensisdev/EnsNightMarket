# WorldGuard Protection

EnsNightMarket integrates with WorldGuard on two levels. Both are optional -
without WorldGuard the checks pass silently.

## 1. Config Blacklist / Whitelist

```yaml
protection:
  enabled: false
  mode: BLACKLIST
  regions:
    - "spawn"
```

- `enabled: true` activates region checks (ignored if WorldGuard is absent).
- `BLACKLIST`: market cannot open inside listed regions.
- `WHITELIST`: market can open only inside listed regions.

When blocked, the player gets the `protection-denied` message and no heads
spawn. The check also runs on the follow tick, so heads will not drift into a
forbidden region.

## 2. Custom Region Flag: `ensnightmarket`

A WorldGuard state flag (`allow` / `deny` / `undefined`) registered as
`ensnightmarket`:

```
/rg flag market_area ensnightmarket deny
```

`deny` blocks opening the market in that region regardless of the config lists;
`undefined` falls back to the config rules above.

## Runtime Notes

- Region membership is checked at `/nightmarket` (open), during the follow tick
  (probe at the arc center), and follows the region at the player's location.
- Combining both layers: flag `deny` wins; otherwise the config mode decides.
- Fail-closed: if WorldGuard is present and protection is enabled but the
  region query itself errors, the market is denied (with a console warning)
  rather than silently allowed.

## Typical Setups

| Goal | Config |
| --- | --- |
| No market at spawn | `mode: BLACKLIST`, `regions: [spawn]` |
| Market only in the bazaar | `mode: WHITELIST`, `regions: [bazaar]` |
| Per-region control in-game | Use the `ensnightmarket` flag, leave config `enabled: false` |
