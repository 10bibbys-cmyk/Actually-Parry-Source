# Actually Parry 1.1.3

**Initial public release — Minecraft 1.20.1 / Fabric**

Timed, server-authoritative parrying for eligible main-hand weapons, with one- and two-handed guard animations.

- Block frontal melee strikes and supported physical projectiles with a 10-tick parry window.
- Configurable 40-tick cooldown, counterattacks and failed-parry vulnerability.
- Distinct player animations for one-handed and two-handed weapons.
- Impact sparks, a restrained metallic parry sound and optional first-person feedback.
- Optional Punchy first-person poses, Better Combat and stamina integrations.
- Configure eligible weapons using explicit item IDs or item tags.
- Reduced repeated whitelist parsing and angle calculations during damage checks.

**Installation:** Fabric API and playerAnimator are required. Punchy is optional. This release was tested with Punchy 3.2a, but is not locked to that exact version; newer versions work if their APIs remain compatible.

**First launch:** `config/weaponparry.json` has an empty `weaponWhitelist` by default. Add your weapon IDs or item tags before attempting a parry. The default key is **V**.

See [Configuration](docs/CONFIGURATION.md) for details.

**Release integrity:** The JAR is verified against the developer-approved build: SHA-256 `69c96c262eb7069fe379e6f714815072f61ab0b419bf454458e1c72949388ffd`. The source is public and the build runs regression tests.

*Gameplay tested in Path of the Ascended: Next Gen; independent verification of every mod combination is not implied.*
