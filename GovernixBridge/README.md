# GovernixBridge

![GovernixBridge icon](src/main/resources/assets/governixbridge/icon.png)

Client-side Fabric mod for Minecraft **1.21.8**. GovernixBridge helps keep a straight bridge lane while the player walks and holds right-click. It also has an optional edge guard that pauses forward input when the next footfall has no solid support block on the client.

> The mod does not place blocks automatically, send extra interaction packets, spoof server acknowledgements, or bypass server-side anti-cheat. Normal Minecraft right-click placement remains responsible for placing blocks.

## Features

- **Lane Assist** — gently corrects sideways input toward the center line of the support block, reducing drift while bridging.
- **Right-click activation** — enabled by default; correction only runs while right-click is held and the player is moving on solid ground.
- **Edge Guard (optional)** — removes forward movement when the next footfall has no locally visible solid top face. It is off by default because some servers confirm blocks with a delay.
- **Persistent config** — settings are stored in `config/governixbridge.json`.
- **Mod icon** — included in Fabric metadata and assets.

## Controls

| Key | Action |
|---|---|
| `R` | Toggle Lane Assist |
| `V` | Toggle Edge Guard |

Both keys can be changed in **Options → Controls → Key Binds → GovernixBridge**.

## Configuration

The config is created automatically at `config/governixbridge.json`:

```json
{
  "laneAssist": true,
  "edgeGuard": false,
  "requireRightClick": true,
  "strength": 0.85
}
```

- `laneAssist`: enable lateral lane correction.
- `edgeGuard`: pause movement toward unsupported space.
- `requireRightClick`: only assist while right-click is held.
- `strength`: correction strength, from `0.25` to `1.50`.

Change the config while Minecraft is closed, then relaunch the game.

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

## Important limitations

Lane Assist changes only local movement input. It cannot make a server accept a block placement that the server rejects. Ghost blocks can still happen because of server timing, latency, placement limits, or anti-cheat. Edge Guard reads the client-side world state, so it cannot guarantee that a locally visible block has been accepted by the server.
