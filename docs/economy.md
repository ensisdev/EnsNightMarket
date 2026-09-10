# Economy

EnsNightMarket supports two economy backends, selected in `config.yml`:

```yaml
economy:
  provider: VAULT   # VAULT or PLAYER_POINTS
```

## Vault

- Install Vault plus any Vault economy (EssentialsX Economy, CMI, XConomy...).
- Used for offer prices, paid refresh (`refresh.price`) and balance checks.
- The active provider name appears in `/nightmarket info` and the
  `%ensnightmarket_economy%` placeholder.

## PlayerPoints

- Install PlayerPoints and set `economy.provider: PLAYER_POINTS`.
- Prices are paid in points; `%ensnightmarket_balance%` shows the point balance.

## Behavior

- If no usable provider is found at startup, the plugin logs a warning and
  starts in a degraded state (all purchases fail) until an economy exists.
- All balance changes run on the main thread inside the per-player purchase
  lock, so double-clicks cannot double-charge.
- Prices are formatted through the provider (`economy.format()`), so currency
  names/symbols match your economy plugin.

## Price Math

An offer's price is rolled per player per refresh:

```
final price = base-price * (1 - discount% / 100)
```

`discount-min` / `discount-max` come from the offer in `offers.yml`. The rolled
price, stock and purchase counters persist in storage until the market
refreshes (`market.refresh-hours`) or the player pays for a reroll.
