# Building

Requirements: **JDK 17+** and **Maven 3.8+**. No other setup — all Minecraft
and library dependencies resolve from Maven repositories.

## Compile

```bash
mvn clean package
```

Output:

```
target/EnsNightMarket-2.1.0.jar
```

The jar is already shaded (Kotlin stdlib + SQLite bundled and relocated under
`dev.ensisdev.ensnightmarket.libs.*`), so it drops straight into `plugins/`.

## Baseline

| | |
| --- | --- |
| Language | Kotlin 2.1.20, JVM target 17 |
| Compile API | Paper API 1.20.1 (runs on Paper **1.20+**; Folia is not supported) |
| Bytecode | Java 17 |

Display/Interaction entity APIs used are stable across 1.20–1.21, and every
integration (Vault, PlayerPoints, PlaceholderAPI, Oraxen, ItemsAdder, Nexo,
MMOItems, HeadDatabase, WorldGuard) is hooked **via reflection only** — so
none of them are compile dependencies and none are required at runtime unless
you use the matching feature.

## Server Dependencies

| Dependency | When needed |
| --- | --- |
| Paper 1.20+ | Always |
| Vault + a Vault economy | When `economy.provider: VAULT` (default) |
| PlayerPoints | When `economy.provider: PLAYER_POINTS` |
| PlaceholderAPI | Only for `%ensnightmarket_...%` placeholders |
| Oraxen / ItemsAdder / Nexo / MMOItems / HeadDatabase | Only for custom-item / `HDB` rewards |
| WorldGuard | Only for region protection |

Storage needs nothing extra: SQLite works out of the box, MySQL/MariaDB only
needs a reachable server (see [Storage](../storage.md)).

## From Source to Server

```bash
mvn clean package
cp target/EnsNightMarket-2.1.0.jar /path/to/server/plugins/
# restart the server once, then configure plugins/EnsNightMarket/
```
