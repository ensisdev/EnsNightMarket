# Admin GUI

`/nightmarket admin` opens a fully in-game management interface. Add a player
name (`/nightmarket admin Steve`) to jump straight into that player's market.
Console can use the `refresh` / `reset` / `reload` subcommands directly.

{% hint style="info" %}
Every admin GUI action requires `ensnightmarket.admin`. In the offer and
rarity lists, **left-click** edits, **right-click** asks for delete
confirmation, and **middle-click** toggles enabled/disabled.
{% endhint %}

## Offers

Browse every offer from `offers.yml` with pagination (28 per page):

- **Add from hand** — takes the item in your main hand as the reward with
  sane defaults (price 100, 10–50% discount, stock 1–2), then asks you to pick
  a rarity.
- **Add manually** — type a material or custom-item id in chat
  (`DIAMOND_SWORD`, `oraxen:golden_backpack`, `mmoitems:SWORD:Excalibur`…);
  invalid input is rejected with a hint.
- **Edit** — display name, material, amount (1–64), base price, discount
  min/max, stock min/max, rarity, per-offer `permission`, `max-purchases`
  limit, lore line editor, and on/off toggle.
- **Delete** — with a confirmation screen.

Chat input closes your inventory and listens for one message (`iptal` /
`cancel` aborts; lore mode ends with `bitti` / `done`, `temizle` clears).
Inputs time out after 60 seconds (180 for lore).

## Rarities

Card per rarity showing weight, particle, sound and head preview:

- **Add** — type a new id in chat (e.g. `mythic`).
- **Edit** — display name, weight (roll odds), color (`#RRGGBB`), particle
  type + count, sound + pitch, floating scale, idle aura on/off, and head
  texture via three buttons: long Base64 value, `HDB` id, or full texture URL.
- **Delete** — blocked while any offer still uses the rarity (`rarity-used`).

New rarities work everywhere immediately: add matching
`motion-pattern.<id>` / `reveal-shape.<id>` rows in `config.yml` to give them
their own animation, and reference the id from any offer's `rarity` key
(or keep `random` for weighted rolls).

## Settings

Live-edit the core numbers from `config.yml` without touching YAML: slot
count (1–54), refresh hours, unique-offers toggle, schedule on/off + open /
close ticks, economy provider flip (Vault ↔ PlayerPoints), paid-refresh
toggle + price + cooldown, session duration, follow toggle, and animation
quality cycle (`LOW → MEDIUM → HIGH → ULTRA`).

{% hint style="warning" %}
Settings save to disk right away, but animation changes only fully apply
after `/nightmarket admin reload`.
{% endhint %}

## Player Market

Inspect any player's live market: rolled prices, discounts, stock, revealed
state and purchase counts — then **refresh** (free reroll), **reveal all**,
or **reset** (full wipe) that player.

## Tips

- The GUI edits live data; player-market changes apply immediately.
- Large edits are easier in YAML — the GUI is for quick fixes and moderation.
- Every `offers.yml` / `rarities.yml` write first backs the file up to
  `offers.yml.bak` / `rarities.yml.bak` (Bukkit rewrites drop file comments).
- All GUI labels come from the `gui-*` keys in your language file, so they
  translate automatically with the language switch.
