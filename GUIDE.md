# 📚 NpcFant — Documentación Oficial & Wiki

Bienvenido a la Wiki oficial de **NpcFant v2.0**. Aquí encontrarás la guía completa de instalación, comandos, permisos, menús interactivos, personalización de skins, hologramas y cadenas de acciones.

---

## 📑 Tabla de Contenidos
1. [Requisitos e Instalación](#-requisitos-e-instalación)
2. [Comandos Principales (`/nfant`)](#-comandos-principales-nfant)
3. [Editor Visual e In-Game GUIs](#-editor-visual-e-in-game-guis)
4. [Sistema de Skins](#-sistema-de-skins)
5. [Sistema de Hologramas (`/holo`)](#-sistema-de-hologramas-holo)
6. [Poses y Animaciones](#-poses-y-animaciones)
7. [Cadenas de Acciones al Clic](#-cadenas-de-acciones-al-clic)
8. [Permisos](#-tabla-completa-de-permisos)
9. [Variables y Placeholders](#-variables-y-placeholders)
10. [Soporte & Comunidad](#-soporte--comunidad)

---

## ⚡ Requisitos e Instalación

### Requisitos del Servidor:
- **Servidor:** Paper, Purpur, Spigot o Leaf (Java Edition).
- **Versiones de Minecraft soportadas:** Desde **1.17 hasta 26.3** nativamente.
- **Java:** Java 17, Java 21 o Java 25 LTS.
- **Rendimiento:** 100% basado en paquetes (PacketEvents integrado). **No genera entidades físicas en el mundo**, lo que garantiza 20 TPS constantes sin consumo de pathfinding ni carga de chunks.

### Pasos de Instalación:
1. **[Descárgalo aquí en Modrinth](https://modrinth.com/plugin/npcfant)** y obtén la última versión `Npcfant-2.0.jar`.
2. Colócalo dentro de la carpeta `plugins/` de tu servidor.
3. Inicia o reinicia el servidor.
4. Se generarán automáticamente las carpetas y archivos en `plugins/NpcFant/`:
   - `config.yml` (Ajustes generales, idioma y distancias).
   - `holograms/` (Hologramas independientes y resúmenes).
   - `skins/custom/` (Carpeta para colocar tus skins `.png` locales).
   - `npcs/` (Datos persistentes de cada NPC creado).

---

## 🎮 Comandos Principales (`/nfant` / `/npcfant`)

> **Nota:** Puedes usar `/nfant` o `/npcfant` indistintamente. Para identificar un NPC puedes usar su **nombre**, su **UUID**, o la palabra **`nearest`** para seleccionar el NPC más cercano en un radio de 15 bloques.

| Comando | Descripción | Permiso |
| :--- | :--- | :--- |
| `/nfant gui` | Abre el menú visual interactivo para configurar NPCs | `npcfant.admin` |
| `/nfant create <nombre>` | Crea un nuevo NPC virtual en tu ubicación | `npcfant.create` |
| `/nfant remove <npc>` | Elimina un NPC de forma permanente | `npcfant.remove` |
| `/nfant clone <npc>` | Clona un NPC existente en tu posición | `npcfant.create` |
| `/nfant move <npc>` | Teletransporta el NPC a tu posición actual | `npcfant.admin` |
| `/nfant tp <npc>` | Te teletransporta a la posición del NPC | `npcfant.use` |
| `/nfant list` | Lista todos los NPCs con distancia y mundo | `npcfant.use` |
| `/nfant name <npc> <nombre>` | Cambia el nombre del NPC (soporta colores con `&`) | `npcfant.admin` |
| `/nfant pose <npc> <pose>` | Modifica la pose (`standing`, `sneaking`, `sitting`, etc.) | `npcfant.admin` |
| `/nfant equip <npc> <slot>` | Equipa el ítem de tu mano en el NPC (`mainhand`, `offhand`, `helmet`, `chestplate`, `leggings`, `boots`) | `npcfant.admin` |
| `/nfant look <npc>` | Activa/desactiva que el NPC siga con la mirada a los jugadores | `npcfant.admin` |
| `/nfant glow <npc> <color\|off>` | Asigna un contorno brillante o lo apaga | `npcfant.admin` |
| `/nfant invisible <npc>` | Alterna la invisibilidad del NPC | `npcfant.admin` |
| `/nfant reload` | Recarga la configuración y catálogo de skins | `npcfant.reload` |

---

## 🖥️ Editor Visual e In-Game GUIs

NpcFant te permite configurar prácticamente todo sin necesidad de recordar comandos:

1. **/nfant gui**: Abre el editor del NPC más cercano o la lista de selección si tienes múltiples NPCs.
   - **Cambiar de NPC:** Botón para alternar rápidamente entre NPCs creados.
   - **Pose del NPC:** Menú con vista de cada postura.
   - **Cambiar Nombre y Color:** Paleta de colores para el nombre o edición por chat.
   - **Cambiar Skin:** Abre el catálogo visual de skins.
   - **Comandos y Acciones:** Editor de acciones al hacer clic.
   - **Mirar al Jugador:** Interruptor con un clic.
   - **Color de Glow:** Selector visual con 8 colores de aura.
   - **Hologramas:** Administrador visual de líneas flotantes.
   - **Eliminar NPC:** Botón de seguridad para borrar el NPC.

---

## 🎨 Sistema de Skins

NpcFant incluye un motor asíncrono para aplicar skins sin congelar el servidor:

### 1. Menú Visual (`/skingui`)
- Usa `/skingui` para obtener el ítem interactivo (Ojo de Ender) o abrir el catálogo.
- Navega con flechas entre páginas y haz clic sobre cualquier skin para aplicarla al NPC seleccionado.

### 2. Formas de aplicar Skins:
- **Por nombre de usuario de Minecraft:**  
  `/nfant skin <npc> username <Jugador>` (ej. `/nfant skin Guía username Notch`).
- **Por archivo PNG local:**  
  Coloca cualquier skin en `plugins/NpcFant/skins/custom/miskin.png` y usa:  
  `/nfant skin <npc> file miskin.png`
- **Por presets predefinidos:**  
  `/nfant skin <npc> preset steve` o `alex`.

---

## 🌌 Sistema de Hologramas (`/holo`)

Los hologramas de NpcFant son 100% virtuales y se almacenan organizadamente en `plugins/NpcFant/holograms/`.

### Comandos de Hologramas:
| Comando | Descripción |
| :--- | :--- |
| `/holo gui` | Abre el gestor visual de hologramas del mapa |
| `/holo edit <nombre>` | Abre el editor interactivo de un holograma |
| `/holo create <nombre> [texto]` | Crea un holograma en tu ubicación |
| `/holo remove <nombre>` | Elimina un holograma existente |
| `/holo add <nombre> <texto>` | Añade una nueva línea al holograma |
| `/holo set <nombre> <índice> <texto>` | Modifica una línea específica |
| `/holo move <nombre>` | Mueve el holograma a tu posición |
| `/holo tp <nombre>` | Te teletransporta al holograma |
| `/holo list` | Muestra la lista de todos los hologramas |

---

## 🧘 Poses y Animaciones

Todas las poses cuentan con compensación automática de altura en el cliente para que el NPC nunca quede enterrado en el suelo:
- **`STANDING`**: De pie habitual.
- **`SNEAKING`**: Agachado.
- **`SITTING`**: Sentado (ideal para sillas, escaleras o bancos).
- **`SLEEPING`**: Acostado / durmiendo.
- **`SWIMMING`**: Animación de nado / vuelo deslizante.
- **`SPIN_ATTACK`**: Pose de tridente Riptide.

---

## ⚡ Cadenas de Acciones al Clic

Puedes encadenar múltiples acciones cuando un jugador hace clic derecho o izquierdo en un NPC:
- **`console <comando>`**: Ejecuta un comando desde la consola del servidor.
- **`player <comando>`**: Hace que el jugador ejecute el comando (ej. `spawn`, `menu`).
- **`message <texto>`**: Envía un mensaje privado al jugador (soporta colores `&`).
- **`teleport <x> <y> <z>`**: Teletransporta al jugador a las coordenadas indicadas.

---

## 📌 Variables y Placeholders

En las acciones de comando y mensajes puedes usar:
- **`{player}`** — Nombre del jugador que interactuó.
- **`{uuid}`** — UUID del jugador.
- **`{npc}`** — UUID del NPC.

---

## 🛡️ Tabla Completa de Permisos

| Permiso | Descripción | Por defecto |
| :--- | :--- | :--- |
| `npcfant.admin` | Acceso a todos los comandos y configuraciones | OP |
| `npcfant.create` | Permiso para crear y clonar NPCs | OP |
| `npcfant.remove` | Permiso para eliminar NPCs | OP |
| `npcfant.skin` | Permiso para cambiar skins de NPCs | OP |
| `npcfant.skingui` | Permiso para abrir el catálogo visual de skins | OP |
| `npcfant.hologram` | Permiso para crear y editar hologramas | OP |
| `npcfant.use` | Permiso para interactuar, ver info y listar NPCs | Todos |
| `npcfant.reload` | Permiso para recargar configuraciones | OP |

---

## 💬 Soporte & Comunidad

Si tienes preguntas, dudas o necesitas ayuda configurando NpcFant:
- 🌐 **Servidor de Discord:** [https://discord.gg/GS6hEw52BB](https://discord.gg/GS6hEw52BB)
- 📌 **Canal de soporte:** `#dudas-de-plugin` (ID: `1565188514138816622`)
- 💻 **Código Fuente & Issues:** [GitHub](https://github.com/Anonimous7800/Npc-Fant)
