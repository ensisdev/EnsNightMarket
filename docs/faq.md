# FAQ

## General

**Q: Can two players see each other's markets?**
No. Each player's heads are display entities visible only to their owner.

**Q: What happens to my market when the server restarts?**
Everything is persisted in storage (offers, prices, stock, purchase counts,
refresh timer). After restart the player continues where they left off.

**Q: Does the market work on Spigot?**
Yes - Paper features (Adventure Components, display entities) are used when
available with Spigot fallbacks. Folia schedulers are also supported.

**Q: Which Minecraft versions are supported?**
1.20+ (display entities are required). Compiled for modern APIs.

## Buying

**Q: Why can't I buy an offer?**
The pipeline checks, in order: stock remaining, per-offer permission,
`max-purchases` limit, free inventory space, and your balance. The
corresponding message tells you which check failed.

**Q: How do prices get so cheap sometimes?**
Each offer rolls a discount in its `discount-min`..`discount-max` range per
player per refresh. Wait for the next refresh or reroll.

**Q: How do I reroll?**
`/nightmarket refresh` (or `/nightmarket yenile`) — costs `refresh.price`
with a `refresh.cooldown-minutes` cooldown. Admins can force-reroll anyone for
free with `/nightmarket admin refresh <player>`.

## Setup

**Q: Heads appear but look like plain Steve heads.**
The texture value in `rarities.yml` is blank/invalid. Use the full Base64
"Value" from minecraft-heads.com with `type: TEXTURE_VALUE`. After fixing,
`/nightmarket reload` and re-showcase (values are cached).

**Q: An offer never appears.**
If it uses a custom item (`oraxen:`, `itemsadder:`, ...), the provider plugin
is missing or the id is wrong - the offer is skipped with a console warning.

**Q: Purchases always fail / "no economy".**
Install Vault + an economy plugin, or PlayerPoints, and set
`economy.provider` accordingly.

**Q: The market won't open at spawn.**
WorldGuard protection: you are in a blacklisted region (or a region with
`ensnightmarket deny`).

**Q: `/nightmarket refresh` says I must wait.**
That is the paid-reroll cooldown (`refresh.cooldown-minutes`, default 30 min).
It resets on server restart; admins bypass it with
`/nightmarket admin refresh <player>`.

## Performance

**Q: Will this lag my server?**
The animation governor (quality presets, LOD, particle budget, distance
culling) keeps effects cheap. On huge servers drop to `MEDIUM` and keep
`enable-lod: true`.

**Q: How do I check effect cost?**
`/nightmarket admin anim stats` shows timing and particle counters.

## Customization

**Q: Can I add my own rarities?**
Yes - add a block in `rarities.yml`, optionally add its motion pattern /
reveal shape entries in `config.yml`, and reference the id in `offers.yml`.

**Q: Can I rename commands?**
Yes - everything is driven by `commands.yml` (names, aliases, permissions,
actions).
