# Changelog

All notable changes to this project will be documented in this file.

This file is formatted as per [Keep a Changelog](https://keepachangelog.com/en/1.0.0),
and Cobalt Age's versioning is based on [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Removed

### Fixed


## [1.2.1] - WORKING ON
//TODO: FIX relay depower and power updates on legacy engine

### Added
- **NEW BLOCK → Cobalt Lamp:** Cobalt lamp will emit the same signal as it receives from cobalt input.
- **Cobalt Wires:** Added support to Experimental Redstone Features for both Modern and Legacy signal engines.
- **Signal Engines:** Possibility to switch signal engine algorithms: optimized engine (aka Modern Signal Engine) and the neighbor updater one (aka Legacy / Vanilla). Use `/cobaltage on` or `/cobaltage off` command to toggle.
### Changed
- **Modern Signal Engine** has been revisited. It is now dynamic and uses bit masks all over `ServerLevel.class`, making it RAM and CPU friendly. *This has increased dramatically the performance*.
- **Converter** now inherits an `UPDATE_DELAY`, which has been set to 1 redstone tick (2 real ticks).
- **Redstone Lamp** will not take cobalt energy anymore.
### Removed
- **Internal Garbage:** Some old textures that were no longer used have been removed.

### Fixed
- **Converter:** Fixed update loop crash when chain conversion happens. Fixed a bug when the converter couldn't take the best external energy to set its flow direction priority
- **Cobalt Wires:** Fixed power loop when activated over a Redstone Block

## [1.2.0] - 2026-05-26

### Added
- **Core Architecture & Performance:** Introduced local chunk data caching to reduce repeated chunk lookups during updates.
- **Converter:** Introduced the new `FLOWING_SIDE` property to track the direction of energy flow.

### Changed
- **Core Architecture & Performance:** Mixins are now restricted to Redstone-type blocks to improve compatibility with modded blocks.
- **Core Architecture & Performance:** Signal transmission data is now stored at the `Level`/`BlockState` level instead of per-class, reducing memory usage.
- **Core Architecture & Performance:** Overall performance has been significantly improved, especially for large wire networks, resulting in less overhead and fewer lag spikes.
- **Cobalt Wires:** Completely redesigned architecture for improved performance.
- **Cobalt Wires:** Improved update handling and consumer behavior.
- **Cobalt Wires:** Expanded compatibility with modded blocks that rely on standard redstone APIs.
- **Cobalt Rails:** Fully reworked structure and updated texture.
- **Cobalt Rails:** Reclassified as a **Cobalt Energy Block (CEB)**; now can only receive energy from other Cobalt Energy Blocks.
- **Cobalt Relays:** Reworked architecture (inherits the same stability fixes as Cobalt Wires).
- **Cobalt Relays:** Limited energy transfer to vertical only (up and down).
- **Cobalt Relays:** Now only connect and transfer energy with Cobalt Wires and compatible Cobalt Energy Blocks.
- **Converter:** Received a major overhaul.
- **Cobalt Repeaters & Comparators:** Now share a more unified implementation.
- **Cobalt Repeaters & Comparators:** Use the same optimized signal-processing system as Cobalt Wires for improved efficiency.
- **Cobalt Dust Block:** Updated texture to better match the appearance of powered Cobalt Wires.

### Removed
- **Cobalt Relays:** Removed interaction behavior with torches or blocks attached to their sides.
- **Converter:** Removed activation sound when waterlogged.

### Fixed
- **Core Architecture & Performance:** Signal channels are now fully independent, eliminating cross-channel interference.
- **Core Architecture & Performance:** Fixed issues preventing Cobalt Energy Blocks from properly powering modded redstone blocks in certain situations.
- **Cobalt Wires:** Fixed multiple visual issues that occurred when wires were placed on non-sturdy block faces.
- **Converter:** Fixed the energy-loss bug that occurred during energy-type conversion.
- **Converter:** Resolved all feedback-loop issues.
- **Converter:** Fixed update and signal transmission bugs that could temporarily power adjacent blocks.
- **Cobalt Torches:** Applied the same bug fixes and stability improvements as those introduced for Cobalt Wires.

## [1.1.1] - 2026-05-26

### Changed
Added vanilla behaviour for cobalt rails, now is not anymore necessary to have the experimental feature `minecart improvements` on. You can either choose to
use it while having experimental features on (suggested) or not.

## [1.1.0] - 2026-05-17

### Added
- **Beacon & GUI Support:** Cobalt ingots can now be used as payment for beacons, and Cobalt Blocks can be used to power them. An optional custom resource pack has been added for those using dark mode.
- **New Block (Cobalt Relay):** An extension of Cobalt Wires that powers upwards and downwards, allowing for vertical circuits. It connects only to Cobalt Wires (allowing parallel vertical circuits), acts as a Transparent Block, and is fully waterloggable.
- **New Armor Trim (Dust Trim):** A new armor trim template featuring shapes reminiscent of redstone/cobalt dust. Currently obtainable via Wandering Traders.
- **New Armor Materials:** Added Cobalt Ingot and Cobalt Dust to the armor trim material list, featuring unique color palettes.

### Changed
- **Blocks (Raw Cobalt & Cobalt):** Textures for the Block of Raw Cobalt and Block of Cobalt have been changed and revamped.
- **Cobalt Items:** Textures for the Cobalt Ingot and Cobalt Nugget have been revamped and saturated.

### Fixed
- **Cobalt Dust Block & Redstone Dust Block:** Changed behavior to ensure complete independence. The Cobalt Dust Block is strictly for Cobalt Energy Blocks (CEBs), while the Redstone Dust Block is strictly for Redstone Energy Blocks (REBs).
- **Cobalt Dust:** Resolved bugs with glass diodes and general updates. Added compatibility for special blocks (non-connectivity). Fixed right-click behavior where the shape could be toggled to a dot when forced by 4 connected wires.
- **Converter:** Resolved a bug where the circuit would get stuck when changing flow directions. Updated particles and corrected visual linking so that Redstone Dust and Cobalt Dust only link to their respective types.
- **Pistons:** Resolved an issue where Quasi-Connectivity was not being implemented correctly.
- **Cobalt Repeater:** Fixed missing particles in the 4-tick state.
## [1.0.1] - 2026-04-27

### Fixed
Various bugs related to Cobalt Age's network wire handlers.
Before you were able to experience a massive lag if you were using lag machine with powered cables
that could change their power state frequently in positive values only (1-15, without the 0 off state). 

## [1.0.0] - 2026-04-27
Initial support for Minecraft 1.21.11, which is the only one in testing.
Feedback and bug reports are greatly appreciated!
**[Bug tracker](https://github.com/FernandoDU14/Cobalt-Age/issues).**

[Unreleased]: https://github.com/FernandoDU14/Cobalt-Age/compare/Fabric-1.21.11-1.2.1...HEAD
[1.2.1]: https://github.com/FernandoDU14/Cobalt-Age/tree/Fabric-1.21.11-1.2.1
[1.2.0]: https://github.com/FernandoDU14/Cobalt-Age/tree/Fabric-1.21.11-1.2.0
[1.1.1]: https://github.com/FernandoDU14/Cobalt-Age/tree/Fabric-1.21.11-1.1.1
[1.1.0]: https://github.com/FernandoDU14/Cobalt-Age/tree/Fabric-1.21.11-1.1.0
[1.0.1]: https://github.com/FernandoDU14/Cobalt-Age/tree/Fabric-1.21.11-1.0.1
[1.0.0]: https://github.com/FernandoDU14/Cobalt-Age/tree/Fabric-1.21.11-1.0.0