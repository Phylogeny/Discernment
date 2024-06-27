# Discernment Changelog
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [6.0.0] - 6-27-2024
### Changed
- Ported to MC 1.21
- The discernment enchantment, as well as the enchantment tag it falls under, are now specified by fully configurable `.json` files found in `resources/data` folders. See the online Minecraft Wiki for information on configuring them.
### Removed
- The now obsolete config for removing the discernment enchantment from enchanting tables
  - This can now be accomplished by setting the primary items to an empty list. Do this by opening `Discernment-1.21-6.0.0.jar` in the `mods` folder and adding the line `"primary_items": []` in `data/discernment/enchantment/discernment.json`.
  - Setting the primary items to an item or item tag (or to a list of items and/or item tags) that is a subset of the set of all items that the enchantment can be applied to (specified by `"supported_items"`) will result in only those primary items being available in enchanting tables. The remaining items will only be able to be enchanted via anvils.
  - The primary items must be a subset of the supported items, and refraining from specifying any primary items (as is the default) will result in all supported items being available in both enchanting tables and anvils.
  - Again, see the online Minecraft Wiki for more information.

## [5.0.0] - 1-24-2024
### Changed
- Ported to MC 1.20.4
- Moved to NeoForge
### Added
- Config to prevent the discernment enchantment from showing up in enchanting tables (Implements issue #1)
- Configurable discernment protection for players, peaceful entities, owned entities, and regex-based white/black lists for entities named with name tags

## [4.0.0] - 6-8-2022
### Changed
- Ported to MC 1.19

## [3.0.0] - 4-28-2022
### Changed
- Ported to MC 1.18.2
### Added
- Configurable particle/sound effects that spawn/play when damage is blocked by the discernment enchantment

## [2.0.1] - 12-28-2020
### Added
- Missing license info, making the mod additionally 1.16.3-4 compatible

## [2.0.0] - 7-9-2020
### Added
- Discernment effect
- Discernment potions
### Changed
- Renamed project from **Discernment Enchant** to **Discernment**

## [1.0.0] - 6-30-2020
### Added
- Initial release

---
# Info
Types of changes include: 
- `Added` for new features.
- `Changed` for changes in existing functionality.
- `Deprecated` for soon-to-be removed features.
- `Removed` for now removed features.
- `Fixed` for any bug fixes.
- `Security` in case of vulnerabilities.