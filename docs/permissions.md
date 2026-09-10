# Permissions

All permission nodes from `plugin.yml`. Defaults are applied when no permission
plugin overrides them.

| Node | Default | Description |
| --- | --- | --- |
| `ensnightmarket.use` | `true` (everyone) | Use the player-facing commands (`showcase`, `close`, `refresh-paid`, `info`, `help`) |
| `ensnightmarket.showcase` | `true` (everyone) | Open the floating showcase |
| `ensnightmarket.admin` | OP | Admin GUI, player market inspection, force refresh/reset, animation tools |
| `ensnightmarket.reload` | OP | `/nightmarket reload` |
| `ensnightmarket.animation` | OP | Animation debug commands (`/nightmarket admin anim ...`) |

## Notes

- Commands carry their **own** permission from `commands.yml`. The stock file
  gives `admin` commands the `ensnightmarket.admin` permission, but you can
  grant, say, `/nightmarket admin refresh` to moderators only by editing that
  node's permission.
- Revealing another player's heads (admin click) requires `ensnightmarket.admin`.
- Offers can have a per-offer `permission` in `offers.yml` - purchases fail with
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
