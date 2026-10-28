# NpcFant — Plugin Guide

Welcome to the official **NpcFant** documentation. This guide covers installation, configuration, commands, permissions, and developer API usage.

---

## Installation

1. Download or build `Npcfant-2.0.jar`.
2. Place `Npcfant-2.0.jar` into your server's `plugins/` directory.
3. Restart your server.
4. Configuration files (`config.yml`, `skins/skins.yml`, `lang/*.yml`, and `data/npcs.yml`) will be generated automatically inside `plugins/NpcFant/`.

---

## Commands and Permissions

> **Tip**: For any `<npc>` parameter, you can use the NPC's **UUID**, its **name**, or **`nearest`** to target the closest NPC within 15 blocks.

| Command | Description | Permission |
|---|---|---|
| `/npcfant create <type> <name>` | Creates an NPC of the specified type at your location | `npcfant.create` |
| `/npcfant remove <npc>` | Permanently deletes an NPC | `npcfant.remove` |
| `/npcfant clone <npc>` | Clones an NPC at your current location | `npcfant.create` |
| `/npcfant move <npc>` | Moves the NPC to your current location | `npcfant.admin` |
| `/npcfant tp <npc>` | Teleports you to the NPC | `npcfant.use` |
| `/npcfant name <npc> <name>` | Updates the NPC's display name (`&` colors supported) | `npcfant.admin` |
| `/npcfant skin <npc> <preset\|file\|username> <value>` | Applies a skin to the NPC | `npcfant.skin` |
| `/npcfant pose <npc> <standing\|sneaking\|sitting\|sleeping\|swimming>` | Changes the NPC pose | `npcfant.admin` |
| `/npcfant equip <npc> <helmet\|chestplate\|leggings\|boots\|mainhand\|offhand\|clear>` | Equips your held item onto the NPC | `npcfant.admin` |
| `/npcfant glow <npc> <color\|off>` | Toggles glowing outline on the NPC | `npcfant.admin` |
| `/npcfant invisible <npc>` | Toggles NPC invisibility | `npcfant.admin` |
| `/npcfant look <npc>` | Toggles whether the NPC looks at nearby players | `npcfant.admin` |
| `/npcfant hologram <npc> <add\|set\|remove\|list> [text]` | Manages the NPC's floating hologram | `npcfant.hologram` |
| `/npcfant action <npc> <add\|remove\|list\|clear> [type] [value]` | Configures click actions on the NPC | `npcfant.admin` |
| `/npcfant visible <npc> <all\|only\|add\|remove> [player]` | Controls per-player visibility | `npcfant.admin` |
| `/npcfant info [npc]` | Displays detailed NPC information | `npcfant.use` |
| `/npcfant list` | Lists all active NPCs | `npcfant.use` |
| `/npcfant reload` | Reloads configuration and skin files | `npcfant.reload` |
| `/skingui [open]` | Gives the Skin Editor item or opens the Skin GUI directly | `npcfant.skingui` |

---

## Action Placeholders

When adding actions via `/npcfant action <npc> add <cmd|console|opcmd|message|sound|server> <value>`, the following placeholders are supported:

- `{player}` — Name of the player who clicked the NPC
- `{uuid}` — UUID of the player
- `{npc}` — UUID of the NPC

