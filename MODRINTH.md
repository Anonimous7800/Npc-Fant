# 🌟 NpcFant — Virtual NPCs & Holograms

<div align="center">

<img src="https://raw.githubusercontent.com/Anonimous7800/Npc-Fant/main/logo/modrinth_icon.jpg" alt="NpcFant Icon" width="220" style="border-radius: 18px;" />

**The ultra-lightweight, high-performance packet-based NPC and Hologram plugin for modern Minecraft servers.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.17--26.3-00c853?style=for-the-badge&logo=minecraft)](https://modrinth.com/plugin/npc-fant)
[![Server Platforms](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Spigot-ff9800?style=for-the-badge)](https://modrinth.com/plugin/npc-fant)
[![Java](https://img.shields.io/badge/Java-17%20--%2025-blue?style=for-the-badge&logo=openjdk)](https://modrinth.com/plugin/npc-fant)
[![PacketEvents](https://img.shields.io/badge/Engine-PacketEvents%202.13-00e5ff?style=for-the-badge)](https://modrinth.com/plugin/npc-fant)
[![Discord](https://img.shields.io/badge/Discord-Soporte%20%26%20Comunidad-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/GS6hEw52BB)

</div>

---

## ⚡ What is NpcFant?

**NpcFant** is a next-generation NPC and Hologram manager built from the ground up for modern Minecraft server networks (Survival, Lobby, RPG, Minigames).

Unlike legacy NPC plugins that spawn tickable server entities that load chunks and lower TPS, **NpcFant creates 100% virtual clientbound packet entities**. The server world never has to track, tick, or pathfind them, allowing your server to host hundreds of active NPCs without dropping a single tick.

---

## ✨ Features at a Glance

### 👥 Virtual FakePlayer & Mob NPCs
- **100% Packet-Based**: Zero world entity clutter. No TPS drops, no chunk loading issues.
- **Custom Player Skins**: Apply skins from Minecraft usernames, local `.png` files, or preset models asynchronously without lag.
- **Full Pose Support**: Standing, Sneaking, Swimming, Sleeping, Sitting, and Riptide gliding with automatic height offsetting so NPCs never sink into the ground.
- **Visual Equipment**: Equip armors, weapons, shields, and items in real time.
- **Glowing Aura & Look-At**: Configurable glowing colors and head/body player-tracking.

### 📜 Interactive Menus & In-Game GUIs
- **/nfant gui**: Intuitive visual editor to change skins, poses, names, colors, equipment, and actions with zero commands needed.
- **/skingui**: Browse and preview preset skins and local skin files in an interactive inventory.
- **/holo gui**: Create and visually edit multi-line floating holograms in real time.

### 🌌 Standalone & NPC Holograms
- **Crystal-clear floating text** powered by virtual ArmorStand metadata.
- Supports RGB hex colors (`&#RRGGBB`), Minecraft color codes (`&a`, `&b`, `&l`), and per-player placeholders.
- Dedicated files saved cleanly in `plugins/NpcFant/holograms/`.

### ⚡ Click Action Chains
Bind powerful click actions to any NPC:
- 💻 **Console Commands**: Run commands with console permissions (`{player}`, `{uuid}`).
- 👤 **Player Commands**: Force player execution (e.g. `/spawn`, `/menu`).
- 💬 **Private Messages**: Send custom colored messages or sound effects.
- 🌐 **Teleportation**: Instantly teleport interactors to designated coordinates.

---

## 🛠️ Supported Minecraft Versions

Tested and natively supported on:
- **Paper / Purpur / Spigot / Leaf**: 1.17, 1.18.x, 1.19.x, 1.20.x, 1.21.x, **26.1, 26.2, 26.3**
- Compatible with Java 17, 21, and 25 LTS.

---

## 📋 Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/nfant gui` | Opens the main NPC management and selection GUI | `npcfant.admin` |
| `/nfant create <name>` | Creates a new NPC at your current location | `npcfant.admin` |
| `/nfant remove <name>` | Deletes an NPC safely | `npcfant.admin` |
| `/nfant move <name>` | Moves the NPC to your current position | `npcfant.admin` |
| `/nfant tp <name>` | Teleports you to the NPC's location | `npcfant.admin` |
| `/nfant list` | Lists all created NPCs with distance and world | `npcfant.admin` |
| `/skingui` | Opens the interactive skin catalog and manager | `npcfant.admin` |
| `/holo gui` | Opens the visual Hologram Manager GUI | `npcfant.hologram` |
| `/holo create <name> [text]` | Spawns a floating hologram at your position | `npcfant.hologram` |
| `/holo remove <name>` | Deletes a floating hologram | `npcfant.hologram` |

---

## 🚀 Quick Start Guide

1. Download **`Npcfant-2.0.jar`** from Modrinth.
2. Drop it into your server's `plugins/` directory.
3. Start or reload your server.
4. Type `/nfant create Guía` to spawn your first NPC.
5. Right-click the NPC or type `/nfant gui` to open the full visual configuration editor!

---

## 💬 Soporte & Comunidad en Discord

¿Tienes preguntas, dudas o necesitas ayuda configurando el plugin?
Únete a nuestra comunidad de Discord:
- 🌐 **Servidor de Discord:** [https://discord.gg/GS6hEw52BB](https://discord.gg/GS6hEw52BB)
- 📌 **Canal de soporte:** `#dudas-de-plugin` (ID: `1565188514138816622`)

---

<div align="center">

**Developed by anonimous1221**  
*Source code on [GitHub](https://github.com/Anonimous7800/Npc-Fant) • Community on [Discord](https://discord.gg/GS6hEw52BB)*

</div>

