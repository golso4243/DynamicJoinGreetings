# Changelog

All notable changes to Dynamic Join Greetings are documented here.

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