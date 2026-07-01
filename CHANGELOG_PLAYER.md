# Changelog Publish Summary 

This is a player-oriented summary of recent Cobalt Age changes, it is a summary of` CHANGELOG.md` ready to be published in public changelogs.

## Version 1.2.1

Added:

- Cobalt Lamp, a lamp that emits light based on received Cobalt signal strength.
- Support for Experimental Redstone Features in both modern and legacy signal engines.
- Commands to toggle the Modern Signal Engine.

Changed:

- Modern Signal Engine was heavily optimized for better RAM and CPU usage.
- Converter update delay is now 1 redstone tick.
- Redstone Lamps no longer take Cobalt Energy.

Fixed:

- Converter feedback and chain-conversion crashes.
- Cobalt Wire power loop issues over Redstone Blocks.

## Version 1.2.0

Major improvements:

- Better Cobalt Wire performance.
- Better large-network stability.
- Improved compatibility with modded blocks using standard redstone APIs.
- Reworked Cobalt Rails.
- Reworked Cobalt Relays.
- Improved Converter behavior.
- Better Cobalt Repeater and Comparator internals.

Important player-facing change:

- Cobalt Relays now focus on vertical transfer and compatible Cobalt Energy connections.

## Version 1.1.1

- Cobalt Rails gained vanilla-style behavior and no longer require the experimental minecart improvements feature.

## Version 1.1.0

Added:

- Beacon support for Cobalt Ingots and Cobalt Blocks.
- Cobalt Relay.
- Dust Armor Trim.
- Cobalt Ingot and Cobalt Dust armor trim materials.

Fixed:

- Better independence between Cobalt Dust Block and Redstone Block behavior.
- Cobalt Dust shape and update bugs.
- Converter visual and logic bugs.
- Piston quasi-connectivity issues.
- Cobalt Repeater particle issue.

## Version 1.0.0

- Initial Minecraft 1.21.11 support.
