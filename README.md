# EcorteWhitelist
A simple MariaDB whitelist plugin for PaperMC servers.

## Features
- Whitelist users by name (saved with UUIDs)
- Saved in a MariaDB database (useful for multiple servers)
- LuckPerms Contexts support
- Allow players to join even if they are not whitelisted (to use LuckPerms Contexts)
- Permissions for all subcommands

## Dependencies
- [CommandAPI](https://github.com/CommandAPI/CommandAPI)
- [LuckPerms](https://luckperms.net/)

## Installation
1. Download the latest release of [CommandAPI](https://github.com/CommandAPI/CommandAPI) from the [releases page](https://github.com/CommandAPI/CommandAPI/releases).
2. Download the latest release of [LuckPerms](https://luckperms.net/) from the [downloads page](https://luckperms.net/download).
3. Download the latest release from the [releases page](https://github.com/Ecorte/EcorteWhitelist/releases).
4. Place the downloaded files in the `plugins` folder of your PaperMC server.
5. Configure the plugin in `plugins/EcorteWhitelist/config.yml`.
6. Start your server.

## Usage
### Whitelist
Whitelist a player by name:
```
/whitelist add <player>
```

Remove a player from the whitelist:
```
/whitelist remove <player>
```

Get the whitelist status of a player:
```
/whitelist status <player>
```

### LuckPerms Contexts
This plugin add the `iswhitelisted` context to LuckPerms. This context is true if the player is whitelisted, false otherwise.