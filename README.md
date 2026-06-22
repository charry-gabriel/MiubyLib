# MiubyLib

A Paper plugin library that handles the infrastructure so you can focus on game logic.

MiubyLib provides a set of cohesive, opinionated abstractions for the recurring pain points of Paper plugin development: structured logging, SQLite with schema migrations, YAML-driven translations, NPC management, world metadata, and more — all wired together through a single static entry point.

---

## Modules

### Core
A single static class (`MiubyLib`) initialized once in `onEnable()`. It exposes scheduler shortcuts (`runTask`, `runLater`, `runTaskTimer`, `runAsync`), the plugin logger, the data folder, and an event dispatcher — so you never have to pass a `JavaPlugin` instance around your codebase.

### Logging — `MLLogManager`
Category-based logging built on JUL. Messages are filtered by **tag** (an enum you define) and by **level** (JUL standard levels), and routed to three rotating file outputs:

| File | Content | Retention |
|---|---|---|
| `debug-%g.log` | Everything, including stack traces | 10 × 10 MB |
| `info-%g.log` | INFO and above | 5 × 5 MB |
| `warn-%g.log` | WARNING and above | 5 × 2 MB |

The console only receives WARNING and above, keeping server output clean. Built-in presets (`setDebugMode()`, `setProductionMode()`, `setQuietMode()`) let you switch verbosity without restarting. Tag and level states can optionally be persisted to a database via `MLLogPersistence`.

### SQLite — `MLSQLite` & `MLRepository`
An abstract base layer for SQLite databases. Extend `MLSQLite` to get automatic schema versioning via `PRAGMA user_version`, WAL mode, a busy timeout, and a clean migration lifecycle (`createTables` → `runMigrations` → `onLoaded`). The library also supports inheritance chains — a useful pattern for separating table definitions from repository wiring.

`MLRepository` is the companion base class for data access objects. It provides `runAsync(SQLTask)` — a one-liner for offloading writes to an async thread with a fresh, auto-closed connection and integrated error logging.

Resilient connections are available via `getResilientConnection()`, a transparent proxy that auto-reconnects if the underlying connection is lost, so repositories initialized in `onLoaded()` never hold stale references.

### Messaging — `MLMessageService`
A translation service backed by YAML files and rendered with [MiniMessage](https://docs.advntr.dev/minimessage/). Each locale lives in a separate `.yml` file under a resource folder you define. Keys support positional string arguments (`{0}`, `{1}`, …) and Adventure `TagResolver` components for rich inline formatting.

Missing keys never crash the server — they log a warning once per key and fall back gracefully through: *requested locale → default locale → visible placeholder*. A mono-language mode is also available if internationalization is not needed.

### Resources — `MLResourceManager`
Handles deployment of YAML resources bundled inside the plugin JAR to the plugin's data folder. Files are only updated on disk when their SHA-1 hash differs from the JAR version. A POJO loader backed by SnakeYAML lets you map YAML files to Java objects with a built-in result cache.

### Commands — Brigadier utilities
`MLBrigadierHelper` provides factory methods for the two most common Brigadier exception types — `notFound(entityLabel)` and `simpleError(message)` — removing the boilerplate of serializing Adventure components to Brigadier messages.

`MLStringArgument` is a custom Brigadier argument type for string inputs with server-side suggestions and validation.

`MLLogCommand` is a built-in `/mllog` admin command for toggling log tags and levels at runtime.

### Players — `MLPlayer` & `MLPlayerRegistry`
A minimal generic player wrapper (`MLPlayer`) and a dual-key registry (`MLPlayerRegistry<T>`) that indexes players by both UUID and username for O(1) lookups in either direction.

### Villagers / NPCs — `MLVillager`
An abstract base for persistent NPC entities (using the Paper `MANNEQUIN` entity type). Handles the full lifecycle: spawning, persistence, chunk-aware loading, and automatic respawn after up to 10 retry attempts. Fires a `VillagerLoadedEvent` once an NPC is ready, and exposes hooks for inventory creation and custom destruction logic.

### Worlds — `MLWorld` & `WorldRegistry`
A thin wrapper around the Bukkit `World` object that adds a display name, a color, a type, an optional 3D zone limit (`Rect`), and a lock flag. `WorldRegistry` is a typed registry for these worlds.

### Utilities
- **`Cooldown<K>`** — a generic time-based cooldown keyed by any type, with `set`, `isOnCooldown`, `remaining`, and `reset`.
- **`Rect`** — an immutable 3D bounding box (Java record) built from two Bukkit `Location`s, with an `isOut(Location)` check.
- **`MultiKeyRegistry<T>`** — a generic registry that maps multiple keys of any type to a single object.

---

## Requirements

- **Paper** 26.2
- **Java** 25

---

## Installation

MiubyLib is distributed via [JitPack](https://jitpack.io).

**Gradle (Kotlin DSL)**
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.charry-gabriel:MiubyLib:v1.16")
}
```

**Maven**
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.charry-gabriel</groupId>
    <artifactId>MiubyLib</artifactId>
    <version>v1.16</version>
</dependency>
```
---

## Getting Started

Initialize the library once at the top of your plugin's `onEnable()`, before anything else:

```java
@Override
public void onEnable() {
    MiubyLib.init(this);
    // your plugin setup…
}
```

Every other module (`MLLogManager`, `MLSQLite`, `MLMessageService`, …) is available immediately after this call.

---

## Philosophy

MiubyLib is opinionated by design. It encodes the decisions made across multiple production plugins — logging strategy, database patterns, resource deployment — so those decisions don't have to be made (and debated) again in each project. If the defaults fit your server, setup is minimal. If they don't, most systems expose enough hooks and overrides to adapt without forking the library.