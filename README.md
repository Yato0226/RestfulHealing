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
- **Balanced Gameplay**: Full heal still requires items or foods
- **Item Preservation**: Avoids trivializing healing items

## Installation

### Using Pre-built JAR
1. Download the latest JAR file from the [Releases](https://github.com/Yato0226/RestfulHealing/releases) page
2. Place the JAR file in your Hytale server's `plugins` directory
3. Restart the server
4. The mod will automatically appear in the mod list

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
4. The built JAR will be in the `build/libs/` directory and automatically copied to:
   - `C:\Users\louize\AppData\Roaming\Hytale\UserData\Mods` (for immediate testing)
   - Project root directory
5. Place the JAR file in your Hytale server's `plugins` directory
6. Restart the server

## Automated Releases

This project uses GitHub Actions to automatically create releases when a new tag is pushed. To create a new release:

1. Update the version in `gradle.properties`
2. Commit your changes
3. Create a new tag: `git tag v1.0.1` (replace with your version)
4. Push the tag: `git push origin v1.0.1`

GitHub Actions will automatically:
- Build the project
- Create a new release with the tag name
- Upload the JAR file as an asset
- Upload a source code archive as an asset

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
