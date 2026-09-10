# Custom Heads

Every market head is a custom player head. Textures come from `rarities.yml`
under each rarity:

```yaml
head:
  type: TEXTURE_VALUE
  value: "eyJ0ZXh0dXJlcyI6..."   # minecraft-heads.com "Value" field
```

## Head Types

| Type | `value` format |
| --- | --- |
| `TEXTURE_VALUE` / `BASE64` | Full Base64 texture (starts with `eyJ0ZXh0dXJlcyI6`). Copy straight from the "Value" field on minecraft-heads.com |
| `RAW` / `VALUE` / `TEXTURE_ID` | Just the texture id (`f18612fd...`) or a full URL; the plugin wraps it for you |
| `URL` / `TEXTURE_URL` | Full `http(s)://textures.minecraft.net/texture/...` URL |
| `HDB` | HeadDatabase head id (requires the HeadDatabase plugin) |

- Blank or invalid values fall back to a **plain player head** with a console
  warning - the market keeps working.
- Texture values are cached. After editing `rarities.yml`, run
  `/nightmarket reload` and re-showcase (`/nightmarket` or relog).
- Once an offer is revealed, the floating display swaps the rarity chest head
  for the actual reward item.

## Where to Find Textures

- [minecraft-heads.com](https://minecraft-heads.com) - copy the "Value" field
  (Base64) and use `type: TEXTURE_VALUE`.
- HeadDatabase in-game heads: use `type: HDB` with the head id as value.

## Why Base64?

The Base64 "Value" field is the raw skin payload the server puts on the player
head. It is stable, needs no HTTP fetches at runtime, and works even when the
skin server is slow - that's why it is the default and recommended type.
