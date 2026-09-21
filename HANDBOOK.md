# BKK Arsenal Handbook

This is the field reference for the clean Arsenal system. Every weapon is a named blaze rod, every custom round is a firework star, and every item has a category label in its display name. The optional resource pack supplies one 16×16 vanilla-style sprite for each item.

## Controls

Direct weapons fire on right-click in the direction the player is looking. Mortars and artillery use two-stage controls: right-click a visible block or entity to lock the target, then left-click to release the shell on the calculated arc. A compatible round in the offhand selects the payload; with no selected round, the platform uses its standard payload.

## Weapon platforms and sprites

| Platform | Role | Controls | Sprite |
| --- | --- | --- | --- |
| Launcher | Fast direct explosive launcher | Right-click | `assets/arsenal/textures/item/launcher.png` |
| Shotgun | Seven-projectile close-range spread | Right-click | `assets/arsenal/textures/item/shotgun.png` |
| Rifle | Very fast direct rifle | Right-click | `assets/arsenal/textures/item/rifle.png` |
| Sniper Rifle | Precision long-range fire | Right-click | `assets/arsenal/textures/item/sniper.png` |
| TNT Mortar | High-angle indirect fire | Right-click target, left-click fire | `assets/arsenal/textures/item/mortar.png` |
| Field Cannon | Lower-arc heavy artillery | Right-click target, left-click fire | `assets/arsenal/textures/item/field_cannon.png` |
| Howitzer | High-angle heavy artillery | Right-click target, left-click fire | `assets/arsenal/textures/item/howitzer.png` |
| Rocket Artillery | Indirect salvo launcher | Right-click target, left-click fire | `assets/arsenal/textures/item/rocket_artillery.png` |
| Anti-Air Cannon | Fast proximity/airburst fire | Right-click | `assets/arsenal/textures/item/aa_cannon.png` |

Impact scaling is platform-specific: the mortar is the baseline, the field cannon is stronger, the howitzer is the heaviest single-shot platform, rocket artillery uses lighter individual rockets in a salvo, and the anti-air cannon uses a smaller proximity blast.

## Ammunition catalogue

All paths below are 16×16 PNG sprites in `resource-pack/assets/arsenal/textures/item/`.

### Launcher

| ID | Effect | Sprite |
| --- | --- | --- |
| `he_rocket` | Standard explosive rocket | `he_rocket.png` |
| `demolition` | Stronger blast with a longer reload | `demolition.png` |
| `sticky` | Attaches to the first block, then detonates | `sticky.png` |

### Shotgun

| ID | Effect | Sprite |
| --- | --- | --- |
| `buckshot` | Seven-pellet spread | `buckshot.png` |
| `breaching` | Multiple close-range breaching pellets with penetration | `breaching.png` |
| `slug` | One concentrated explosive slug | `slug.png` |
| `incendiary_shot` | Player damage plus a small crater and fire patch | `incendiary_shot.png` |

### Rifle

| ID | Effect | Sprite |
| --- | --- | --- |
| `standard` | Standard rifle round | `standard.png` |
| `armor_piercing` | Shallow block penetration | `armor_piercing.png` |
| `tracer` | Visible tracer trail | `tracer.png` |
| `disruptor` | Temporarily disables nearby powered redstone | `disruptor.png` |

### Sniper

| ID | Effect | Sprite |
| --- | --- | --- |
| `precision` | Standard precision round | `precision.png` |
| `anti_materiel` | High damage and deeper penetration | `anti_materiel.png` |
| `marker` | Glowing target marker | `marker.png` |
| `shatter` | Main blast plus surrounding fragmentation blasts | `shatter.png` |

### Mortar

| ID | Effect | Sprite |
| --- | --- | --- |
| `he` | Standard high-explosive grenade | `he.png` |
| `penetrating` | Deep penetration followed by a stronger blast | `penetrating.png` |
| `depth_charge` | Four delayed blasts, strengths 9 through 6, progressively deeper | `depth_charge.png` |
| `airburst` | Detonates above the first terrain below the shell | `airburst.png` |
| `cluster` | Eight secondary impacts around the target | `cluster.png` |
| `incendiary_grenade` | Full entity damage, reduced terrain damage, broad fire patch | `incendiary_grenade.png` |
| `smoke` | Dense drifting three-dimensional smoke cloud | `smoke.png` |
| `illumination` | Temporary marker and shared light blocks | `illumination.png` |

### Artillery

| ID | Platforms | Effect | Sprite |
| --- | --- | --- | --- |
| `artillery_he` | Field cannon, howitzer, rocket artillery | Heavy explosive shell | `artillery_he.png` |
| `artillery_ap` | Field cannon, howitzer | Deep-penetrating shell | `artillery_ap.png` |
| `artillery_smoke` | Field cannon, howitzer, rocket artillery | Dense smoke screen | `artillery_smoke.png` |
| `artillery_illumination` | Field cannon, howitzer | Temporary illumination flare | `artillery_illumination.png` |
| `rocket_salvo` | Rocket artillery | Eight secondary rocket impacts | `rocket_salvo.png` |
| `incendiary_shell` | Field cannon, howitzer, rocket artillery | Reduced crater with a wide fire patch | `incendiary_shell.png` |
| `aa_proximity` | Anti-air cannon | Proximity or descending-terrain airburst | `aa_proximity.png` |

## Category-aware commands

```text
/arsenal ammo <player> list
/arsenal ammo <player> list <category-or-weapon>
/arsenal ammo <player> <munition> [amount]
/arsenal ammo <player> <weapon> <munition> [amount]
```

Examples:

```text
/arsenal ammo Bissbert list mortar
/arsenal ammo Bissbert list howitzer
/arsenal ammo Bissbert mortar depth_charge 4
/arsenal ammo Bissbert field_cannon artillery_ap 8
```

The weapon-qualified command validates compatibility. Hyphens and underscores are both accepted for weapon IDs.

## World and damage rules

- Projectiles use swept server-side collision; display entities are visual only.
- Shared artillery rounds inherit the impact profile of the gun that fired them; a howitzer shell is stronger than the same shell fired by a field cannon or rocket platform.
- There is no artificial travel-distance or lifetime expiry.
- Generated chunks are ticketed while a shot crosses them; terrain is never generated just to keep a shot alive.
- Incendiaries preserve their entity-damage radius, use a smaller terrain crater, and leave a larger fire patch.
- Smoke is layered in three dimensions and drifts over its lifetime.
- Illumination lights share ownership and restore the original block state when they expire.
- Player logout, death, world changes, world unloads, and clean shutdown remove owned projectiles safely.

## Graphics contract

All Arsenal item textures are exactly 16×16 pixels, RGBA PNG, with transparent backgrounds where appropriate. Models and item selectors refer only to the `arsenal` namespace, so the pack can be merged with other packs by merging selector cases.
