# Placeholders

With PlaceholderAPI installed, EnsNightMarket registers the `ensnightmarket`
expansion automatically (no compile-time dependency - it hooks via a dynamic
proxy, works with PAPI 2.10+ on 1.20/1.21).

All placeholders require a player context: `%ensnightmarket_<key>%`.

## Market

| Placeholder | Example | Description |
| --- | --- | --- |
| `%ensnightmarket_refresh%` | `3g 2sa 45dk 12sn` | Time until the market auto-refreshes |
| `%ensnightmarket_refresh_seconds%` | `9912` | Same, in raw seconds |
| `%ensnightmarket_refresh_hours%` | `2` | Same, in whole hours |
| `%ensnightmarket_offers%` | `6` | Number of offers in the current market |
| `%ensnightmarket_revealed%` | `2` | Revealed offers |
| `%ensnightmarket_unrevealed%` | `4` | Unrevealed offers |
| `%ensnightmarket_remaining%` | `9` | Total stock remaining across offers |
| `%ensnightmarket_purchased%` | `1` | Offers fully bought (purchased flag) |
| `%ensnightmarket_purchases%` | `3` | Total purchase count across offers |

## Schedule

| Placeholder | Example | Description |
| --- | --- | --- |
| `%ensnightmarket_schedule_open%` | `true` | Is the night window currently open (in the player's world) |
| `%ensnightmarket_schedule_enabled%` | `false` | Is the schedule feature enabled |

## Session

| Placeholder | Example | Description |
| --- | --- | --- |
| `%ensnightmarket_session_active%` | `true` | Does the player have a live showcase session |
| `%ensnightmarket_session_remaining%` | `7dk 30sn` | Session time left (`∞` if unlimited) |
| `%ensnightmarket_session_remaining_seconds%` | `450` | Session seconds left (`-1` if unlimited) |

## Refresh / Economy

| Placeholder | Example | Description |
| --- | --- | --- |
| `%ensnightmarket_refresh_price%` | `$500` | Paid reroll price (formatted) |
| `%ensnightmarket_refresh_enabled%` | `true` | Is paid reroll enabled |
| `%ensnightmarket_economy%` | `Vault` | Active economy provider name |
| `%ensnightmarket_balance%` | `$12,345` | Player balance (formatted) |

## Notes

- Empty string is returned for players without data or console usage.
- Duration placeholders use the language file units (`unit-day`, `unit-hour`,
  `unit-min`, `unit-sec`).
- The expansion is registered/unregistered with the plugin (persist mode on),
  so `/papi reload` is unnecessary after plugin reloads.
