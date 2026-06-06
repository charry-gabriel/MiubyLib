# MiubyLib

A personal utility library for Minecraft plugin development — built to avoid rewriting the same boilerplate across every project.

[![](https://jitpack.io/v/charry-gabriel/MiubyLib.svg)](https://jitpack.io/#charry-gabriel/MiubyLib)

---

## Requirements

- Java 25+
- Paper 26.1+

---

## Installation

Add JitPack to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency in `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.charry-gabriel:MiubyLib:<version>")
}
```

> **Shading** — MiubyLib must be shaded into your plugin JAR. Relocate the package if multiple plugins on the same server use it.

---

## Getting Started

Call `MiubyLib.init(this)` once in your plugin's `onEnable()`, before using anything else:

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        MiubyLib.init(this);
    }
}
```

---

## Modules

### `MiubyLib` — Core / Scheduler

Static helpers for the Bukkit scheduler, the plugin logger and event dispatch.

| Method | Description |
|---|---|
| `init(plugin)` | Initialize the library — call once in `onEnable()` |
| `runTask(task)` | Run on the next tick (sync) |
| `runLater(task, delay)` | Run after `delay` ticks (sync) |
| `runTaskTimer(task, delay, period)` | Repeating task every `period` ticks (sync) |
| `runAsync(task)` | Run off the main thread (DB / IO) |
| `callEvent(event)` | Fire a Bukkit event |
| `getLogger()` | Get the plugin logger |
| `getDataFolder()` | Get the plugin data folder |

```java
MiubyLib.runLater(() -> player.sendMessage("Hello!"), 20L);
MiubyLib.runAsync(() -> database.save(playerData));
MiubyLib.callEvent(new MyCustomEvent(player));
```

---

### `MLLogManager` — Tag-based Logging

Log messages filtered by **category (tag)** and **level** simultaneously. Outputs are written to rotating files inside `plugins/<Plugin>/logs/`:

| File | Content | Retention |
|---|---|---|
| `<plugin>-debug-%g.log` | Everything (ALL) | 10 × 10 MB |
| `<plugin>-info-%g.log` | INFO and above | 5 × 5 MB |
| `<plugin>-warn-%g.log` | WARNING and above | 5 × 2 MB |
| Console | WARNING and above | — |

**1. Declare your tags**

```java
public enum ELogTag implements ILogTag {
    PLAYER, WORLD, SYSTEM, QUEST
}
```

**2. Initialize (after `MiubyLib.init()`)**

```java
MLLogManager log = MLLogManager.getInstance();
log.registerTags(ELogTag.values());
log.initialize();               // in-memory only
// or:
log.initialize(myPersistence);  // with DB-backed persistence
```

**3. Log**

```java
log.log(Level.INFO,    ELogTag.SYSTEM, "Plugin started");
log.log(Level.WARNING, ELogTag.PLAYER, "Player data missing for " + uuid);
log.log(Level.SEVERE,  ELogTag.SYSTEM, "Critical error", exception);
```

**Presets** (also switchable at runtime via `MLLogCommand`):

```java
log.setDebugMode();       // everything on
log.setProductionMode();  // WARNING + SEVERE only
log.setQuietMode();       // SEVERE only
```

**Optional persistence** — implement `MLLogPersistence` to save tag/level states across restarts:

```java
public class MyLogPersistence implements MLLogPersistence {
    @Override public Boolean getTagState(String name)           { return repo.getLogTagState(name); }
    @Override public void saveTagState(String name, boolean e)  { repo.saveLogTagState(name, e); }
    @Override public Boolean getLevelState(String name)         { return repo.getLogLevelState(name); }
    @Override public void saveLevelState(String name, boolean e){ repo.saveLogLevelState(name, e); }
}
```

---

### `MLLogCommand` — In-game Log Management

A ready-to-plug Brigadier sub-command exposing tag/level controls to ops at runtime, with no coupling to your specific tag enum.

```java
return Commands.literal("myplugin")
        .requires(s -> s.getSender().isOp())
        .then(MLLogCommand.create())   // → /myplugin log ...
        .then(/* your other subcommands */);
```

Exposed sub-commands:

```
/myplugin log status
/myplugin log tag   toggle|enable|disable <TAG>
/myplugin log level toggle|enable|disable <LEVEL>
/myplugin log mode  production|debug|quiet
```

---

### `MLStringArgument<T>` — Brigadier Argument Base

Eliminates the boilerplate of typed Brigadier arguments that parse a single word. Implement `convert()` and `suggestions()`.

```java
public class RoleArgument extends MLStringArgument<Role> {

    public static RoleArgument role() { return new RoleArgument(); }

    @Override
    public Role convert(String value) throws CommandSyntaxException {
        Role role = RoleRegistry.get(value);
        if (role == null) throw CommandErrors.ROLE_NOT_FOUND.create(value);
        return role;
    }

    @Override
    protected Collection<String> suggestions() {
        return RoleRegistry.getAll().stream().map(Role::name).toList();
    }
}
```

For context-dependent suggestions, override `listSuggestions()` directly.

---

### `MLBrigadierHelper` — Exception Factories

```java
// "Player not found: Steve" when throw PLAYER_NOT_FOUND.create("Steve")
public static final DynamicCommandExceptionType PLAYER_NOT_FOUND = MLBrigadierHelper.notFound("Player");

public static final SimpleCommandExceptionType INVALID_STATE = MLBrigadierHelper.simpleError("Invalid state.");
```

---

### `MLSQLite` — SQLite with Schema Versioning

Abstract base for SQLite databases. Handles file creation, connection management, schema versioning via `PRAGMA user_version`, and migrations.

```java
public class MyDatabase extends MLSQLite {
    private static final int TARGET_VERSION = 2;
    private PlayerRepository playerRepo;

    public MyDatabase() {
        super("myplugin"); // opens plugins/MyPlugin/myplugin.db
    }

    @Override protected int getTargetVersion() { return TARGET_VERSION; }

    @Override
    protected void createTables() throws SQLException {
        try (Statement s = getConnection().createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS player (uuid TEXT PRIMARY KEY, name TEXT NOT NULL)");
        }
    }

    @Override
    protected void runMigrations(int currentVersion) throws SQLException {
        try (Statement s = getConnection().createStatement()) {
            if (currentVersion < 2)
                s.executeUpdate("ALTER TABLE player ADD COLUMN score INT NOT NULL DEFAULT 0");
        }
    }

    @Override
    protected void onLoaded() {
        playerRepo = new PlayerRepository(getConnection(), this);
    }

    public PlayerRepository players() { return playerRepo; }
}
```

```java
// In onEnable():
MyDatabase db = new MyDatabase();
db.load(); // creates file, runs migrations, calls onLoaded()
```

---

### `MLRepository` — Async Repository Base

Provides a persistent sync connection for reads and a fire-and-forget async runner for writes.

```java
public class PlayerRepository extends MLRepository {

    public PlayerRepository(Connection connection, MLSQLite db) {
        super(connection, db);
    }

    // Sync read — uses the persistent connection
    public String loadName(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM player WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("name") : null;
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.PLAYER, "Failed to load", ex);
            return null;
        }
    }

    // Async write — fresh connection, auto-closed, errors auto-logged
    public void saveName(UUID uuid, String name) {
        runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO player (uuid, name) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.executeUpdate();
            }
        }, ELogTag.PLAYER, "Failed to save");
    }
}
```

---

### `MLResourceManager` — YAML Resource Management

Deploy YAML files from your JAR to disk on startup (MD5-checked, no unnecessary overwrites), then load them as POJOs with a built-in cache.

```java
// In onEnable() — can be called before MiubyLib.init()
MLResourceManager.deploy(this, "config.yml");
MLResourceManager.deployFolder(this, "villagers"); // deploys all .yml in the folder
MLResourceManager.deployFolder(this, "quests");

// Load as POJO (result is cached)
VillagerConfig cfg = MLResourceManager.loadPojo(plugin, "villagers", "trader_bob", VillagerConfig.class);
List<QuestConfig> quests = MLResourceManager.loadPojoAll(plugin, "quests", QuestConfig.class);

// On hot-reload
MLResourceManager.clearCache();
```

---

### `MLPlayerRegistry<T>` — Player Registry

Generic registry indexed by both UUID and username.

```java
public class MyPlayer extends MLPlayer {
    public MyPlayer(UUID uuid) { super(uuid); }
}

private final MLPlayerRegistry<MyPlayer> players = new MLPlayerRegistry<>();

// On join
MyPlayer p = new MyPlayer(player.getUniqueId());
p.setPseudo(player.getName());
p.setPlayer(player);
players.register(p);

// Lookup
players.get(player.getUniqueId());
players.get("Steve");
players.get(player);     // shortcut
players.getAll();
players.unregister(p);
```

---

### `MLVillager` — Custom Persistent Villagers

Abstract class for NPC villagers that automatically save, load and respawn across restarts.

**Features:**
- Automatic save/load lifecycle
- Chunk-aware retry system (10 attempts × 10 ticks) before force-respawning
- AI, collision and sounds disabled by default
- Auto-registered in `VillagerRegistry`
- `VillagerLoadedEvent` fired when the villager is ready

```java
public class TraderBob extends MLVillager {

    public TraderBob() {
        super("trader_bob", Villager.Type.PLAINS, Villager.Profession.FARMER);
    }

    @Override
    protected MLVillagerData loadData() {
        return database.villagers().load("trader_bob"); // null on first spawn
    }

    @Override
    protected void saveData() {
        database.villagers().save("trader_bob", getVillagerData());
    }

    @Override
    protected MLVillagerData createDefaultData() {
        MLVillagerData data = new MLVillagerData();
        data.setLocation(spawnLocation);
        return data;
    }

    @Override
    protected void onInitialized() {
        super.onInitialized(); // applies display name, registers, fires event
        // custom setup...
    }
}

// Always create via spawn(), never new directly
TraderBob bob = MLVillager.spawn(TraderBob::new);
```

**VillagerRegistry:**

| Method | Description |
|---|---|
| `get(UUID)` | Get by Bukkit entity UUID |
| `get(String)` | Get by `nameId` |
| `getAll()` | Get all registered villagers |
| `contains(UUID/String)` | Check if registered |
| `unregister(villager)` | Remove from registry |

---

### `MLWorld` — World Wrapper

Wraps a Bukkit `World` with display metadata and an optional zone boundary.

```java
MLWorld lobby = new MLWorld(
    Bukkit.getWorld("lobby"),
    "Lobby",
    NamedTextColor.GOLD,
    EWorldType.LOBBY        // enum implementing WorldType
);

lobby.setLimit(Rect.of(locationA, locationB));
lobby.setLocked(true);

// Checks
lobby.isPlayerInWorld(player);
lobby.isPlayerOutOfLimit(player); // false if no limit is set

// Register
WorldRegistry.register(lobby);
```

**WorldRegistry:**

| Method | Description |
|---|---|
| `get(UUID)` | Get by Bukkit world UUID |
| `get(String)` | Get by display name |
| `get(WorldType)` | Get by type |
| `get(Player)` | Get the world a player is currently in |
| `getAll()` | Get all registered worlds |
| `isPlayerInRegisteredWorld(player)` | Check if the player's world is registered |

---

### Utilities

#### `Cooldown<K>` — Generic Cooldown

```java
private final Cooldown<UUID> cooldown = new Cooldown<>(1_500L); // 1.5 seconds

if (!cooldown.isOnCooldown(player.getUniqueId())) {
    cooldown.set(player.getUniqueId());
    // perform action
}

cooldown.remaining(uuid); // ms left
cooldown.reset(uuid);     // force expire
```

#### `MultiKeyRegistry<V>` — Multi-key Map

Associate a single value with multiple lookup keys of any type.

```java
MultiKeyRegistry<MyObject> registry = new MultiKeyRegistry<>();
registry.register(obj, obj.getUUID(), obj.getName(), obj.getType());

registry.get(uuid);
registry.get("name");
registry.unregister(obj);   // removes all keys pointing to obj
registry.getAll();          // deduplicated collection
```

#### `Rect` — 3D Region

```java
// Build from two Bukkit locations (min/max auto-resolved)
Rect zone = Rect.of(locationA, locationB);

// Or manually: xMax, xMin, yMax, yMin, zMax, zMin
Rect zone = new Rect(100, -100, 255, 0, 100, -100);

zone.contains(player.getLocation());
zone.isOut(x, y, z);
zone.expand(5); // grow 5 blocks in every direction
```

---

## Dependencies

- [Paper API](https://papermc.io/) — Bukkit / Brigadier
- [Lombok](https://projectlombok.org/)
- [Adventure](https://docs.advntr.net/) (bundled with Paper)
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) (bundled with Bukkit)
