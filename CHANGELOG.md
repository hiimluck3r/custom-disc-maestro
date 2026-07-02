# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/), and this project adheres to
[Semantic Versioning](https://semver.org/).

## [0.2.0] — 2026-07-02

A big quality release: performance, vanilla-style art, Russian localization, an admin server config,
bundle-style sleeves, rotatable worktables and an auto-built documentation site. Worlds and items
from 0.1.x load unchanged; wear budgets of newly made discs/matrices now come from the server config
(32 / 32 / 3 by default).

### Added
- Russian localization (`ru_ru`), using authentic vinyl-production terminology (мастер-диск,
  гальваническая матрица, нарезка) and vanilla Minecraft naming (Чернит, Пластинка); note-block
  instrument names follow the Russian Minecraft Wiki.
- The Cutting Lathe's instrument names now come from the language file (localisable) instead of
  being hard-coded English strings.
- **Worktables now face the placer** (furnace-style `facing`), and the Cutting Lathe / Record Press
  show the record lying on the platter/platen while one is inside (`has_record` blockstate).
- A record tucked into a sleeve now **visibly peeks out** of the envelope (blank and designed
  sleeves alike).
- **Mod logo** (the master disc with the vanilla yellow note, composed from the actual assets) shown
  in the mod list.
- **Documentation site on GitHub Pages** (MkDocs Material, English + Russian) with an illustrated
  progression guide; every image is rendered from the mod's actual textures by the committed
  `docs/tools/render_images.py` (isometric 3D stations, the emerald bath inside the cauldron, flat
  item icons for ingredients, overlap-free captions with inventory display names) and the site is
  rebuilt automatically by the `docs.yml` workflow. The README is now a short overview linking there.
- **Server config** (`cdm-server.toml`, admin-editable, synced): `masterUses` (default **32**),
  `recordUses` (**32**) and `matrixUses` (**3**). The values are stamped onto items when they are
  made (`max_damage` component), and sound distortion always starts at 50% / 75% / 100% of the
  configured wear; smithing-table scratching moves by the same percentages.
- **Sleeves now behave like vanilla bundles**: right-click a sleeve onto a record (or a record onto
  the sleeve) in any inventory to tuck it in, right-click onto an empty slot to slide it out, and
  use a filled sleeve in hand to toss the record on the ground — with bundle sounds. The old
  one-slot sleeve GUI (menu, screen, menu type) was removed, which also closes a duplication edge
  with stacked sleeves.

### Changed
- **Textures**: the disc label is vanilla-proportioned with high-contrast stripes / ribbon / dots
  patterns; the disc family, blank and matrix picked up vanilla-style top-lit shading; the
  kupfernickel bucket now uses the vanilla bucket texture with an emerald fill overlay; the galvanic
  matrix is drawn as a record-shaped nickel negative with groove rings; a fully worn disc keeps its
  own colours and label pattern on a shattered, "music disc 11"-style model (bite + drifting shard)
  instead of a generic grey cracked texture.
- **Sleeve covers redesigned as album art**: bold two-tone motifs (colour-block bands, a diagonal
  sash, a big circle) over a printed-cover base with a spine shadow, so designed sleeves read like
  real record covers; the stencil preview got its own mini-motif textures fitted to its window.
- **Worktable blocks** got proper multi-face textures in the vanilla crafting-table layout (distinct
  top / front / side, spruce-planks underside on the packaging table): a turntable deck with a copper
  tonearm on the Cutting Lathe, a platen top and piston mouth on the Record Press, and a worktop with
  a paper sleeve, record and drawer on the Packaging Table.
- English strings: the lathe's record button says "Cut" (a lacquer master is cut, not burned), and
  the kupfernickel fluid is named "Kupfernickel" (the "Bath" is the cauldron).

### Removed
- The never-used `cdm:shrink_wrapped` data component (it was never written to any item).
- The packaging table's retired record-slot insertion path and the dead insert/extract network
  actions (the block-entity slot layout is unchanged, so old saves still load — and still drop a
  record stored there by very old versions when the table is broken).
- The old single-face worktable textures, superseded by the multi-face sets.
- Disc playback keeps a small pool of prepared melodies (instead of a single slot), so several
  jukeboxes playing different discs at once no longer re-validate and re-index their note lists every
  tick; the per-tick note lookup also uses a primitive-keyed index (no boxing).
- The kupfernickel brewing watcher checks dropped items every 4 ticks instead of every tick, removing
  most of its per-tick work for item entities lying around the world (brewing itself is unchanged).
- Reading the record tucked in a sleeve copies the stack once instead of twice (tooltips, crafting
  checks and slot validation all go through this path).
- GUI ghost hints and the press/packaging live previews reuse their item stacks across frames instead
  of allocating new ones on every render pass.
- Internal cleanup: shared sleeve/stencil model-predicate lambda, removed an unused import, an
  unused menu field and nine dead language keys.

## [0.1.1] — 2026-06-27

### Added
- **Cutting Lathe slots**: the lathe now has a Blank Disc input slot and a master-disc output slot,
  with the player inventory shown — cut records by hand instead of pulling/placing via the backpack.

### Changed
- Disc playback prepares and indexes each melody once instead of re-validating and scanning the whole
  note list every tick, lowering server cost for long discs.
- Only **master discs** can be electroformed into a galvanic matrix — a pressed (final) vinyl record no
  longer can. Master cuts are now explicitly tagged (`cdm:master` component) and labelled "Master" in
  the tooltip.
- The galvanic matrix keeps its own item name ("Galvanic Matrix") and shows the track in its tooltip.

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
