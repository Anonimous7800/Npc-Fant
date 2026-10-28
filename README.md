<p align="center">
  <img src="logo/logo.png" alt="NpcFant Logo" width="280" />
</p>

<h1 align="center">Npc-Fant (v2.0)</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Version-2.0.0-brightgreen" alt="Version" />
  <img src="https://img.shields.io/badge/Minecraft-1.17%20--%2026.3-blue" alt="Minecraft" />
  <img src="https://img.shields.io/badge/Java-17%2B-orange" alt="Java" />
  <img src="https://img.shields.io/badge/License-MIT-purple" alt="License" />
</p>

**Npc-Fant** es un plugin avanzado de NPCs para servidores de Minecraft (Spigot, Paper, Purpur y forks) basado en paquetes (**PacketEvents**). Permite crear NPCs ligeros tipo jugador (`FakePlayer`) o de cualquier entidad, con soporte completo para skins personalizadas, menú interactivo (GUI), hologramas integrados, poses, equipamiento, visibilidad por jugador y cadenas de acciones al hacer clic.

---

## Características Principales

- **Basado en Paquetes (Zero Lag)**: Los NPCs son entidades virtuales enviadas mediante paquetes con rastreo automático de distancia (aparecen y desaparecen dinámicamente según el rango del jugador).
- **Multi-Versión (`1.17` – `26.3`)**: Funciona de forma nativa en todas las versiones modernas sin depender de reflexión pesada en el servidor.
- **Sistema Completo de Skins + GUI (`/skingui`)**:
  - Skins por nombre de jugador premium (`username`).
  - Skins predefinidas (`preset` en `skins/skins.yml`).
  - Skins desde archivos locales (`file` en `skins/custom/`).
  - Menú gráfico interactivo y ítem selector (*Editor de Skins*).
- **Múltiples Tipos de Entidades**: Soporta `PLAYER`, `VILLAGER`, `ZOMBIE`, `SKELETON`, `WARDEN`, `BREEZE`, `IRON_GOLEM`, `ENDERMAN` y más de 60 tipos de entidades.
- **Poses, Efectos y Equipamiento**:
  - Poses en tiempo real: `STANDING`, `SNEAKING`, `SITTING`, `SLEEPING`, `SWIMMING`.
  - Equipamiento completo (`helmet`, `chestplate`, `leggings`, `boots`, `mainhand`, `offhand`).
  - Efecto `Glow` e `Invisible`, y rotación automática de cabeza hacia los jugadores cercanos (`look`).
- **Hologramas Multilínea Integrados**: Añade, edita o elimina líneas flotantes sobre cualquier NPC.
- **Acciones al Interactuar (Clic Izquierdo / Derecho)**:
  - Ejecutar comandos como jugador (`cmd`), como consola (`console`) o con permisos OP temporales (`opcmd`).
  - Enviar mensajes con colores (`message`), reproducir sonidos (`sound`) o enviar a otro servidor BungeeCord/Velocity (`server`).
- **Multilenguaje**: Traducciones incluidas en 12 idiomas (`es`, `en`, `de`, `fr`, `hr`, `hu`, `it`, `nl`, `no`, `pt`, `sv`, `zh`).

---

## Comandos

> **Tip**: En `<npc>` puedes usar el **UUID**, el **nombre** del NPC o escribir **`nearest`** para seleccionar automáticamente el NPC más cercano a ti.

### Comandos Principales (`/npcfant`, `/nfant`, `/fnpc`)

| Comando | Descripción | Permiso |
|---|---|---|
| `/npcfant create <tipo> <nombre>` | Crea un NPC en tu posición actual | `npcfant.create` |
| `/npcfant remove <npc>` | Elimina un NPC permanentemente | `npcfant.remove` |
| `/npcfant clone <npc>` | Clona un NPC en tu posición | `npcfant.create` |
| `/npcfant move <npc>` | Mueve el NPC a tu ubicación actual | `npcfant.admin` |
| `/npcfant tp <npc>` | Te teletransporta a la posición del NPC | `npcfant.use` |
| `/npcfant name <npc> <nombre>` | Cambia el nombre visible (soporta colores `&`) | `npcfant.admin` |
| `/npcfant skin <npc> <preset\|file\|username> <valor>` | Cambia la skin del NPC | `npcfant.skin` |
| `/npcfant pose <npc> <standing\|sneaking\|sitting\|sleeping\|swimming>` | Cambia la postura del NPC | `npcfant.admin` |
| `/npcfant equip <npc> <helmet\|chestplate\|leggings\|boots\|mainhand\|offhand\|clear>` | Equipa el ítem que tienes en tu mano principal | `npcfant.admin` |
| `/npcfant glow <npc> <color\|off>` | Activa o desactiva el brillo del NPC | `npcfant.admin` |
| `/npcfant invisible <npc>` | Alterna la invisibilidad del NPC | `npcfant.admin` |
| `/npcfant look <npc>` | Alterna si el NPC sigue con la mirada a los jugadores | `npcfant.admin` |
| `/npcfant hologram <npc> <add\|set\|remove\|list> [args]` | Gestiona las líneas del holograma del NPC | `npcfant.hologram` |
| `/npcfant action <npc> <add\|remove\|list\|clear> [tipo] [valor]` | Gestiona las acciones al hacer clic en el NPC | `npcfant.admin` |
| `/npcfant visible <npc> <all\|only\|add\|remove> [jugador]` | Controla qué jugadores pueden ver al NPC | `npcfant.admin` |
| `/npcfant info [npc]` | Muestra la información detallada del NPC | `npcfant.use` |
| `/npcfant list` | Lista todos los NPCs registrados | `npcfant.use` |
| `/npcfant reload` | Recarga la configuración y las skins | `npcfant.reload` |

### Editor Gráfico de Skins (`/skingui`, `/sgui`, `/skinmenu`)

| Comando | Descripción | Permiso |
|---|---|---|
| `/skingui` | Te entrega el ítem **Editor de Skins** en el inventario | `npcfant.skingui` |
| `/skingui open` | Abre directamente el menú gráfico de skins | `npcfant.skingui` |

---

## Ejemplos de Uso Rápido

1. **Crear un NPC jugador con colores en el nombre:**
   ```mcfunction
   /npcfant create PLAYER &6&lGuardia Real
   ```
2. **Cambiarle la skin por la de un jugador de Minecraft:**
   ```mcfunction
   /npcfant skin nearest username Notch
   ```
3. **Equiparle el ítem que llevas en la mano:**
   ```mcfunction
   /npcfant equip nearest mainhand
   ```
4. **Añadir un holograma encima del NPC:**
   ```mcfunction
   /npcfant hologram nearest add &e¡Haz clic para ir al Lobby!
   ```
5. **Añadir acciones al hacer clic en el NPC:**
   ```mcfunction
   /npcfant action nearest add message &a¡Bienvenido al servidor, {player}!
   /npcfant action nearest add console give {player} diamond 1
   /npcfant action nearest add server survival
   ```

---

## Compilación desde el Código Fuente

Requisitos:
- **Java JDK 17** o superior
- **Apache Maven 3.8+**

Para compilar y generar el JAR listo para el servidor (con dependencias incluidas y reubicadas):

```bash
mvn clean package -pl NpcfantCore -am -DskipTests
```

El archivo compilado se generará en:
```text
NpcfantCore/target/Npcfant-2.0.jar
```

---

## Estructura del Proyecto

- **`NpcfantAPI`**: Interfaces públicas (`INPC`, `INPCManager`, `IHologram`, `INPCAction`) y eventos (`NPCInteractEvent`, `NPCSkinChangeEvent`) para desarrolladores.
- **`NpcfantCore`**: Núcleo del plugin, gestor de paquetes con PacketEvents, comandos, almacenamiento YAML, sistema de hologramas, diálogos y GUI de skins.
- **`v1_17_R1` – `v26_3_R1`**: Módulos adaptadores por versión.

---

## Licencia

Distribuido bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.
