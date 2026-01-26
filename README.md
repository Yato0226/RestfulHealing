## sorry it is kinda messy lol

# Restful Healing Mod

A Hytale server mod that adds intuitive healing mechanics while resting or sleeping, with accelerated regen curves and smart auto-heal thresholds.

## Features

### Simple & Intuitive Healing
- **Passive Regen While Resting**: Recover HP% per second after being out of combat
- **Sitting**: Standard healing rate
- **Sleeping**: Faster regeneration or full heal over time
- **Standing**: No regeneration (encourages intentional rest)

### Scalable Healing Mechanics
- **Accelerated Regen Curve**:
  - First few seconds: Slow heal
  - After 10-15 seconds of uninterrupted rest: Regeneration ramps up
  - Getting hit or moving cancels the effect

### Auto-Heal to Safe Threshold
- **Smart Healing**: While sleeping, HP restores to 70-80% max, not full
- **Balanced Gameplay**: Full heal still requires items or longer rest
- **Item Preservation**: Avoids trivializing healing items

## Installation

### Using Pre-built JAR
1. Place `RestfulHealing.jar` in your Hytale server's `plugins` directory
2. Restart the server
3. The mod will automatically appear in the mod list

### Building from Source with Gradle
1. Clone the repository
2. Navigate to the project directory
3. Build the mod using Gradle:
   ```bash
   # On Windows
   gradlew.bat build

   # On macOS/Linux
   ./gradlew build
   ```
4. The built JAR will be in the `build/libs/` directory
5. Place the JAR file in your Hytale server's `plugins` directory
6. Restart the server

## Configuration

Healing rates, thresholds, and timing can be configured through the mod's config file.

## Compatibility

Designed for Hytale server API. Compatible with other mods that don't modify core healing mechanics.

## Phases Completed

- **Phase 1**: Basic Plugin Structure - Complete
- **Phase 2**: State Detection - Complete
- **Phase 3**: Combat Detection - Complete
- **Phase 4**: Healing Logic - Complete
- **Phase 5**: Performance Optimization - Complete

## License

MIT License