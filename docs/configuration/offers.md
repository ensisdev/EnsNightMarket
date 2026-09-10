# offers.yml

Offers are the purchasable items that appear on market heads. Six defaults ship
with the plugin; add as many as you like.

```yaml
offers:
  enchanted_golden_apple:
    display-name: "<yellow>Enchanted Golden Apple"
    material: ENCHANTED_GOLDEN_APPLE
    amount: 1
    base-price: 1500.0
    discount-min: 30
    discount-max: 55
    stock-min: 1
    stock-max: 2
    rarity: random
    max-purchases: 0
```

## Keys

| Key | Default | Description |
| --- | --- | --- |
| `display-name` | - | MiniMessage name shown on the hologram |
| `material` | - | Bukkit material, or a custom item id (see below) |
| `amount` | 1 | Items given per purchase |
| `base-price` | - | Full price before discount |
| `discount-min` / `discount-max` | 0 | Discount % range rolled per player per refresh |
| `stock-min` / `stock-max` | 1 | Stock range rolled per refresh; stock hits 0 = cannot buy |
| `rarity` | random | A rarity id from `rarities.yml`, or `random` for weighted roll |
| `max-purchases` | 0 | Per-player purchase limit; `0` = unlimited |
| `permission` | - | Optional: buyer needs this permission node |

Every player gets their **own** rolled discount and stock per offer; these are
persisted in storage and rerolled when the market refreshes.

## Custom Item Materials

When the provider plugin is installed, `material` accepts:

```yaml
material: "oraxen:golden_backpack"
material: "itemsadder:ruby_sword"     # also "ia:ruby_sword"
material: "nexo:amber_pickaxe"
material: "mmoitems:SWORD:Excalibur"  # TYPE:ID
```

If the provider is missing or the id is wrong, the offer is skipped with a
console warning at roll time.

## Custom Lore

Offers can carry extra lore lines shown under the price on the hologram:

```yaml
  netherite_ingot:
    lore:
      - "<gray>Hot from the depths!"
```

## Tips

- Keep enough offers defined so weighted rolls stay interesting; `market.slots: 6`
  picks six distinct offers per refresh when `unique-offers: true`.
- Jackpot offers: tiny `stock-max`, huge `base-price`, `rarity: legendary`.
- Budget items: large `stock-max`, small `discount-max`.
