# Comprehensive YAML-Based Persistent Storage System Plan

## Current State Analysis

### Current YAML Usage in CraftBook
- **YAMLProcessor** (from `com.sk89q.util.yaml`) - Used throughout CraftBook for configuration
  - Provides comment support, header support, writeDefaults
  - Used in: ICConfiguration, VariableConfiguration, RecipeManager, CommandItems, etc.
- **Paper/Bukkit YamlConfiguration** - Available but not currently used for data storage
  - Standard Bukkit/Paper API (`org.bukkit.configuration.file.YamlConfiguration`)
  - Better performance, native to Paper
  - No comment preservation by default (but Paper may have extensions)

### Persistent Storage FIXMEs Identified

1. **WirelessTransmitter** - Currently uses `PersistentDataIC` with binary DataStream
   - Data: `Set<String> memory` (wireless band states)
   - **Recommendation**: Migrate to YAML for better readability and Paper compatibility

2. **TeleportTransmitter** - Currently uses `PersistentDataIC` with binary DataStream  
   - Data: `HistoryHashMap<String, Location> lastKnownLocations`
   - **Recommendation**: Migrate to YAML for better readability and Paper compatibility

3. **CommandItems** - Currently uses YAMLProcessor (partially implemented)
   - Data: `Map<UUID, List<ItemStack>> deathPersistItems`
   - **Recommendation**: Complete YAML implementation using Paper's YamlConfiguration

## Proposed Solution: Unified YAML Storage System

### Option 1: Use Paper's YamlConfiguration (Recommended)
**Pros:**
- Native to Paper, optimized performance
- Standard Bukkit API, well-documented
- Consistent with Paper ecosystem
- Better thread safety

**Cons:**
- No built-in comment preservation (but can use Paper's Comment API if available)
- Less flexible than YAMLProcessor for complex structures

### Option 2: Continue with YAMLProcessor
**Pros:**
- Already integrated, comment support
- Consistent with existing codebase

**Cons:**
- External dependency (WorldEdit)
- May not be as optimized as Paper's native tools

### Recommendation: Hybrid Approach

Create a **unified YAML storage utility** that:
1. Uses Paper's `YamlConfiguration` for new persistent storage implementations
2. Provides helper methods for common data types (Location, ItemStack, UUID, etc.)
3. Maintains compatibility with existing YAMLProcessor usage for configuration
4. Offers both synchronous and asynchronous save options for performance

## Implementation Plan

### Phase 1: Create Unified YAML Storage Utility

Create `org.enginehub.craftbook.util.persistence.YamlStorage` utility class:

```java
public class YamlStorage {
    // Load YAML file using Paper's YamlConfiguration
    // Save with error handling
    // Helper methods for serializing:
    //   - Location (world:name:x:y:z:yaw:pitch)
    //   - ItemStack (using ItemSyntax)
    //   - UUID
    //   - Collections (List, Set, Map)
}
```

### Phase 2: Migrate WirelessTransmitter

Replace `PersistentDataIC` binary storage with YAML:
- File: `wireless-bands.yml`
- Structure:
  ```yaml
  wireless-bands:
    - "band1"
    - "band2"
    - "band3"
  ```

### Phase 3: Migrate TeleportTransmitter

Replace `PersistentDataIC` binary storage with YAML:
- File: `teleport-locations.yml`
- Structure:
  ```yaml
  teleport-locations:
    "frequency1":
      world: "world"
      x: 100.5
      y: 64.0
      z: 200.5
      yaw: 0.0
      pitch: 0.0
    "frequency2":
      world: "world_nether"
      x: 50.0
      y: 70.0
      z: 100.0
      yaw: 90.0
      pitch: -45.0
  ```

### Phase 4: Complete CommandItems Implementation

Enhance existing YAML implementation:
- File: `command-items/death-items.yml`
- Structure:
  ```yaml
  death-items:
    "uuid1":
      - "minecraft:stone:1"
      - "minecraft:diamond_sword:1|Sharpness:5"
    "uuid2":
      - "minecraft:gold_ingot:64"
  ```

## Technical Details

### Paper YamlConfiguration Usage Pattern

```java
File file = new File(plugin.getDataFolder(), "storage.yml");
YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

// Load
String value = config.getString("path.to.value", "default");

// Save
config.set("path.to.value", value);
config.save(file);
```

### Location Serialization Helper

```java
public static void setLocation(YamlConfiguration config, String path, Location loc) {
    config.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : null);
    config.set(path + ".x", loc.getX());
    config.set(path + ".y", loc.getY());
    config.set(path + ".z", loc.getZ());
    config.set(path + ".yaw", loc.getYaw());
    config.set(path + ".pitch", loc.getPitch());
}

public static Location getLocation(YamlConfiguration config, String path) {
    String worldName = config.getString(path + ".world");
    if (worldName == null) return null;
    World world = Bukkit.getWorld(worldName);
    if (world == null) return null;
    return new Location(
        world,
        config.getDouble(path + ".x"),
        config.getDouble(path + ".y"),
        config.getDouble(path + ".z"),
        (float) config.getDouble(path + ".yaw"),
        (float) config.getDouble(path + ".pitch")
    );
}
```

### Performance Considerations

1. **Lazy Loading**: Only load data when needed
2. **Batch Saves**: Save periodically or on disable, not on every change
3. **Async Saves**: Use Bukkit scheduler for non-critical saves
4. **File Locking**: Ensure thread-safe access to YAML files

## Migration Strategy

1. Keep existing `PersistentDataIC` implementations working
2. Add new YAML-based storage alongside
3. Migrate data from old format to new format on first load
4. Remove old binary storage after migration period

## Files to Create/Modify

1. **New**: `craftbook-bukkit/src/main/java/org/enginehub/craftbook/util/persistence/YamlStorage.java`
   - Unified YAML storage utility

2. **Modify**: `WirelessTransmitter.java`
   - Replace PersistentDataIC with YAML storage

3. **Modify**: `TeleportTransmitter.java`
   - Replace PersistentDataIC with YAML storage

4. **Modify**: `CommandItems.java`
   - Enhance existing YAML implementation using Paper's YamlConfiguration

