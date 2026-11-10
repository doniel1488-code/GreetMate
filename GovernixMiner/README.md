# GovernixMiner

![GovernixMiner icon](src/main/resources/assets/governixminer/icon.png)

**Client-side Fabric mod for Minecraft 1.21.8.** GovernixMiner holds the normal attack input while your crosshair targets a block, and automatically selects the best mining tool from your hotbar.

## Features

- **Auto-mining** — holds Minecraft's normal attack input while a non-air block is targeted.
- **Auto-tool** — checks all 9 hotbar slots whenever the target block changes.
- **Suitable-tool priority** — if at least one hotbar stack is suitable for the block, the mod chooses the fastest suitable stack.
- **Fastest fallback** — if no suitable tool is available, it chooses the stack with the highest mining speed multiplier.
- **Efficiency-aware** — the tool comparison includes the Efficiency enchantment.
- **No slot flicker** — the current slot wins ties, and the selected slot changes only when a better choice is found.
- **Restore previous slot** — pressing G again or leaving the world restores the slot that was selected when auto-mining was enabled.

## How to use

1. Join a world and point your crosshair at the block you want to mine.
2. Press **G** to enable or disable auto-mining.
3. Keep the crosshair on the block or line of blocks.
4. GovernixMiner selects a suitable tool from the hotbar and holds normal attack.
5. Press **G** again to stop. The previously selected hotbar slot is restored.

## Controls

| Key | Action |
|---|---|
| `G` | Toggle auto-mining |

The key can be rebound in **Options → Controls → Key Binds → GovernixMiner**.

## Tool selection rules

1. The mod checks the **hotbar only** (slots 1–9). It does not move items from the main inventory.
2. If any hotbar stack reports itself as suitable for the targeted block, only suitable stacks are considered.
3. Among suitable stacks, the one with the highest mining speed is selected.
4. If no stack is suitable, the fastest mining-speed multiplier wins.
5. Efficiency enchantment is included in the comparison.
6. When multiple choices have effectively equal speed, the current slot is preferred to avoid unnecessary switching.

The game still decides actual mining speed, drops, tool requirements and whether the server accepts the break. This selector only chooses the stack; it does not modify block-breaking rules.

## Requirements

- Minecraft Java Edition **1.21.8**
- Fabric Loader
- Fabric API for 1.21.8
- Java 21

## Build

Run from this directory:

```bash
gradle --no-daemon clean build
```

The remapped mod JAR is written to `build/libs/`.

## Important

GovernixMiner uses Minecraft's ordinary client attack-key path. It does not send custom packets, increase reach, speed up the block-breaking timer, or bypass server validation. Mining speed, tool requirements and server-side rules remain unchanged.

Some multiplayer servers prohibit automated holding/clicking of attack. Check the server rules before using the mod there.
