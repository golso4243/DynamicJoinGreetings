# Changelog

All notable changes to Dynamic Join Greetings are documented here.

## [1.0.4+26.2] - 2026-09-09

### Fixed

- Added the complete advertised default catalog of 10 first-time greetings and 50 returning-player greetings.

### Changed

- Moved the generated default configuration into a bundled JSON resource for easier maintenance.
- Added regression tests for default message counts and unique message IDs.

## [1.0.2+26.2] - 2026-09-09

### Fixed

- Fixed greetings, previews, and simulations failing to appear on servers containing Fabric Essentials Message API.
- Fixed system-chat packet encoding failures caused by incompatible Adventure library versions.

### Changed

- Replaced Adventure Platform with Placeholder API.
- Messages are now rendered as native Minecraft components.
- Message formatting now uses Placeholder API's Simplified Text Format.
- Updated the release version to identify Minecraft 26.2 compatibility.

### Compatibility

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.160.0+26.2
- Java 25

## [1.0.1] - 2026-09-09

### Changed

- Lowered the minimum Fabric Loader requirement from 0.19.5 to 0.19.3.
- Verified compatibility with Fabric Loader 0.19.3 and Fabric API 0.160.0+26.2.
- No gameplay or configuration behavior was changed.

## [1.0.0] - 2026-09-09

### Added

- Separate greetings for first-time and returning players.
- Random, no-repeat, and shuffle-bag message selection.
- Weighted messages and optional per-player selection history.
- `{player}` and `{server}` placeholders.
- MiniMessage formatting with safe placeholder insertion.
- Player, broadcast, and combined message audiences.
- Configurable greeting delay.
- Persistent player history with damaged-file recovery.
- Runtime configuration validation and reload support.
- Preview, simulation, status, and reload commands.
- Automated unit tests and GitHub Actions builds.
- Production-style server smoke-test task.
- Embedded Adventure Platform dependency.
- DJG mod icon and complete server-side metadata.

### Commands

- `/joingreetings status`
- `/joingreetings reload`
- `/joingreetings preview first`
- `/joingreetings preview returning`
- `/joingreetings simulate first`
- `/joingreetings simulate returning`