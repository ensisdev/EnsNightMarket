# rarities.yml

Rarities define how market heads look, float and feel. Every market head is a
custom player head texture, colored by its rarity.

```yaml
rarities:
  common:
    display-name: "<green>Yaygın"
    weight: 50.0
    color: "#55FF55"
    head:
      type: TEXTURE_VALUE
      value: "eyJ0ZXh0dXJlcyI6..."   # Base64 skin texture
    effects:
      particle: VILLAGER_HAPPY
      particle-count: 3
      sound: BLOCK_NOTE_BLOCK_PLING
      pitch: 1.0
```

## Keys

| Key | Description |
| --- | --- |
| `display-name` | MiniMessage string shown in holograms and messages |
| `weight` | Roll weight. Odds = weight / total weight. Defaults: 50 / 30 / 15 / 4 / 1 |
| `color` | Hex color used for dust particles, text and UI accents |
| `head.type` | Texture type, see below |
| `head.value` | The texture payload for the chosen type |
| `effects.particle` | Bukkit particle enum used for reveal + idle effects |
| `effects.particle-count` | Base particle amount (scaled by quality and LOD) |
| `effects.sound` | Sound enum played on reveal |
| `effects.pitch` | Sound pitch |

## Head Texture Types

| Type | `value` format |
| --- | --- |
| `TEXTURE_VALUE` / `BASE64` | Full Base64 texture from minecraft-heads.com ("Value" field, starts with `eyJ0ZXh0dXJlcyI6`) |
| `RAW` / `VALUE` | Only the texture id, or a direct texture URL |
| `URL` / `TEXTURE_URL` | `http://textures.minecraft.net/texture/<id>` |
| `HDB` | HeadDatabase head id (HeadDatabase must be installed) |

Unknown types fall back to a plain player head with a console warning - the
market keeps working.

## Custom Rarity Example

```yaml
rarities:
  mythic:
    display-name: "<gradient:#FF5555:#FFAA00>Mythic</gradient>"
    weight: 0.5
    color: "#FF3344"
    head:
      type: HDB
      value: "12345"
    effects:
      particle: DRAGON_BREATH
      particle-count: 12
      sound: ENTITY_ENDER_DRAGON_AMBIENT
      pitch: 0.8
```

Then reference it: in `config.yml` add idle motion / reveal shape entries under
`animation.motion-pattern.mythic` and `animation.reveal-shape.mythic`, and assign
`rarity: mythic` to offers in `offers.yml`. Note that special reveal effects in
code are hard-wired for the ids `legendary` and `mysterious` (flame ring, totem
spiral, dragon growl), so extra cinematic layers only trigger for those ids.

See [Custom Heads](../custom-heads.md) for the full head texture guide.
