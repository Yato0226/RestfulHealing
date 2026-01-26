# Restful Healing Mod

A Hytale server mod that adds intuitive healing mechanics while resting or sleeping.

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

1. Compile the mod into a JAR file
2. Place in your Hytale server's plugins/mods directory
3. Restart server

## Configuration

Healing rates, thresholds, and timing can be configured through the mod's config file.

## Compatibility

Designed for Hytale server API. Compatible with other mods that don't modify core healing mechanics.

## License

MIT License