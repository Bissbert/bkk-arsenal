# BKK Arsenal

A clean Paper 1.21.11 / Java 21 weapons system. It replaces the separate launcher and mortar plugins with one authoritative item, ammunition, projectile, effect, and artillery runtime.

## Weapons

| ID | Controls | Role |
| --- | --- | --- |
| `launcher` | Right-click | Fast direct launcher |
| `shotgun` | Right-click | Seven-projectile close-range spread |
| `rifle` | Right-click | Very fast direct rifle |
| `sniper` | Right-click | Long-range precision weapon |
| `mortar` | Right-click target, left-click fire | High-angle indirect fire |
| `field_cannon` | Right-click target, left-click fire | Lower arc heavy artillery |
| `howitzer` | Right-click target, left-click fire | High-angle heavy artillery |
| `rocket_artillery` | Right-click target, left-click fire | Indirect salvo launcher |
| `aa_cannon` | Right-click | Fast proximity/airburst cannon |

All weapons use the same projectile lifecycle, swept collision, chunk-ticket handling, cooldowns, ammunition selection, and permission model. Standard rounds use plain TNT as the fallback; custom ammunition is selected from the offhand.

## Ammunition

The system includes direct-fire rounds, mortar rounds, and artillery shells. Compatibility is explicit, so a round is accepted by every weapon for which it makes sense.

- Launcher: `he_rocket`, `demolition`, `sticky`
- Shotgun: `buckshot`, `breaching`, `slug`, `incendiary_shot`
- Rifle: `standard`, `armor_piercing`, `tracer`, `disruptor`
- Sniper: `precision`, `anti_materiel`, `marker`, `shatter`
- Mortar: `he`, `penetrating`, `depth_charge`, `airburst`, `cluster`, `incendiary_grenade`, `smoke`, `illumination`
- Artillery: `artillery_he`, `artillery_ap`, `artillery_smoke`, `artillery_illumination`, `rocket_salvo`, `incendiary_shell`, `aa_proximity`

Mortar and artillery shells support target-lock arcs, penetration, delayed depth charges, airbursts, cluster strikes, dense volumetric smoke, illumination lights, markers, redstone disruption, incendiary fire, and reduced-terrain incendiary craters.

## Commands

```text
/arsenal give <player> [weapon] [amount]
/arsenal ammo <player> <munition> [amount]
/arsenal cooldown [weapon] <seconds>
/arsenal reload
/arsenal status
```

Permissions:

- `arsenal.use` — use weapons (everyone by default)
- `arsenal.admin` — issue weapons/ammunition and reload settings (operators by default)
- `arsenal.infiniteammo` — bypass survival ammunition consumption (disabled by default)

There is deliberately no compatibility layer for the retired plugins. Existing items from those plugins are not recognized; issue new Arsenal items after installation.

## Projectile and world behavior

Projectiles are logical server-side objects. BlockDisplay entities are visuals only and may disappear without ending the shot. Collision is swept across every movement segment, including high-speed rounds and entities. Projectiles do not have artificial distance or lifetime expiry and may travel above the build ceiling before descending.

Only already-generated chunks are ticketed; the plugin does not generate terrain. Chunk leases are reference-counted across simultaneous shots. Player death, disconnect, world changes, world unloads, and plugin shutdown clean up owned projectiles and temporary effects.

Smoke is emitted as a layered, drifting three-dimensional particle volume. Incendiaries preserve full entity damage radius while using a small terrain crater and a larger persistent fire patch. Temporary illumination lights share ownership and restore their original blocks.

## Resource pack

The optional pack contains 32×32 vanilla-style models under the `arsenal` namespace for every weapon and ammunition type. Without it, the plugin remains fully functional using named blaze rods and firework stars. The pack has no dependency on the old plugin namespaces and is safe to merge into the existing Dungeons & Taverns pack by merging selector cases rather than replacing selector files.

Build it with:

```sh
zip -r target/BKKArsenal-resource-pack-1.0.0.zip pack.mcmeta assets
```

## Build and tests

```sh
mvn verify
```

The project targets Paper 1.21.11 and Java 21. The live server should be stopped cleanly before replacing its plugins. Install only `BKKArsenal-1.0.0.jar`; do not run the retired launcher or mortar plugins beside it.
