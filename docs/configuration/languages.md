# Languages

All player-facing text lives in `plugins/EnsNightMarket/lang/` as one file per
language. The active file is selected by `language` in `config.yml`:

```yaml
language: en_en   # or tr_tr
```

Ships with: `en_en.yml` (English) and `tr_tr.yml` (Turkish, also the fallback).

## Switching Language

In-game (needs `ensnightmarket.admin`):

```
/nightmarket admin lang        # show current language + available codes
/nightmarket admin lang tr_tr  # switch to Turkish
/nightmarket admin lang en_en  # switch to English
```

Accepted codes are forgiving: `tr`, `tr_tr`, `turkish`, `türkçe` → Turkish;
`en`, `en_en`, `english`, `ingilizce` → English. The switch applies instantly
without a restart.

## Message Format

Messages use **MiniMessage** (`<red>`, `<green>`, `<bold>`, gradients, click and
hover events) with a `<prefix>` placeholder prepended automatically, plus
`%name%`-style placeholders resolved per message:

```yaml
prefix: "<dark_gray>[<gradient:#9D4EDD:#FFD166>NightMarket</gradient><dark_gray>]<reset> "
revealed: "<green>Offer revealed: <white>%offer%</white>"
purchased: "<green>Purchased <white>%offer%</white> <gray>for <gold>%price%</gold> <gray>(stock: %stock%)"
insufficient: "<red>Not enough money. <gray>Need: <gold>%price%</gold>"
refresh-cooldown: "<red>Wait a little. <gray>Remaining: <white>%time%</white>"
info-line: "<gray>EnsNightMarket <white>%version%</white> <dark_gray>| <gray>%economy% <dark_gray>| <gray>%offers% offers"
```

Multi-line messages are YAML lists — one chat line per entry.

{% hint style="info" %}
On Paper, messages are sent as Adventure Components. On Spigot the plugin
falls back to legacy `§` colors automatically — MiniMessage tags still work in
the files either way.
{% endhint %}

## Message Groups

~240 keys per file, in matching pairs across both languages:

| Group | Examples |
| --- | --- |
| Status | `prefix`, `revealed`, `purchased`, `insufficient`, `sold-out`, `no-permission`, `purchase-limit`, `inventory-full`, `not-revealed`, `busy`, `not-found` |
| Market flow | `showcase`, `showcase-closed`, `schedule-open`, `schedule-closed`, `schedule-over`, `refresh-paid`, `refresh-cooldown`, `session-expired`, `protection-denied`, `reveal-hint` |
| Admin | `admin-saved`, `admin-added`, `admin-removed`, `admin-invalid`, `admin-cancelled`, `admin-timeout`, `admin-prompt`, `lore-hint`, `lore-cleared`, `no-item`, `exists`, `rarity-first`, `rarity-used` |
| Language | `lang-set`, `lang-invalid`, `lang-current` |
| Help / info | `info-line`, `help-player-title`, `help-admin-title`, `purchase-subtitle` |
| Animation debug | `anim-players-only`, `anim-preview`, `anim-stats-title`, `anim-quality-set`, `anim-patterns`, `anim-shapes`, `anim-reloaded`, ... |
| Hologram / reward | `holo-stock`, `holo-soldout`, `holo-price`, `reward-price`, `reward-brand` |
| Shared | `common-back`, `common-close`, `state-on`, `state-off`, `none` |
| GUI (60+ keys) | `gui-main-*`, `gui-offers-*`, `gui-offer-*`, `gui-edit-*`, `gui-rarities-*`, `gui-rarity-*`, `gui-select-*`, `gui-delete-*`, `gui-settings-*`, `gui-pm-*`, ... |
| Units / words | `unit-day/hour/min/sec`, `economy-points`, `words-cancel/done/clear` |

Duration placeholders (`%time%`, session/refresh timers) render with the unit
keys, so Turkish shows `2sa 45dk 12sn` while English shows `2h 45m 12s`.

## Adding Your Own Language

1. Copy `lang/en_en.yml` to `lang/de_de.yml` (any `xx_yy` name works).
2. Translate the values — keep the keys and `%placeholders%` untouched.
3. Set `language: de_de` in `config.yml` and run `/nightmarket admin reload`.

{% hint style="warning" %}
If a key is missing from the active file, the plugin falls back to `tr_tr`
and logs which keys were missing. Keep both files' key sets in sync.
{% endhint %}
