# Actually Parry 1.1.3 — Release provenance

**Approved artifact:** `actually-parry-1.1.3.jar`

**SHA-256:** `69c96c262eb7069fe379e6f714815072f61ab0b419bf454458e1c72949388ffd`

Tested source revision: `430adb0d3fd299ec4df2f7d5fd46fd07cd4f7fd5` (internal development repo). The corresponding GitHub Actions build [44166479022](https://github.com/10bibbys-cmyk/Actually-Parry/actions/runs/44166479022) passed.

The approved JAR passed final gameplay testing in Path of the Ascended: Next Gen. This public repository contains the corresponding Java source and resources. Its simplified workflow builds the same version and tests the parry state machine. A new build need not be byte-for-byte identical due to archive timestamps and build metadata.

**Requirements:** Minecraft 1.20.1, Fabric Loader, Fabric API and playerAnimator (minimum 1.0.2-rc1). Later playerAnimator builds are allowed but depend on API compatibility. Punchy is optional, has **no exact-version lock**, and was tested with 3.2a. If either library introduces breaking API changes, a future compatibility patch may be necessary.

The base mod does **not** need an external resource pack. PotA's HyperPunchy equip mappings are independent and are not included in the standalone mod.
