# EnsNightMarket 2.1.0 build notes

Build with:

    mvn clean package

Output:

    target/EnsNightMarket-2.1.0.jar

Compile baseline: Paper API 1.21.4, Java 17 bytecode. The implementation uses
stable Display/Interaction Entity APIs plus reflection-only hooks, so no
compile dependency on the integrations below is needed.

Required/optional server dependencies:
- Paper 1.21+
- Vault + a Vault economy when using `economy.provider: VAULT`
- PlayerPoints when using `economy.provider: PLAYER_POINTS`
- PlaceholderAPI is optional
- Oraxen / ItemsAdder / Nexo / MMOItems / HeadDatabase are optional (custom item rewards)
- WorldGuard is optional (region protection)

Storage:
- SQLite default
- MySQL/MariaDB via `storage.type: MYSQL`
