# Dynamic Join Greetings

Dynamic Join Greetings is a server-side Fabric mod that sends configurable messages when players join a Minecraft server.

It supports separate greetings for first-time and returning players, weighted random selection, repeat prevention, shuffle bags, configurable audiences, delayed delivery, MiniMessage formatting, and safe player/server placeholders.

## Features

- Separate first-time and returning-player message pools
- Persistent UUID-based player history
- Automatic migration of players from existing Minecraft player data
- Weighted random message selection
- No-repeat selection
- Shuffle-bag selection
- Per-player or server-wide selection history
- Configurable message delay
- Player-only, broadcast, or combined audiences
- Multi-line messages
- MiniMessage colors and formatting
- Safe `{player}` and `{server}` placeholders
- Live configuration reloads
- Preview, simulation, and status commands
- Automatic recovery from damaged player-history files
- Server-side only; clients do not need the mod

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.5 or newer
- Fabric API 0.160.0 or newer for Minecraft 26.2
- Java 25

Adventure Platform for Fabric is bundled inside the mod.

## Installation

1. Install Fabric Loader on the server.
2. Install Fabric API.
3. Place the Dynamic Join Greetings JAR in the server’s `mods` folder.
4. Start the server.
5. Edit the generated configuration:

```text
config/dynamic-join-greetings.json
```

6. Apply changes with:

```text
/joingreetings reload
```

Clients do not need to install Dynamic Join Greetings.

## Configuration

The default configuration looks similar to this:

```json
{
  "configVersion": 1,
  "enabled": true,
  "serverName": "Minecraft Server",
  "delayTicks": 40,
  "selection": {
    "mode": "SHUFFLE_BAG",
    "avoidImmediateRepeats": true,
    "rememberPerPlayer": true
  },
  "firstJoin": {
    "enabled": true,
    "audience": "PLAYER",
    "messages": [
      {
        "id": "first_welcome",
        "weight": 1.0,
        "lines": [
          "<gold><bold>Welcome to {server}, {player}!</bold></gold>",
          "<gray>Your first adventure begins here.</gray>"
        ]
      },
      {
        "id": "first_new_chapter",
        "weight": 1.0,
        "lines": [
          "<yellow>Welcome, {player}!</yellow>",
          "<gray>A new chapter awaits you on {server}.</gray>"
        ]
      }
    ]
  },
  "returningJoin": {
    "enabled": true,
    "audience": "PLAYER",
    "messages": [
      {
        "id": "return_welcome_back",
        "weight": 1.0,
        "lines": [
          "<gold>Welcome back, <yellow>{player}</yellow>!</gold>"
        ]
      },
      {
        "id": "return_adventure",
        "weight": 1.0,
        "lines": [
          "<yellow>Another adventure awaits, {player}!</yellow>"
        ]
      },
      {
        "id": "return_home",
        "weight": 1.0,
        "lines": [
          "<gold>Welcome home, {player}.</gold>",
          "<gray>It is good to see you again on {server}.</gray>"
        ]
      }
    ]
  }
}
```

### General settings

| Setting | Description |
|---|---|
| `configVersion` | Configuration format version. Currently `1`. |
| `enabled` | Enables or disables all automatic greetings. |
| `serverName` | Value used for the `{server}` placeholder. |
| `delayTicks` | Delay before delivery. Twenty ticks is approximately one second. Valid range: `0–1200`. |

### Selection modes

The `selection.mode` setting supports:

#### `RANDOM`

Selects an entry using its configured weight.

When `avoidImmediateRepeats` is enabled, the previously selected entry is excluded when another entry is available.

#### `NO_REPEAT`

Uses weighted random selection while always preventing the same message from being selected twice consecutively.

#### `SHUFFLE_BAG`

Places every message into a randomized bag and uses each one before refilling it.

Weights influence the order in which entries enter the bag. Every configured entry still appears once during each complete cycle.

### Selection settings

| Setting | Description |
|---|---|
| `mode` | `RANDOM`, `NO_REPEAT`, or `SHUFFLE_BAG`. |
| `avoidImmediateRepeats` | Prevents an immediate repeat when possible. |
| `rememberPerPlayer` | When `true`, each player has independent selection history. When `false`, the pool uses shared server-wide history. |

Selection state is held in memory and resets when the server restarts or the configuration reloads. Player first-join history remains persistent.

### Message pools

Both `firstJoin` and `returningJoin` contain:

| Setting | Description |
|---|---|
| `enabled` | Enables or disables this message pool. |
| `audience` | Determines who receives the message. |
| `messages` | List of selectable message entries. |

### Audiences

| Audience | Behavior |
|---|---|
| `PLAYER` | Sends the greeting only to the joining player. |
| `BROADCAST` | Sends it to all online players except the joining player. |
| `BOTH` | Sends it to every online player, including the joining player. |

### Message entries

Each message entry contains:

| Setting | Description |
|---|---|
| `id` | Unique identifier within its pool. |
| `weight` | Relative selection weight. Must be greater than zero. |
| `lines` | One or more MiniMessage-formatted lines. |

Example weighted pool:

```json
"messages": [
  {
    "id": "common",
    "weight": 5.0,
    "lines": [
      "<green>Welcome back, {player}!</green>"
    ]
  },
  {
    "id": "rare",
    "weight": 1.0,
    "lines": [
      "<gold><bold>A legendary return!</bold></gold>"
    ]
  }
]
```

In weighted random selection, `common` is five times as likely to be selected as `rare`.

## Placeholders

| Placeholder | Replacement |
|---|---|
| `{player}` | Joining or previewing player’s current username |
| `{server}` | Configured `serverName` |

Placeholder values are inserted as literal text components. They cannot inject MiniMessage formatting, commands, hover events, or click events.

## MiniMessage

Message lines use Adventure MiniMessage formatting.

Examples:

```text
<gold>Gold text</gold>
<red><bold>Bold red text</bold></red>
<gradient:#F6C344:#FF8C32>Gradient text</gradient>
<rainbow>Rainbow text</rainbow>
```

Formatting tags must be explicitly closed. Invalid formatting is rejected during startup or `/joingreetings reload`, and the previous valid configuration remains active.

The strict validator does not permit `<reset>`. Close active tags explicitly instead.

MiniMessage documentation:

https://docs.papermc.io/adventure/minimessage/format/

## Commands

All commands require vanilla permission level 2 (`GAMEMASTERS`) or higher.

| Command | Description |
|---|---|
| `/joingreetings status` | Displays the active configuration summary. |
| `/joingreetings reload` | Validates and reloads the configuration. |
| `/joingreetings preview first` | Immediately previews a selected first-time greeting. |
| `/joingreetings preview returning` | Immediately previews a selected returning greeting. |
| `/joingreetings simulate first` | Simulates the complete first-time workflow without changing player history. |
| `/joingreetings simulate returning` | Simulates the complete returning workflow without changing player history. |

Preview commands always send the result only to the administrator.

Simulation commands honor the configured delay and audience. Simulations use separate selection history and do not change whether the administrator is considered a first-time or returning player.

## Player history

Player UUIDs are stored separately for each world:

```text
<world>/dynamic-join-greetings/players.json
```

When the mod is first installed on an existing server, it scans:

```text
<world>/playerdata
```

Existing player UUIDs are imported as returning players. This prevents established community members from receiving first-time greetings after the mod is installed.

If `players.json` becomes malformed or uses an unsupported schema version:

1. The damaged file is renamed to a timestamped `.bak` file.
2. History is rebuilt from Minecraft’s existing player data.
3. The server continues starting normally.

Do not manually edit `players.json` while the server is running.

## Building from source

Clone the repository and run:

### Windows

```powershell
.\gradlew.bat clean build
```

### Linux or macOS

```bash
./gradlew clean build
```

Built JARs are placed in:

```text
build/libs
```

Run the automated test suite with:

```powershell
.\gradlew.bat clean test
```

## License

Dynamic Join Greetings is licensed under the MIT License.