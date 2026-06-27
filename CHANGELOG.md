# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/), and this project adheres to
[Semantic Versioning](https://semver.org/).

## [0.1.1] — 2026-06-27

### Added
- **Cutting Lathe slots**: the lathe now has a Blank Disc input slot and a master-disc output slot,
  with the player inventory shown — cut records by hand instead of pulling/placing via the backpack.

### Changed
- Disc playback prepares and indexes each melody once instead of re-validating and scanning the whole
  note list every tick, lowering server cost for long discs.

### Removed
- Unused configuration options (file-import / Simple Voice Chat / upload-size / playback-range) left
  over from an earlier audio-upload design; the mod is notes-only and never read them.

### Fixed
- Build no longer pins a machine-specific JDK path in the committed `gradle.properties`.

## [0.1.0] — 2026-06-27

### Added
- Initial public release for Minecraft 1.21.1 / NeoForge 21.1.228.
- **Cutting Lathe**: in-game note sequencer and Note Block Studio (`.nbs`) import; cut a melody to a
  master disc.
- **Kupfernickel Bath**: throw a copper ingot + spider eye into a water cauldron to brew the bath;
  inflicts Wither on contact.
- **Galvanic Matrix**: electroform a reusable pressing matrix from a master disc on the bath (10 presses).
- **Record Press**: stamp finished records from a matrix + blackstone, with dyeable vinyl, label and
  pattern.
- **Wear & tear**: jukebox plays wear discs; full wear distorts audio and shows a cracked texture; a worn
  master produces a matrix that stamps pre-worn records.
- **Smithing-table scratching**: age a record to the next wear stage (50 / 75 / 100).
- **Packaging**: design sleeves at the Packaging Table, right-click a sleeve to insert a record, and bake
  reusable stencils for matching artwork.
