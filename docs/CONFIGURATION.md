# Configuration

Actually Parry is configured independently of any modpack. The **server** decides whether an item is eligible and whether a parry succeeds.

## Weapon selection

On first launch, the mod generates `config/weaponparry.json`. **`weaponWhitelist` is empty by default**, so the mod will not let you parry until you add allowed weapons.

The following fragment shows only the whitelist field (keep the other generated fields):

```json
"weaponWhitelist": [
  "minecraft:iron_sword",
  "minecraft:diamond_sword",
  "#example:parry_weapons"
]
```

Use exact item IDs, item-tag selectors prefixed with `#`, or both. For example, `#example:parry_weapons` requires an item tag defined by a datapack or another mod. The tag is only an example and does not exist automatically.

**Main hand only.** The mod excludes bows, crossbows, shields, common tools, vanilla axes and other unsupported items from the parry whitelist even if their names are listed.

### Two-handed guard poses

Any *eligible* item in the standard item tag `weaponparry:two_handed` uses the two-handed guard. Eligible items outside the tag use the one-handed guard.

A normal datapack can define that tag in `data/weaponparry/tags/items/two_handed.json`:

```json
{
  "replace": false,
  "values": ["example:greatsword", "example:halberd"]
}
```

The above modded IDs are illustrative placeholders. Use IDs actually present in your instance. KubeJS is **not** required.

## Defaults

| Setting | Default | Meaning |
| --- | ---: | --- |
| `parryWindowTicks` | 10 | Guard is active for 0.5 seconds |
| `cooldownTicks` | 40 | Failed guard recovery is 2 seconds |
| `counterDurationTicks` | 20 | Counter opportunity lasts 1 second |
| `counterBonusDamage` | 1.0 | Added damage on a valid counterattack |
| `failedParryBonusDamage` | 1.0 | Added damage on the next eligible hit after a failed guard |
| `frontalAngleDegrees` | 120 | Frontal defensive cone |
| `sparkCount` | 8 | Base spark amount; bounded effect |
| `enableCounter` | true | Enable counter damage |
| `enableFailedParryPenalty` | true | Enable failure exposure |
| `freeBceCounterattackStamina` | true | Optional stamina integration |

The default parry key is **V** and can be changed in Minecraft controls. Press it with an eligible weapon in your main hand.

A successful parry prevents the qualifying hit, resets the cooldown and opens the counter window. A missed guard exposes the player to one additional-damage hit until the cooldown ends; the penalty is then removed.

## First-person visuals

The client creates `config/actually-parry-animations.json` with the included one- and two-handed Punchy profiles. You can manually adjust those visual poses. They do **not** change server-side combat timings.

`impactPulseDegrees` controls a brief first-person FOV impact effect. Set it to `0` to disable it.

Punchy is optional. Actually Parry does not require one exact Punchy version: the tested version is **3.2a**, and newer versions may work while their animation API remains compatible. If Punchy's API changes, the first-person integration can be updated independently; combat, damage checks and third-person player animations do not require Punchy.\n\nplayerAnimator is required, with a minimum version of **1.0.2-rc1** for Minecraft 1.20.1. Later versions are permitted by the mod metadata, but an upstream breaking API change may require an Actually Parry compatibility update.

## Multiplayer

- Install Actually Parry on the client and server.
- Keep the server weapon whitelist and item tags aligned with the weapons in your pack.
- Restart after editing config files; changes are read at initialization.
- If a parry does not activate, confirm the held item ID, the whitelist, main-hand placement and cooldown first.

## Optional PotA integration

Path of the Ascended: Next Gen provides its own KubeJS item tags for weapon classifications. Those files are **not** necessary for standalone Actually Parry installations.
