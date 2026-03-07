# Changelog

## [Unreleased]

### Added
- Added SP (Stamina Point) cost display to the Sword Skill Selection Screen.
- Added new server configuration options for SP management:
    - `defaultMaxSP`: Configure the base maximum SP for players.
    - `defaultSPRegen`: Configure the base SP regeneration rate per second.
- Implemented enhanced SP regeneration mechanics:
    - SP regeneration is tripled when the player is out of combat.
    - Added a "Combat Timer" system to track combat status.
- Added SP recovery on successful attacks (recovers a percentage of Max SP).

### Changed
- Updated the Sword Skill Selection Screen layout to accommodate SP cost information.
- Improved English translations in `en_us.json`:
    - Updated GUI strings for better clarity (e.g., "Select Skill" -> "Sword Skill Selection").
    - Unified weapon naming conventions (e.g., "Short Sword" -> "Dagger").
    - Fixed several typos and formatting issues in skill descriptions.
- Refactored `ServerEventHandler` to apply SP configuration values upon player login, respawn, and weapon change.

### Fixed
- Fixed an issue where the `AttackEffectEntity` would sometimes apply damage multiple times in moving effect.
- Synchronized SP consumption and recovery correctly between server and client.
