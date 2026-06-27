# Custom Disc Maestro

[![Build](https://github.com/hiimluck3r/custom-disc-maestro/actions/workflows/build.yml/badge.svg)](https://github.com/hiimluck3r/custom-disc-maestro/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-blue.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.228-orange.svg)](https://neoforged.net/)

A NeoForge mod for **Minecraft 1.21.1** that turns making music discs into a small, hands-on
manufacturing pipeline. Compose a tune, cut it to a master, electroform a pressing matrix, stamp your
own vinyl, dye the artwork, and sleeve the finished record. Discs wear out as they play — and you can
deliberately distress them, too.

All audio plays through Minecraft's own **note-block sounds** — no external audio mods are required.

---

## Features

- **In-game note sequencer** to compose melodies, plus **Note Block Studio (`.nbs`) import**.
- A multi-step production chain: **master → matrix → pressed record**, each with its own station.
- **Custom dyeable records**: choose the vinyl colour, label colour and a label pattern.
- **Wear & tear**: every jukebox play wears a disc; at full wear it plays distorted and shows a cracked
  texture. A worn master produces a matrix that stamps pre-worn records.
- **Deliberate scratching** at a smithing table to age a record to a chosen stage.
- **Decorated sleeves** with a reusable **stencil** system for mass-producing matching artwork.
- Plays back **positionally in multiplayer** through the vanilla jukebox.

---

## The production pipeline

1. **Cut a master.** Craft a **Blank Disc** (iron) and record a melody onto it at the **Cutting Lathe**.
2. **Make the bath.** Throw a **copper ingot** and a **spider eye** into a water-filled cauldron to turn
   it into a **Kupfernickel Bath** (emerald-green). *Standing in the bath inflicts Wither.*
3. **Grow a matrix.** Right-click your recorded master on a cauldron full of bath to electroform a
   **Galvanic Matrix** carrying that track. The master is kept; the bath level drops a little. A matrix
   is good for 10 presses.
4. **Press the record.** In the **Record Press**, combine the matrix with **Blackstone** to stamp a
   finished record. Add optional dyes/pattern to colour the vinyl, label and pattern.
5. **Play it.** Drop the record in any **jukebox**. Each play wears the groove a little.
6. **Sleeve it.** Design a sleeve at the **Packaging Table**, then right-click the sleeve in hand to drop
   a record inside.

Optional: **scratch** a record at a **smithing table** (flint + record + flint) to age it to the next
wear stage (50 → 75 → 100).

---

## Crafting recipes

| Item | Pattern | Ingredients |
|------|---------|-------------|
| **Blank Disc** (master) | `·I·` / `I·I` / `·I·` | `I` = Iron Ingot |
| **Cutting Lathe** | `ICI` / `CNC` / `IRI` | `I` Iron, `C` Copper, `N` Note Block, `R` Redstone |
| **Record Press** | `III` / `IPI` / `CCC` | `I` Iron, `P` Piston, `C` Copper |
| **Packaging Table** | `PPP` / `PSP` / `PPP` | `P` Planks, `S` String |
| **Blank Sleeve** | `PPP` / `P·P` / `PPP` | `P` Paper |

Pattern templates can be duplicated by combining a template with a **diamond**. The **Kupfernickel Bath**,
**Galvanic Matrix**, **pressed record** and **scratching** are made through their stations/mechanics
described above rather than a crafting grid.

---

## Installation

1. Install [NeoForge **21.1.228**](https://neoforged.net/) for Minecraft **1.21.1**.
2. Drop the mod JAR into your `mods/` folder.
3. Launch the game.

Download JARs from the [Releases](https://github.com/hiimluck3r/custom-disc-maestro/releases) page, or
grab a development build from the [Actions](https://github.com/hiimluck3r/custom-disc-maestro/actions)
artifacts.

---

## Building from source

Requires a full **JDK 21** (a JRE is not enough — NeoForm recompiles Minecraft).

```bash
git clone https://github.com/hiimluck3r/custom-disc-maestro.git
cd custom-disc-maestro
./gradlew build          # output: build/libs/cdm-<version>.jar
./gradlew runClient      # launch the dev client
./gradlew runData        # regenerate data/asset JSONs (src/generated)
```

> The committed `gradle.properties` pins `org.gradle.java.home` to a local JDK path. If your default
> Java is already a full JDK 21, you can remove that line (CI strips it automatically).

---

## Versioning

Releases follow `MAJOR.MINOR.PATCH` and are tagged `vX.Y.Z`. Each Minecraft version is maintained on its
own branch (e.g. `1.21.1`); `master` tracks the latest.

---

## License

Released under the [MIT License](LICENSE) — free to use, modify, fork and redistribute, including in
modpacks. Attribution is appreciated but not required.
