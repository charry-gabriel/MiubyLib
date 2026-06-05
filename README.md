# MiubyLib

A personal utility library for Minecraft plugin development — built to avoid rewriting the same boilerplate across every project.

[![](https://jitpack.io/v/charry-gabriel/MiubyLib.svg)](https://jitpack.io/#charry-gabriel/MiubyLib)

---

## Installation

Add JitPack to your `settings.gradle`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.github.charry-gabriel:MiubyLib:<version>")
}
```

---

## Getting Started

Call `MiubyLib.init(this)` in your plugin's `onEnable()` before using anything else:

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

### MLVillager — Custom Persistent Villagers

An abstract class for creating custom villagers that automatically save, load, and respawn across server restarts.

**Features:**
- Automatic save/load lifecycle
- Chunk-aware retry system (10 attempts, 10 ticks apart) before respawning
- AI, collision and sounds disabled by default
- Event fired when the villager is ready: `VillagerLoadedEvent`

**Usage:**

```java
public class MyVillager extends MLVillager {
    public MyVillager() {
        super("my_unique_id", Villager.Type.PLAINS, Villager.Profession.FARMER);
    }

    @Override
    protected MLVillagerData loadData() {
        // Load from file/database
        return data;
    }

    @Override
    protected void saveData() {
        // Save to file/database
    }

    @Override
    protected MLVillagerData createDefaultData() {
        return new MLVillagerData(spawnLocation, null);
    }

    @Override
    protected void onInitialized() {
        super.onInitialized();
        // Called when the villager is fully loaded and ready
    }
}

// Registering
MyVillager villager = MLVillager.create(MyVillager::new);
VillagerRegistry.register(villager);
```

**VillagerRegistry:**

| Method | Description |
|---|---|
| `register(villager)` | Register a villager |
| `get(UUID)` | Get by UUID |
| `get(String)` | Get by name/id |
| `getAll()` | Get all registered villagers |

---

### MLWorld — World Wrapper

A wrapper around Bukkit's `World` with extra features like custom names, colors, spawn points, and region limits.

**Usage:**

```java
MLWorld world = new MLWorld(
    bukkitWorld,
    "Custom Name",
    NamedTextColor.RED,
    WorldType.OVERWORLD
);

world.setSpawnPoint(location);
world.setLimit(new Rect(100, -100, 255, 0, 100, -100)); // xMax, xMin, yMax, yMin, zMax, zMin
world.setLocked(true);

// Checks
world.isPlayerInWorld(player);
world.isPlayerOutOfLimit(player); // returns false if no limit is set

// Register
WorldRegistry.register(world);
```

**WorldRegistry:**

| Method | Description |
|---|---|
| `register(world)` | Register a world |
| `get(UUID)` | Get by UUID |
| `get(String)` | Get by name |
| `getAll()` | Get all registered worlds |

---

### Rect — 3D Region

A simple record defining a rectangular 3D zone.

```java
// 200x200 square centered on 0,0
new Rect(100, -100, 255, 0, 100, -100); // xMax, xMin, yMax, yMin, zMax, zMin

rect.isOut(x, y, z); // true if the point is outside the bounds
```

---

### MiubyLib — Main Class

| Method | Description |
|---|---|
| `init(plugin)` | Initialize the library |
| `runLater(Runnable, delay)` | Schedule a delayed task |
| `getLogger()` | Get the plugin logger |
| `callEvent(Event)` | Fire a custom event |

---

## Dependencies

- Bukkit / Spigot API
- [Lombok](https://projectlombok.org/)
- [Kyori Adventure](https://docs.advntr.net/)

---

## Project Structure

```
MiubyLib/
└── src/main/java/fr/miuby/lib/
    ├── MiubyLib.java
    ├── utils/
    │   └── Rect.java
    ├── villager/
    │   ├── MLVillager.java
    │   ├── MLVillagerData.java
    │   ├── VillagerLoadedEvent.java
    │   └── VillagerRegistry.java
    └── world/
        ├── MLWorld.java
        ├── WorldRegistry.java
        └── WorldType.java
```