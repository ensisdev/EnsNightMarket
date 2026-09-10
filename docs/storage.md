# Storage

Player markets are persisted so timers, discounts, stock and purchase history
survive restarts.

```yaml
storage:
  type: SQLITE # SQLITE or MYSQL
  mysql:
    host: localhost
    port: 3306
    database: ensnightmarket
    username: root
    password: change-me
```

## SQLite (default)

- Zero setup; creates `plugins/EnsNightMarket/markets.db`.
- Perfect for small/medium servers.
- Select with `storage.type: SQLITE`.

## MySQL / MariaDB

- Set `storage.type: MYSQL` and fill the `mysql:` block.
- Use this for networks, multi-server setups or where you want external backups.
- The plugin manages its own connection pool and reopens dropped connections.

{% hint style="danger" %}
The stock `mysql.password` is `change-me`. Set a real password (and a
dedicated DB user) before switching to MySQL — the file sits on disk in
plain text like every Bukkit config.
{% endhint %}

## What Is Stored

- Player UUID, market offer list (ids, rarity, price, stock, purchases,
  revealed flags), and the market refresh timestamp.
- Written on every relevant change; `performance.async-storage: true` moves
  writes off the main thread (recommended).

## Switching Backends

1. Stop the server.
2. Change `storage.type`.
3. Start the server - the new backend initializes its schema automatically.

There is no built-in SQLite->MySQL migration; if you move, players simply get
fresh markets on first open (or copy data manually if you need history).

## Performance Notes

- Keep `async-storage: true` unless debugging.
- MySQL over a slow link can stall async writers; host the DB near the server.
