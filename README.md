# Actually Parry

**Timed weapon parrying for Minecraft 1.20.1 / Fabric.**

Actually Parry lets players guard with eligible main-hand weapons, deflect frontal melee attacks and physical projectiles, and follow successful parries with a counterattack. Parry timing, combat bonuses, eligible weapons, effects and animation settings are configurable.

> **Release status:** 1.1.3 is undergoing final in-game acceptance testing. The public source tree and release links will be added after the approved build is final. This repository currently provides the project overview and setup documentation.

## Features

- Timing-based weapon parries: a **10-tick (0.5-second)** window and **40-tick (2-second)** cooldown by default. A successful parry resets the cooldown.
- Deflect melee strikes and physical projectiles from the **front**. Magic, explosions, void damage and environmental hazards cannot be parried.
- Failed guards expose you to a configurable damage penalty on the next qualifying hit during the remaining cooldown.
- Successful parries enable a configurable counterattack for **20 ticks (1 second)**.
- Separate one-handed and two-handed guard poses, third-person animation, metallic impact sound and bounded sparks.
- Optional Punchy first-person guard animation with built-in profiles. The mod still works without Punchy.
- Optional integrations with Better Combat and supported stamina systems.
- Server-authoritative damage and timing. No continuous entity scanning.

## Requirements

- **Minecraft 1.20.1**
- **Fabric Loader** (0.16.0 or newer)
- **Fabric API**
- **playerAnimator** by KosmX (1.0.2-rc1 or compatible 1.20.x build)

**Optional:** Punchy 3.2a (first-person animation), Better Combat, Better Combat Extension, Stamina Attributes.

Install the same mod version on server and clients when playing multiplayer.

## Getting started

1. Install the required dependencies and Actually Parry.
2. Launch Minecraft once to create `config/weaponparry.json`.
3. Add the weapons you want to parry with to `weaponWhitelist`. **The whitelist is empty by default.**
4. Hold an eligible weapon in your **main hand**, then press **V** to attempt a parry (rebindable).

Example whitelist:

```json
{
  "weaponWhitelist": [
    "minecraft:iron_sword",
    "minecraft:diamond_sword",
    "#yourpack:parry_weapons"
  ]
}
```

The example contains only the whitelist portion of the configuration; preserve the other generated settings in your existing config. Weapons listed in the item tag `weaponparry:two_handed` use the two-handed guard. Other eligible weapons use the one-handed guard.

Read the [configuration guide](docs/CONFIGURATION.md) for the full setup.

## Independent mod, originally developed for PotA

Actually Parry was developed for **Path of the Ascended: Next Gen**, but does not require the modpack, KubeJS or its custom weapon tags. Modpack authors can supply standard item tags or configure item IDs directly.

## Support and source

- Use [GitHub Issues](../../issues) for reproducible bugs; include your Minecraft version, Fabric Loader version, mod version, `latest.log` and relevant configuration.
- This public repository will receive the approved source tree after the final test sign-off.
- The mod's current license designation is **All Rights Reserved**. Public visibility of the source does not itself grant permission to reuse or redistribute it. See [LICENSE](LICENSE).

**No separate resource pack is required to use Actually Parry.**
