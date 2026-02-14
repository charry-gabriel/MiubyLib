# MiubyLib

Bibliothèque personnelle pour faciliter le développement de plugins Minecraft.

## C'est quoi ?

MiubyLib est une petite lib que j'ai créée pour éviter de réécrire toujours le même code dans mes plugins. Elle fournit des systèmes prêts à l'emploi pour gérer des villageois personnalisés et des mondes avec limites.

## Comment l'utiliser

### Initialisation

Dans ton plugin principal, appelle `MiubyLib.init(this)` dans `onEnable()` :

```java
public class MonPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        MiubyLib.init(this);
        // reste du code...
    }
}
```

## Modules disponibles

### 1. Système de Villageois Personnalisés (MLVillager)

Permet de créer des villageois custom qui persistent et se rechargent automatiquement.

**Fonctionnalités :**
- Sauvegarde/chargement automatique
- Persistance (le villageois reste même après redémarrage)
- Recherche automatique si le chunk n'est pas chargé (avec retry)
- AI désactivée, collision désactivée

**Comment créer un villageois custom :**

```java
public class MonVillager extends MLVillager {
    public MonVillager() {
        super("mon_id_unique", Villager.Type.PLAINS, Villager.Profession.FARMER);
    }
    
    @Override
    protected MLVillagerData loadData() {
        // Charge depuis fichier/DB
        return data;
    }
    
    @Override
    protected void saveData() {
        // Sauvegarde dans fichier/DB
    }
    
    @Override
    protected MLVillagerData createDefaultData() {
        // Retourne les données par défaut (location, etc.)
        return new MLVillagerData(location, null);
    }
    
    @Override
    protected void onInitialized() {
        super.onInitialized();
        // Appelé quand le villageois est prêt
    }
}

// Pour créer :
MonVillager villager = MLVillager.create(MonVillager::new);
VillagerRegistry.register(villager);
```

**VillagerRegistry :**
- `register(villager)` - Enregistre un villageois
- `get(UUID)` - Récupère par UUID
- `get(String)` - Récupère par nom
- `getAll()` - Récupère tous les villageois

**Event :**
- `VillagerLoadedEvent` - Déclenché quand un villageois est chargé/initialisé

### 2. Système de Mondes (MLWorld)

Wrapper autour de World avec des fonctionnalités supplémentaires.

**Fonctionnalités :**
- Nom custom et couleur
- Point de spawn personnalisé
- Limites de monde (Rect)
- Lock/unlock du monde
- Type de monde (WorldType enum)

**Comment l'utiliser :**

```java
MLWorld world = new MLWorld(
    bukkitWorld,
    "Nom Custom",
    NamedTextColor.RED,
    WorldType.OVERWORLD
);

world.setSpawnPoint(location);
world.setLimit(new Rect(100, -100, 255, 0, 100, -100)); // xMax, xMin, yMax, yMin, zMax, zMin
world.setLocked(true);

// Vérifications
if (world.isPlayerInWorld(player)) { ... }
if (world.isPlayerOutOfLimit(player)) { ... }

// Enregistrer
WorldRegistry.register(world);
```

**WorldRegistry :**
- `register(world)` - Enregistre un monde
- `get(UUID)` - Récupère par UUID
- `get(String)` - Récupère par nom
- `getAll()` - Récupère tous les mondes

### 3. Utilitaires (utils)

**Rect :**
Record pour définir une zone 3D rectangulaire.
- `isOut(x, y, z)` - Vérifie si un point est hors limites

**MiubyLib (classe principale) :**
- `init(plugin)` - À appeler dans onEnable()
- `runLater(Runnable, delay)` - Planifie une tâche
- `getLogger()` - Récupère le logger du plugin
- `callEvent(Event)` - Appelle un event custom

## Structure du projet

```
MiubyLib/
└── src/main/java/fr/miuby/lib/
    ├── MiubyLib.java              # Classe principale
    ├── utils/
    │   └── Rect.java              # Zone 3D rectangulaire
    ├── villager/
    │   ├── MLVillager.java        # Classe abstraite pour villageois custom
    │   ├── MLVillagerData.java    # Données de sauvegarde
    │   ├── VillagerLoadedEvent.java
    │   └── VillagerRegistry.java  # Registre global
    └── world/
        ├── MLWorld.java           # Wrapper de World
        ├── WorldRegistry.java     # Registre global
        └── WorldType.java         # Enum des types
```

## Dépendances

- Bukkit/Spigot API
- Lombok (pour @Getter, @Setter, etc.)
- Kyori Adventure (pour les TextComponents)

## Notes importantes

### MLVillager
- Le système de retry attend 10 ticks entre chaque tentative
- Maximum 10 tentatives avant de recréer le villageois
- Toujours sauvegarder après création/modification
- Les villageois sont automatiquement configurés : AI off, collidable off, silent, persistent

### MLWorld
- Les limites (Rect) sont optionnelles
- Le lock ne fait rien automatiquement, c'est à toi de le gérer
- La vérification `isPlayerOutOfLimit()` retourne false si pas de limite définie

### Rect
- Les coordonnées sont inclusives
- Format: `(xMax, xMin, yMax, yMin, zMax, zMin)`
- Exemple pour un carré de 200x200 centré sur 0,0 : `new Rect(100, -100, 255, 0, 100, -100)`
