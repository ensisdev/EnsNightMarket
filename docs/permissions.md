# Permissions

All permission nodes from `plugin.yml`. Defaults are applied when no permission
plugin overrides them.

| Node | Default | Description |
| --- | --- | --- |
| `ensnightmarket.use` | `true` (everyone) | Player commands: open, close, paid refresh, info, help |
| `ensnightmarket.admin` | OP | Admin GUI, player market inspection, force refresh/reset, animation tools |
| `ensnightmarket.reload` | OP | `/nightmarket admin reload` |
| `ensnightmarket.animation` | OP | Animation debug commands (`/nightmarket admin anim ...`) |

## Notes

- Player commands carry `ensnightmarket.use` from `commands.yml` (revoke it to
  block the market for a group without touching anything else). Admin commands
  carry `ensnightmarket.admin`, reload carries `ensnightmarket.reload` — all
  editable per node in `commands.yml`.
- Revealing another player's heads (admin click) requires `ensnightmarket.admin`.
  Admins can reveal but never purchase on someone else's behalf.
- Offers can have a per-offer `permission` in `offers.yml` — purchases fail with
  a "no permission" message when the buyer lacks it.
- WorldGuard's `ensnightmarket` state flag can deny opening the market in
  specific regions regardless of permission nodes.

## Typical LuckPerms Setup

```
# Everyone can use the market (already default)
 /lp group default permission set ensnightmarket.use true

# Moderators can inspect and reset player markets
/lp group moderator permission set ensnightmarket.admin true

# Only admins reload configs
/lp group admin permission set ensnightmarket.reload true
```
