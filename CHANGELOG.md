# Changelog

## 1.1.3 — Stable

Timed weapon parrying for Minecraft Fabric 1.20.1.

- Configurable frontal parries for eligible main-hand weapons.
- Deflect qualifying melee hits and physical projectiles; magic, explosions, void and environmental damage cannot be parried.
- Separate one-handed/two-handed guard animations with optional Punchy first-person support.
- Configurable melee counterattack and missed-parry exposure.
- Server-authoritative hit validation and Better Combat compatibility.
- Refined anvil impact sound, bounded spark particles and subtle optional first-person feedback.
- Removed the development animation editor; final Punchy profiles included.
- Cached weapon whitelist selectors and frontal parry-angle cosine to reduce hot-path allocations and calculations.

**Acceptance:** Final gameplay test passed in Path of the Ascended: Next Gen. Automated parry state-machine tests and Fabric build passed on the approved source. Other mod combinations and dedicated-server configurations have not all been independently certified.
