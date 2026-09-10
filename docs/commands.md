# Commands

The main command is `/nightmarket` with aliases:
`/ensnightmarket`, `/enm`, `/gecepazari`, `/gece-pazari`, `/night-market`.

The command tree is defined in `commands.yml`, so servers can rename commands,
add aliases, change permissions, or add new commands bound to existing actions —
without recompiling. See [commands.yml](configuration/commands-yml.md).

## Player Commands

| Command | Alias (TR) | Description |
| --- | --- | --- |
| `/nightmarket` | `/nightmarket ac` | Opens your floating market showcase |
| `/nightmarket close` | `/nightmarket kapat` | Closes your market with a closing animation |
| `/nightmarket refresh` | `/nightmarket yenile` | Pays to reroll your offers (price + cooldown from config) |
| `/nightmarket info` | `/nightmarket bilgi` | Plugin version, economy provider, offer count |
| `/nightmarket help` | `/nightmarket yardim` | Command list filtered by your permissions |

With no arguments (`/nightmarket`) the `default-action: showcase` runs, so it
behaves exactly like `/nightmarket open`.

Player commands require `ensnightmarket.use` (default: everyone); admin
commands require `ensnightmarket.admin` (reload: `ensnightmarket.reload`).
See [Permissions](permissions.md).

## Admin Commands

Permission node shown in parentheses.

| Command | Permission | Description |
| --- | --- | --- |
| `/nightmarket admin [player]` | `ensnightmarket.admin` | Opens the admin GUI; with a player name opens that player's market |
| `/nightmarket admin refresh <player>` | `ensnightmarket.admin` | Force-rerolls a player's market (free, no cooldown) |
| `/nightmarket admin reset <player>` | `ensnightmarket.admin` | Deletes a player's market data completely |
| `/nightmarket admin reload` | `ensnightmarket.reload` | Reloads configs, languages, commands, economy, schedule and displays (closes open showcases so nobody keeps stale prices) |
| `/nightmarket admin lang [code]` | `ensnightmarket.admin` | Shows the current language, or switches it (`tr_tr` / `en_en`) |
| `/nightmarket admin anim <sub>` | `ensnightmarket.admin` | Animation debug tools, see below |

{% hint style="info" %}
`refresh` and `reset` accept offline players too. `reset` wipes everything
(price rolls, stock, purchase counts, timer); `refresh` only rerolls.
{% endhint %}

## Animation Debug (`/nightmarket admin anim ...`)

Player-only. Requires `ensnightmarket.admin` (or `ensnightmarket.animation`).

| Subcommand | Description |
| --- | --- |
| `anim preview [rarity]` | Plays the reveal animation of a rarity at your location |
| `anim stats` | Average animation/follow time, particle totals |
| `anim quality <LOW\|MEDIUM\|HIGH\|ULTRA>` | Overrides `performance.animation-quality` live (saved to config) |
| `anim patterns` | Lists all motion patterns |
| `anim shapes` | Lists all reveal particle shapes |
| `anim reload` | Removes all displays and resets performance counters |
| `anim help` | Animation help |

## How Command Resolution Works

`commands.yml` maps names to **actions**. The registry:

1. Reads `commands.yml` (auto-created if missing).
2. Resolves the typed token against each node's `name` + `aliases`.
3. Falls back to `default-action: showcase` when no arguments are given.
4. Checks the node's permission, then dispatches the action.

Available actions: `showcase`, `close`, `refresh-paid`, `info`, `help`,
`admin-menu`, `admin-refresh`, `admin-reset`, `admin-reload`, `admin-anim`,
`admin-lang`.

That means you can, for example, create a `/gece` command with action
`showcase` and permission `""` without touching code.

## Tab Completion

Tab completion is permission-aware:

- Argument 1: command names + aliases you may use.
- Argument 2: subcommands of that command, or online player names for
  `admin` / `refresh` / `reset`.
- Argument 3–4: language codes for `lang`, sub-actions for `anim`.
