# Restful Healing Mod - Implementation Plan

## Overview
A Hytale server mod that adds intuitive healing mechanics while resting or sleeping, with accelerated regen curves and smart auto-heal thresholds.

## Key API Discoveries & Sources

### 1. Plugin Architecture
**Base Class:** `JavaPlugin` (Source: `com.hypixel.hytale.server.core.plugin.JavaPlugin`)
- Extend this class for our mod
- Use `setup()` method for initialization
- Access `getEventRegistry()` for event handling
- Access `getTaskRegistry()` for scheduling

### 2. Player State Detection
**Movement States:** `MovementStates` class (Source: `com.hypixel.hytale.protocol.MovementStates`)
- Contains `sitting` boolean (line 36)
- Contains `sleeping` boolean (line 38)
- Accessed via `MovementStatesComponent`
- Movement state changes come through `ClientMovement` packets

### 3. Health/Stats System
**Entity Stats:** `EntityStatMap` (Source: `com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap`)
- Primary component for player health/stat tracking
- Component type: `EntityStatMap.getComponentType()`
- Access pattern: `store.getComponent(playerRef, EntityStatMap.getComponentType())`
- Health stat modifications via `processStatChanges()` method

### 4. Event System
**Available Events:** (Source: Various event classes)
- `PlayerReadyEvent` - Player joins world
- `PlayerInteractEvent` - Combat/damage interactions
- `PlayerConnectEvent` - Player connects
- Access via: `getEventRegistry().registerGlobal(EventClass, this::handlerMethod)`

### 5. Scheduling System
**Task Registry:** Available via `getTaskRegistry()`
- For periodic healing checks
- Uses server's tick system
- Can schedule recurring tasks

### 6. Configuration System
**Plugin Configs:** Available through `PluginBase`
- JSON-based configuration files
- Stored in plugin data directory
- Access via plugin's config system

## Implementation Architecture

### Core Components

#### 1. RestfulHealingPlugin (Main Class)
```java
public class RestfulHealingPlugin extends JavaPlugin {
    // Healing configuration
    private HealingConfig config;
    private Map<UUID, PlayerHealingState> healingStates;

    @Override
    protected void setup() {
        // Register events
        // Schedule healing task
        // Load config
    }
}
```

#### 2. PlayerHealingState (Data Class)
```java
public class PlayerHealingState {
    private long lastCombatTime;
    private long restStartTime;
    private boolean isHealing;
    private float healingProgress;
}
```

#### 3. HealingConfig (Configuration)
```java
public class HealingConfig {
    private float sittingHealRate;        // HP% per second
    private float sleepingHealRate;       // HP% per second
    private int combatTimeout;            // milliseconds
    private float healThreshold;          // 0.7-0.8 for 70-80%
    private int accelerationTime;         // 10-15 seconds
}
```

### Healing Logic Implementation

#### 1. State Detection System
**Monitor Movement States:**
- Register for `ClientMovement` updates
- Check `movementStates.sitting` and `movementStates.sleeping`
- Track state changes for start/stop healing

#### 2. Combat Detection System
**Monitor Combat Events:**
- Listen to `PlayerInteractEvent` for damage
- Track `lastCombatTime` per player
- Reset healing on damage

#### 3. Healing Calculation
**Implementation Steps:**
1. Check if player is out of combat (`System.currentTimeMillis() - lastCombatTime > combatTimeout`)
2. Verify player is sitting/sleeping and stationary
3. Calculate healing rate based on state and time
4. Apply accelerated regen curve after accelerationTime
5. Cap healing at healThreshold for sleeping

#### 4. Health Application
**Modify Entity Stats:**
```java
EntityStatMap stats = store.getComponent(playerRef, EntityStatMap.getComponentType());
stats.processStatChanges(EntityStatMap.Predictable.SELF,
                        statChanges,
                        ValueType.FLAT,
                        ChangeStatBehaviour.MIN);
```

## File Structure Plan

```
src/main/java/com/hypixel/hytale/mod/restfulhealing/
├── RestfulHealingPlugin.java           # Main plugin class
├── config/
│   ├── HealingConfig.java             # Configuration class
│   └── HealingConfigLoader.java       # Config loading logic
├── system/
│   ├── HealingSystem.java             # Main healing logic
│   ├── PlayerHealingState.java        # Player state tracking
│   └── HealingCalculator.java        # Healing calculations
├── listeners/
│   ├── PlayerStateListener.java       # Movement state changes
│   └── CombatListener.java           # Combat detection
└── utils/
    ├── MovementStateChecker.java      # State utilities
    └── HealthModifier.java           # Health modification utils

src/main/resources/
├── plugin.json                        # Plugin manifest
├── config.json                        # Default config
└── assets/                            # Any assets if needed
```

## Step-by-Step Implementation

### Phase 1: Basic Plugin Structure
1. Create `RestfulHealingPlugin` extending `JavaPlugin`
2. Set up basic event registration
3. Create configuration system
4. Test plugin loads/unloads properly

### Phase 2: State Detection
1. Implement player state monitoring
2. Track sitting/sleeping states
3. Implement movement detection (to cancel healing)
4. Test state detection accuracy

### Phase 3: Combat Detection
1. Implement combat event listening
2. Track last combat time per player
3. Test combat timeout system
4. Verify healing cancels on damage

### Phase 4: Healing Logic
1. Implement basic healing calculations
2. Add accelerated regen curve
3. Implement healing threshold system
4. Test healing rates and caps

### Phase 5: Integration & Polish
1. Optimize performance
2. Add comprehensive logging
3. Test with multiple players
4. Add configuration validation

## Key Technical Considerations

### Performance
- Use efficient data structures for player state tracking
- Minimize per-tick computations
- Cache component lookups where possible

### Thread Safety
- Player state map should be concurrent
- Proper synchronization for healing calculations
- Safe component access patterns

### Compatibility
- Follow Hytale plugin conventions
- Proper resource cleanup on disable
- Handle edge cases (player disconnect mid-heal)

### Configuration
- Provide sensible defaults
- Validate configuration values
- Support runtime config reloading

## Testing Strategy

1. **Unit Tests**: Test healing calculations separately
2. **Integration Tests**: Test full healing workflow
3. **Load Tests**: Test with multiple players
4. **Edge Case Tests**: Disconnects, combat edge cases, etc.

## Current Status

All five phases have been successfully completed:

- **Phase 1**: Basic Plugin Structure - Complete
- **Phase 2**: State Detection - Complete
- **Phase 3**: Combat Detection - Complete
- **Phase 4**: Healing Logic - Complete
- **Phase 5**: Performance Optimization - Complete

The mod is now fully functional and packaged as `RestfulHealing.jar` with optimized logging to prevent memory leaks. The mod is ready for deployment to a Hytale server.

This plan leverages Hytale's built-in systems while implementing the specific healing mechanics requested. The modular approach allows for incremental development and testing.

/dir add C:\Users\louize\AppData\Roaming\Hytale\install\release\package\game\latest, C:\Users\louize\source\repos\TempDecompiledAssembly\HYTALE