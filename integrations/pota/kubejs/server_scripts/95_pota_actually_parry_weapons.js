// Path of the Ascended: Next Gen - Actually Parry weapon classifications
// Fabric 1.20.1 / KubeJS 2001.x (server_scripts)
// PotA-only integration: Actually Parry does not depend on KubeJS.
// Add #pota:parry_weapons to config/weaponparry.json weaponWhitelist.
// Eligible weapons outside #weaponparry:two_handed use the one-handed guard.
// Tag generation only; no parry timing, damage or animation logic is changed.
ServerEvents.tags('item', event => {
  const ALLOW = 'pota:parry_weapons'
  const TWO = 'weaponparry:two_handed'

  // Vanilla: only the usable sword tiers and one-handed trident.
  event.add(ALLOW, /^minecraft:(?:iron|diamond|netherite)_sword$/)
  event.add(ALLOW, 'minecraft:trident')

  // Physical melee weapon families. Name suffixes prevent most false matches.
  const bladed = /^[a-z0-9_.-]+:[a-z0-9_]*(?:sword|longsword|greatsword|shortsword|broadsword|claymore|dagger|rapier|katana|saber|sabre|scimitar|cutlass|falchion|estoc|glaive|warglaive|halberd|spear|pike|lance|polearm|poleaxe|scythe|sickle|runeblade|swordspear|trident|twinblade|twin_blade|cleaver|machete|soul_knife|combat_knife)$/
  const heavyAxes = /^[a-z0-9_.-]+:[a-z0-9_]*(?:great_axe|greataxe|battle_axe|battleaxe|war_axe|waraxe|double_axe|doubleaxe|berserker_axe|runeaxe|poleaxe|two_handed_axe|heavy_axe)$/
  const heavyHammers = /^[a-z0-9_.-]+:[a-z0-9_]*(?:great_hammer|greathammer|war_hammer|warhammer|sledgehammer|polehammer|battle_hammer|heavy_hammer|ultra_hammer|maul)$/
  for (const pattern of [bladed, heavyAxes, heavyHammers]) event.add(ALLOW, pattern)

  // Two-handed physical weapons: horizontal guard and deeper impact sound.
  // Only specifically selected axe classes are two-handed; raid axes are exempt.
  const longWeapons = /^[a-z0-9_.-]+:[a-z0-9_]*(?:greatsword|great_sword|ultra_greatsword|claymore|glaive|warglaive|halberd|spear|pike|lance|polearm|poleaxe|scythe|swordspear|trident|double_axe|doubleaxe|berserker_axe|runeaxe|great_axe|greataxe|battle_axe|battleaxe|war_axe|waraxe|two_handed_axe|heavy_axe|great_hammer|greathammer|war_hammer|warhammer|sledgehammer|polehammer|battle_hammer|heavy_hammer|ultra_hammer|maul)$/
  const twoHandedBlades = /^[a-z0-9_.-]+:[a-z0-9_]*(?:katana|twin_blade|twinblade|soul_knife|soulknife|longsword|long_sword)$/
  event.add(TWO, longWeapons)
  event.add(ALLOW, twoHandedBlades)
  event.add(TWO, twoHandedBlades)
  // Death Knights: only great runeblades are two-handed.
  event.add(TWO, /^death_knights:great_[a-z0-9_]*runeblade$/)

  // Explicit Soulslike Weaponry selections. Keep the existing 52-item pool.
  // IDs are optional via registry regexes (no missing-tag references).
  event.add(ALLOW, /^soulsweapons:(bloodlust|bloodthirster|bluemoon_greatsword|bluemoon_shortsword|comet_spear|crucible_sword|dark_moon_greatsword|darkin_blade|darkin_scythe|darkin_scythe_pre|dawnbreaker|dragonbane|dragonslayer_swordspear|draugr|draupnir_spear|empowered_dawnbreaker|excalibur|featherlight|forlorn_scythe|freyr_sword|frostmourne|glaive_of_hodir|guts_sword|holy_greatsword|holy_moonlight_greatsword|holy_moonlight_sword|kirkhammer|leviathan_axe|lich_bane|master_sword|mehrunes_razor|moonlight_greatsword|moonlight_shortsword|moonveil|nightfall|nightlords_sword|nights_edge_item|pure_moonlight_greatsword|rageblade|shadow_assassin_scythe|silver_sword|simons_blade|skofnung|soul_reaper|sting|supernova|translucent_double_greatsword|translucent_glaive|translucent_sword|ultra_greatsword|ultra_hammer|whirligig_sawblade)$/)
  event.add(TWO, /^soulsweapons:(bluemoon_greatsword|comet_spear|dark_moon_greatsword|darkin_blade|darkin_scythe|darkin_scythe_pre|dragonslayer_swordspear|draupnir_spear|featherlight|forlorn_scythe|frostmourne|glaive_of_hodir|guts_sword|holy_greatsword|holy_moonlight_greatsword|kirkhammer|leviathan_axe|moonlight_greatsword|nightfall|pure_moonlight_greatsword|shadow_assassin_scythe|soul_reaper|supernova|translucent_double_greatsword|translucent_glaive|ultra_greatsword|ultra_hammer|whirligig_sawblade)$/)

  // Exclude ranged weapons, fists, tools, shields and dedicated spellcasters.
  // Exceptions (Paladin maces and physical battlestaffs) are added below.
  const excluded = [
    /^[a-z0-9_.-]+:[a-z0-9_]*(?:bow|crossbow|bowblade|shield|wand|staff|stave|spellbook|grimoire|mace|pickaxe|shovel|hoe|fishing_rod)$/,
    /^[a-z0-9_.-]+:[a-z0-9_]*(?:knuckle|knuckles|gauntlet|gauntlets|fist|fists)$/,
    /^soulsweapons:(?:dragon_staff|chungus_staff|withered_wabbajack|simons_bowblade|mjolnir|tonitrus)$/,
    /^minecraft:(?:wooden|stone|golden)_sword$/
  ]
  for (const pattern of excluded) {
    event.remove(ALLOW, pattern)
    event.remove(TWO, pattern)
  }

  // One-handed exceptions: diagonal guard (explicitly clear any generic 2H tag).
  const oneHanded = [
    /^paladins:[a-z0-9_]*mace$/,
    /^berserker_rpg:[a-z0-9_]*berserker_axe$/,
    /^berserker_rpg:[a-z0-9_]*raid_axe$/,
    /^minecraft:trident$/
  ]
  for (const pattern of oneHanded) {
    event.add(ALLOW, pattern)
    event.remove(TWO, pattern)
  }

  // Physical battlestaffs only; magical staffs remain excluded.
  const battlestaff = /^[a-z0-9_.-]+:[a-z0-9_]*(?:battlestaff|battle_staff)$/
  event.add(ALLOW, battlestaff)
  event.add(TWO, battlestaff)
})
