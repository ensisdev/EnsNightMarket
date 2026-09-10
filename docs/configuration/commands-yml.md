# commands.yml

`commands.yml` is the runtime command registry. It lets you rename commands,
add aliases, change required permissions and bind commands to existing actions —
without recompiling.

```yaml
default-action: showcase
commands:
  open:
    aliases: [ac]
    permission: ""
    description: "Open the market"
    action: showcase
  close:
    aliases: [hide, kapat]
    permission: ""
    description: "Close the market"
    action: close
  refresh:
    aliases: [yenile]
    permission: ""
    description: "Pay to refresh the market"
    action: refresh-paid
  admin:
    aliases: []
    permission: ensnightmarket.admin
    description: "Admin menu"
    action: admin-menu
    subcommands:
      reload:
        aliases: []
        permission: ensnightmarket.reload
        description: "Refresh the configuration"
        action: admin-reload
```

## Fields

| Field | Description |
| --- | --- |
| `action` | Which handler runs. See list below |
| `aliases` | Alternative names players can type |
| `permission` | Required permission node (`ensnightmarket.use` for player commands, `""` = everyone) |
| `description` | Shown in `/nightmarket help` |
| `subcommands` | Nested nodes for `admin`-style commands |
| `default-action` | Action used for `/nightmarket` with no arguments |

## Available Actions

`showcase`, `close`, `refresh-paid`, `info`, `help`,
`admin-menu`, `admin-refresh`, `admin-reset`, `admin-reload`, `admin-anim`,
`admin-lang`.

## Stock Aliases

The stock file ships with both English and Turkish aliases out of the box:

| Key | Aliases |
| --- | --- |
| `open` | `ac` |
| `close` | `hide`, `kapat` |
| `refresh` | `yenile` |
| `info` | `bilgi` |
| `help` | `yardim` |
| `admin reset` | `sifirla` |
| `admin lang` | `dil` |

So `/nightmarket yenile` and `/nightmarket refresh` run the same paid-reroll
action, and `/nightmarket kapat` closes the market just like `close`.

## Custom Command Example

```yaml
commands:
  gece:
    aliases: [nm, market]
    permission: ""
    description: "Gece pazari - herkese acik!"
    action: showcase
```

This adds `/nightmarket gece`, `/nightmarket nm` and `/nightmarket market`,
all opening the market. The base command `/nightmarket` itself (with its
aliases) is fixed in `plugin.yml`.

{% hint style="warning" %}
After editing, run `/nightmarket admin reload` so the registry re-reads the
file. Unknown tokens fall back to the help screen.
{% endhint %}
