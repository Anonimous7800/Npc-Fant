# NpcFant — Documentación de Comandos

> Plugin de NPCs avanzado para Minecraft Java (1.17 – 1.26.3+)  
> Autor: **anonimous1221**  
> Versión: 1.0.0  

---

## Comando Principal

```
/npcfant  |  alias: /nfant, /fnpc
```

Permiso base: `npcfant.use` (solo OPs por defecto)

---

## Subcomandos

### 📦 Control de NPCs

| Comando | Descripción | Permiso |
|---|---|---|
| `/npcfant create <tipo> <nombre>` | Crea un NPC del tipo indicado en tu posición | `npcfant.create` |
| `/npcfant remove <uuid>` | Elimina el NPC con ese UUID | `npcfant.remove` |
| `/npcfant clone <uuid>` | Clona un NPC en tu posición actual | `npcfant.create` |
| `/npcfant move <uuid>` | Mueve el NPC a donde estás parado | `npcfant.admin` |
| `/npcfant tp <uuid>` | Te teletransporta al NPC | `npcfant.use` |
| `/npcfant copy <uuid>` | Copia la configuración de un NPC | `npcfant.admin` |
| `/npcfant list` | Lista todos los NPCs activos con su UUID | `npcfant.use` |
| `/npcfant info <uuid>` | Muestra detalles de un NPC específico | `npcfant.use` |
| `/npcfant reload` | Recarga la configuración del plugin | `npcfant.reload` |

---

### 🎨 Personalización

| Comando | Descripción | Permiso |
|---|---|---|
| `/npcfant name <uuid> <nombre>` | Cambia el nombre visible del NPC (soporta `&` colores) | `npcfant.admin` |
| `/npcfant skin <uuid> username <nick>` | Aplica la skin de un jugador de Minecraft | `npcfant.skin` |
| `/npcfant skin <uuid> uuid <UUID>` | Aplica skin por UUID de jugador | `npcfant.skin` |
| `/npcfant skin <uuid> url <URL>` | Aplica skin desde una URL de textura | `npcfant.skin` |
| `/npcfant skin <uuid> file <ruta>` | Aplica skin desde un archivo local | `npcfant.skin` |

---

### 🕹️ Poses y Animaciones

| Comando | Descripción | Permiso |
|---|---|---|
| `/npcfant pose <uuid> standing` | Postura normal | `npcfant.admin` |
| `/npcfant pose <uuid> sneaking` | Postura agachada | `npcfant.admin` |
| `/npcfant pose <uuid> sitting` | Postura sentada | `npcfant.admin` |
| `/npcfant pose <uuid> sleeping` | Postura durmiendo | `npcfant.admin` |
| `/npcfant pose <uuid> swimming` | Postura nadando | `npcfant.admin` |

---

### 🛡️ Equipamiento

| Comando | Descripción | Permiso |
|---|---|---|
| `/npcfant equip <uuid> helmet <material>` | Pone un casco al NPC | `npcfant.admin` |
| `/npcfant equip <uuid> chestplate <material>` | Pone una pechera | `npcfant.admin` |
| `/npcfant equip <uuid> leggings <material>` | Pone pantalones | `npcfant.admin` |
| `/npcfant equip <uuid> boots <material>` | Pone botas | `npcfant.admin` |
| `/npcfant equip <uuid> mainhand <material>` | Item en mano principal | `npcfant.admin` |
| `/npcfant equip <uuid> offhand <material>` | Item en mano secundaria | `npcfant.admin` |

---

### 💡 Efectos Visuales

| Comando | Descripción | Permiso |
|---|---|---|
| `/npcfant glow <uuid> <COLOR>` | Activa glow de ese color (WHITE, RED, GOLD, etc.) | `npcfant.admin` |
| `/npcfant glow <uuid> off` | Desactiva el glow | `npcfant.admin` |
| `/npcfant invisible <uuid>` | Toggle de invisibilidad (cuerpo invisible, holograma visible) | `npcfant.admin` |

---

### ✨ Hologramas (Integrados)

| Comando | Descripción | Permiso |
|---|---|---|
| `/npcfant hologram <uuid> add <texto>` | Añade una línea al holograma del NPC | `npcfant.hologram` |
| `/npcfant hologram <uuid> remove` | Elimina el holograma del NPC | `npcfant.hologram` |
| `/npcfant hologram <uuid> edit <índice> <texto>` | Edita una línea existente | `npcfant.hologram` |
| `/npcfant hologram <uuid> list` | Muestra las líneas del holograma | `npcfant.hologram` |

> 💡 Los hologramas usan **TextDisplay** en Minecraft 1.19.4+ y **ArmorStand** invisible en versiones anteriores.

---

### ⚡ Interacciones y Acciones

| Comando | Descripción | Permiso |
|---|---|---|
| `/npcfant action <uuid> add console <comando>` | Ejecuta un comando como consola al interactuar | `npcfant.admin` |
| `/npcfant action <uuid> add player <comando>` | Ejecuta un comando como jugador al interactuar | `npcfant.admin` |
| `/npcfant action <uuid> add message <texto>` | Envía un mensaje al jugador al interactuar | `npcfant.admin` |
| `/npcfant action <uuid> add requireitem <material> <cantidad>` | Requiere un item en mano para interactuar | `npcfant.admin` |
| `/npcfant action <uuid> remove <índice>` | Elimina una acción de la cadena | `npcfant.admin` |
| `/npcfant action <uuid> list` | Lista todas las acciones del NPC | `npcfant.use` |

---

### 👁️ Visibilidad

| Comando | Descripción | Permiso |
|---|---|---|
| `/npcfant visible <uuid> all` | El NPC es visible para todos los jugadores | `npcfant.admin` |
| `/npcfant visible <uuid> player <jugador>` | El NPC solo es visible para un jugador específico | `npcfant.admin` |
| `/npcfant visible <uuid> hide <jugador>` | Oculta el NPC a un jugador específico | `npcfant.admin` |

---

## Placeholders en acciones

| Placeholder | Reemplazado por |
|---|---|
| `{player}` | Nombre del jugador que interactuó |
| `{uuid}` | UUID del jugador |
| `{npc}` | UUID del NPC |
| `{world}` | Mundo del jugador |

---

## Tipos de NPC disponibles (para `/npcfant create`)

```
PLAYER, VILLAGER, ZOMBIE, SKELETON, CREEPER, ENDERMAN,
WITCH, PILLAGER, IRON_GOLEM, WARDEN, BREEZE, BLAZE,
HOGLIN, STRIDER, PIGLIN, ENDER_DRAGON, WITHER, ...
```

---

## Colores para Glow

```
WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME,
PINK, GRAY, LIGHT_GRAY, CYAN, PURPLE, BLUE,
BROWN, GREEN, RED, BLACK
```

---

*Documentación generada para NpcFant v1.0.0 — © anonimous1221 — MIT License*
