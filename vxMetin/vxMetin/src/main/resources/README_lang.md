#  vxMetin Language Documentation

## Overview
This document explains all language keys used in the `vxMetin` plugin.  
Each key can be translated in `/plugins/vxMetin/lang/<locale>.yml` files such as:
- `en_US.yml`
- `tr_TR.yml`
- `de_DE.yml`
- `es_ES.yml`

---

##  Structure

| Section | Description |
|----------|--------------|
| `gui.*` | Texts, titles, and lore for in-game GUIs |
| `messages.*` | Player messages, errors, broadcasts |
| `debug.*` | Console or log debug messages |
| `hologram.*` | Lines shown above metin stones |
| `console.*` | Console messages (not shown to players) |

---

## Placeholder Reference

| Placeholder | Description |
|--------------|--------------|
| `{id}` | The stone’s **config ID** (from `stones.yml`) |
| `{uid}` | The **unique ID** of a specific spawned stone |
| `{player}` | Player’s name |
| `{stone}` | The stone’s display name |
| `{health}` | Current health of a stone |
| `{respawn}` | Remaining respawn time (seconds) |
| `{world}` | World name |
| `{x}`, `{y}`, `{z}` | Location coordinates |
| `{rank}` | Player’s rank in damage leaderboard |
| `{damage}` | Damage amount |
| `{current}`, `{max}` | Current and maximum HP |
| `{time}` | Countdown or respawn timer |
| `{offset}` | Hologram Y offset |
| `{error}` | Error message text |
| `{file}`, `{locale}`, `{count}` | File or localization information |

---

##  GUI Keys (`gui.*`)

| Key | Description |
|-----|--------------|
| `gui.title-add-stone` | Title of the Add Stone menu |
| `gui.no-stones-title` | Title displayed when no stones exist |
| `gui.no-stones-lore` | Lore displayed when no stones exist |
| `gui.stone-lore-id` | Lore showing the stone ID |
| `gui.stone-lore-health` | Lore showing stone HP |
| `gui.stone-lore-respawn` | Lore showing respawn timer |
| `gui.stone-lore-click` | “Click to select” hint |
| `gui.stone-lore-location` | Lore showing world & coordinates |
| `gui.stone-lore-location-unknown` | “Unknown location” text |
| `gui.title-admin` | Title for the Admin Menu |
| `gui.button-add` | “Add Stone” button text |
| `gui.button-remove` | “Remove Stone” button text |
| `gui.button-respawn` | “Respawn Stone” button text |
| `gui.confirm-yes` | “Confirm” button text |
| `gui.confirm-no` | “Cancel” button text |
| `gui.confirm-yes-lore` | Lore for confirm button |
| `gui.confirm-no-lore` | Lore for cancel button |
| `gui.title-stonelist`, `gui.title-stonelist-remove`, etc. | Titles for various GUI modes |
| `gui.action-remove`, `gui.action-teleport`, `gui.action-respawn` | Button actions shown as lore |
| `gui.filler-item` | Material name used for filler slots |

---

##  Player Messages (`messages.*`)

| Key | Description |
|-----|--------------|
| `messages.invalid-stone-data` | Invalid data warning |
| `messages.invalid-stone-id` | Invalid stone ID warning |
| `messages.received-stone` | Sent when a player gets a stone item |
| `messages.no-permission` | Shown when player lacks permission |
| `messages.stone-already-placed` | Same UID already placed |
| `messages.stone-already-exists` | Stone already exists at location |
| `messages.player-stone-placed` | Stone successfully placed |
| `messages.place-error` | Error while placing stone |
| `messages.stone-respawned` | Stone respawned successfully |
| `messages.stone-deleted` | Stone deleted successfully |
| `messages.stone-damage` | Player deals damage to a stone |
| `messages.top-damager` | Top damage ranking line |
| `messages.broadcast-stone-destroyed` | Global broadcast when stone is broken |
| `messages.player-stone-destroyed` | Sent to the player who broke the stone |
| `messages.broadcast-stone-spawned` | Broadcast when stone spawns |
| `messages.spawn-manager-error` | Error starting the spawn manager |
| `messages.action-cancelled` | Action was cancelled |
| `messages.delete-error` | Deletion failed |
| `messages.delete-cancelled` | Deletion cancelled |
| `messages.teleported-to-stone` | Sent when player teleports to a stone |
| `messages.lang-created` | Language file created |
| `messages.lang-loaded` | Language loaded |
| `messages.lang-reloaded` | Language reloaded |
| `messages.log-*` | Logging system messages (YAML, MySQL, SQLite) |

---

##  Hologram Keys (`hologram.*`)

| Key | Description |
|-----|--------------|
| `hologram.respawn-line-name` | Upper line shown during respawn |
| `hologram.respawn-line-timer` | Countdown line under respawn name |
| `hologram.no-damage-yet` | Text if no one damaged the stone |
| `hologram.topdamager-line` | Line showing top damage dealt |

---

##  Debug Messages (`debug.*`)

| Key | Description |
|-----|--------------|
| `debug.hologram-base-offset` | When hologram base offset is loaded |
| `debug.hologram-created` | New hologram created |
| `debug.chunk-loaded` | Chunk loaded successfully |
| `debug.chunk-load-fail` | Failed to load chunk |
| `debug.respawn-update-fail` | Hologram failed to update after respawn |
| `debug.respawn-retry` | Respawn retried |
| `debug.respawn-complete` | Respawn complete |
| `debug.hologram-removed` | Single hologram removed |
| `debug.hologram-cleared` | All holograms cleared |
| `debug.invalid-stone-id` | Invalid stone ID |
| `debug.stone-given` | Stone given to player |
| `debug.open-add-menu` | Player opened AddStone menu |
| `debug.open-confirm` | Player opened confirm menu |
| `debug.force-respawn` | Force respawn triggered |

---

##  Console Messages (`console.*`)

| Key | Description |
|-----|--------------|
| `console.plugin-starting` | Plugin starting up |
| `console.plugin-enabled` | Plugin enabled successfully |
| `console.plugin-stopping` | Plugin shutting down safely |
| `console.storage-active` | Storage type currently used |
| `console.storage-close-error` | Error closing storage |
| `console.listeners-registered` | All listeners registered |
| `console.listeners-failed` | Listener registration failed |
| `console.reload-storage-failed` | Reload failure |
| `console.hologram-reloaded` | Hologram height reloaded |
| `console.spawnstone-created` | Stone successfully spawned |
| `console.yaml-loaded`, `console.sqlite-loaded`, `console.mysql-connected` | Storage connections and status logs |

---

##  Notes
- All placeholders can be used in any language file.
- Colors must use `&` instead of `§`.
- Avoid quotes unless your string starts or ends with special YAML characters (`:`, `#`, `{}`, etc.).
- Always save files in **UTF-8 encoding**.

---

 Example of a custom translation:
```yaml
messages:
  stone-damage: "&e{player}&7 dealt &c{damage}&7 damage to &6{stone} &7(&f{current}/{max}&7)"
