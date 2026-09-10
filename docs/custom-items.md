# Custom Items

Offers can sell items from popular custom-item plugins. The prefix decides which
provider resolves the item:

| Prefix | Plugin | Example |
| --- | --- | --- |
| `oraxen:` | Oraxen | `material: "oraxen:golden_backpack"` |
| `itemsadder:` or `ia:` | ItemsAdder | `material: "itemsadder:ruby_sword"` |
| `nexo:` | Nexo | `material: "nexo:amber_pickaxe"` |
| `mmoitems:` | MMOItems | `material: "mmoitems:SWORD:Excalibur"` (`TYPE:ID`) |
| `hdb:` | HeadDatabase | `material: "hdb:12345"` |

## Setup

1. Install the provider plugin on the server.
2. Reference the item in `offers.yml`:

```yaml
  ruby_sword:
    display-name: "<red>Ruby Sword"
    material: "itemsadder:ruby_sword"
    amount: 1
    base-price: 5000.0
    discount-min: 5
    discount-max: 15
    stock-min: 1
    stock-max: 1
    rarity: legendary
```

## Behavior

- Providers are detected via reflection at startup; a missing provider only
  disables its prefix.
- If the provider is missing or the id does not resolve, the offer is skipped at
  roll time with a console warning - the market never crashes.
- Resolved custom items are used both as the reward stack and (once revealed) as
  the floating display item.

## Vanilla Namespaced Items

Plain Bukkit material names (`DIAMOND`, `ENCHANTED_GOLDEN_APPLE`) always work
without any provider plugin.
