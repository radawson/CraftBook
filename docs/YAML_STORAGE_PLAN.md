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

## Implementation Details

### YamlStorage Utility Class

**Location**: `craftbook-bukkit/src/main/java/org/enginehub/craftbook/util/persistence/YamlStorage.java`

**Purpose**: Provides a unified interface for persistent data storage using Paper's `YamlConfiguration`. This utility class abstracts away the complexity of YAML file operations and provides helper methods for serializing common data types.

**Key Features**:
- Uses Paper's native `YamlConfiguration` for optimal performance
- Thread-safe file operations with error handling
- Helper methods for common data types (Location, ItemStack, UUID, Set, Map)
- Support for both synchronous and asynchronous saves
- Automatic file creation and directory structure setup

**Class Structure**:

```java
public final class YamlStorage {
    // Core file operations
    public static YamlConfiguration loadConfiguration(File file)
    public static boolean saveConfiguration(FileConfiguration config, File file)
    public static void saveConfigurationAsync(FileConfiguration config, File file)
    
    // Location serialization
    public static void setLocation(FileConfiguration config, String path, Location loc)
    public static Location getLocation(FileConfiguration config, String path)
    public static void setLocationMap(FileConfiguration config, String path, Map<String, Location> locations)
    public static Map<String, Location> getLocationMap(FileConfiguration config, String path)
    
    // ItemStack serialization (using ItemSyntax)
    public static void setItemStack(FileConfiguration config, String path, ItemStack item)
    public static ItemStack getItemStack(FileConfiguration config, String path)
    public static void setItemStackList(FileConfiguration config, String path, List<ItemStack> items)
    public static List<ItemStack> getItemStackList(FileConfiguration config, String path)
    public static void setItemStackMap(FileConfiguration config, String path, Map<UUID, List<ItemStack>> items)
    public static Map<UUID, List<ItemStack>> getItemStackMap(FileConfiguration config, String path)
    
    // String Set serialization
    public static void setStringSet(FileConfiguration config, String path, Set<String> set)
    public static Set<String> getStringSet(FileConfiguration config, String path)
    
    // Migration helper
    public static boolean migrateFromBinary(File oldFile, File newFile)
}
```

**Design Decisions**:
1. **Static utility class**: No instance needed, all methods are static for easy access
2. **Paper YamlConfiguration**: Uses native Paper API instead of YAMLProcessor for better performance
3. **ItemSyntax integration**: Leverages existing `ItemSyntax` class for ItemStack serialization
4. **Error handling**: All file operations include try-catch with logging
5. **Migration support**: Helper method to detect old binary files for migration

### WirelessTransmitter Migration

**File**: `craftbook-bukkit/src/main/java/org/enginehub/craftbook/mechanics/ic/gates/world/miscellaneous/WirelessTransmitter.java`

**Changes**:
- Removed `PersistentDataIC` interface implementation
- Removed binary `DataInputStream`/`DataOutputStream` methods
- Added `loadPersistentData()` method that:
  - Migrates from old `wireless-bands.dat` binary format if it exists
  - Loads from new `wireless-bands.yml` YAML format
- Modified `unload()` to save using `YamlStorage`
- Storage file changed from `wireless-bands.dat` to `wireless-bands.yml`

**YAML Structure**:
```yaml
wireless-bands:
  - "band1"
  - "band2"
  - "band3"
```

**Migration Logic**:
- On load, checks for old `.dat` file
- If found, reads binary format and converts to YAML
- Deletes old file after successful migration
- Falls back to YAML if migration fails

### TeleportTransmitter Migration

**File**: `craftbook-bukkit/src/main/java/org/enginehub/craftbook/mechanics/ic/gates/world/entity/TeleportTransmitter.java`

**Changes**:
- Removed `PersistentDataIC` interface implementation
- Removed binary `DataInputStream`/`DataOutputStream` methods
- Added `loadPersistentData()` method that:
  - Migrates from old `teleport-locations.dat` binary format if it exists
  - Loads from new `teleport-locations.yml` YAML format using `YamlStorage.getLocationMap()`
- Modified `unload()` to save using `YamlStorage.setLocationMap()`
- Storage file changed from `teleport-locations.dat` to `teleport-locations.yml`

**YAML Structure**:
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

**Migration Logic**:
- On load, checks for old `.dat` file
- If found, reads binary format (band name, world, coordinates, yaw, pitch)
- Converts to YAML format using `YamlStorage.setLocationMap()`
- Deletes old file after successful migration
- Validates world exists before storing location

### CommandItems Migration

**File**: `craftbook-bukkit/src/main/java/org/enginehub/craftbook/mechanics/items/CommandItems.java`

**Changes**:
- Replaced `YAMLProcessor` with Paper's `YamlConfiguration` for death items storage
- Changed `deathItemsConfig` from `YAMLProcessor` to `YamlConfiguration`
- Simplified `saveDeathItems()` to use `YamlStorage.setItemStackMap()`
- Simplified `loadDeathItems()` to use `YamlStorage.getItemStackMap()`
- Removed manual UUID parsing and ItemStack conversion loops
- Storage file remains `command-items/death-items.yml` (same path, different format)

**YAML Structure**:
```yaml
death-items:
  "uuid1":
    - "minecraft:stone:1"
    - "minecraft:diamond_sword:1|Sharpness:5"
  "uuid2":
    - "minecraft:gold_ingot:64"
```

**Benefits**:
- Cleaner code with less manual parsing
- Better error handling through YamlStorage
- Consistent with other persistent storage implementations
- Uses ItemSyntax for ItemStack serialization (maintains compatibility)

## Files Created/Modified

1. **Created**: `craftbook-bukkit/src/main/java/org/enginehub/craftbook/util/persistence/YamlStorage.java`
   - Unified YAML storage utility with Paper's YamlConfiguration
   - Helper methods for Location, ItemStack, UUID, Set, Map serialization
   - Error handling and migration support

2. **Modified**: `craftbook-bukkit/src/main/java/org/enginehub/craftbook/mechanics/ic/gates/world/miscellaneous/WirelessTransmitter.java`
   - Removed PersistentDataIC interface
   - Migrated from binary `.dat` to YAML `.yml` storage
   - Added migration logic for old format

3. **Modified**: `craftbook-bukkit/src/main/java/org/enginehub/craftbook/mechanics/ic/gates/world/entity/TeleportTransmitter.java`
   - Removed PersistentDataIC interface
   - Migrated from binary `.dat` to YAML `.yml` storage
   - Added migration logic for old format with Location serialization

4. **Modified**: `craftbook-bukkit/src/main/java/org/enginehub/craftbook/mechanics/items/CommandItems.java`
   - Replaced YAMLProcessor with Paper's YamlConfiguration for death items
   - Simplified save/load logic using YamlStorage helpers
   - Maintained backward compatibility with existing YAML file format

