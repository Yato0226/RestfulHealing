# Restful Healing Mod for Hytale

A Hytale mod that adds intuitive healing mechanics while resting or sleeping, with accelerated regen curves and smart auto-heal thresholds.

## Features

- Automatic healing while resting or sleeping
- Accelerated regeneration curves
- Smart auto-heal thresholds
- Seamless integration with Hytale's gameplay

## Building

To build the mod locally:

```bash
gradle build
```

The built JAR file will automatically be copied to:
- `C:\Users\louize\AppData\Roaming\Hytale\UserData\Mods` (for immediate testing)
- Project root directory

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

## Contributing

Feel free to submit issues and enhancement requests.

## License

This project is licensed under the MIT License - see the LICENSE file for details.