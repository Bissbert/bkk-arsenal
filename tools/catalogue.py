#!/usr/bin/env python3
"""Write the source-derived weapon and ammunition reference."""

from pathlib import Path
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(HERE))

import arsenal_source as source


def number(value):
    if float(value).is_integer():
        return str(int(value))
    return f"{value:g}"


def weapon_rows(weapons, munitions):
    standard = {}
    for munition in munitions:
        if munition["standard"]:
            for weapon in munition["kompatibel"]:
                standard[weapon] = munition["id"]
    rows = []
    for weapon in weapons:
        mode = "indirect" if weapon["indirekt"] else "direct"
        rows.append(
            "| `{id}` | {mode} | {speed} | {gravity} | {power} | {ticks} "
            "({seconds}s) | {projectiles} | {spread}° | `{ammo}` |".format(
                id=weapon["id"],
                mode=mode,
                speed=number(weapon["speed"]),
                gravity=number(weapon["gravity"]),
                power=number(weapon["power"]),
                ticks=weapon["cooldown"],
                seconds=number(weapon["cooldown_s"]),
                projectiles=weapon["projectiles"],
                spread=number(weapon["spread"]),
                ammo=standard[weapon["konstante"]],
            )
        )
    return rows


def ammunition_rows(munitions):
    rows = []
    for munition in munitions:
        compatible = ", ".join(
            f"`{name.lower()}`" for name in munition["kompatibel"]
        )
        standard = "yes" if munition["standard"] else "no"
        rows.append(
            "| `{id}` | {title} | {category} | {compatible} | {standard} | "
            "`{model}` |".format(
                id=munition["id"],
                title=munition["titel"],
                category=munition["kategorie"].lower(),
                compatible=compatible,
                standard=standard,
                model=munition["modell"],
            )
        )
    return rows


def category_diagram(categories, munitions):
    lines = ["```mermaid", "flowchart LR"]
    for category in categories:
        node = category["id"]
        members = sorted({
            weapon.lower()
            for munition in munitions
            if munition["kategorie"] == category["konstante"]
            for weapon in munition["kompatibel"]
        })
        category_node = f"cat_{node}"
        lines.append(f'    {category_node}["{category["titel"]} ammo"]')
        targets = " & ".join(f'{item}["{item}"]' for item in members)
        if targets:
            lines.append(f"    {category_node} --> {targets}")
    lines.append("    style cat_mortar fill:#8250df,stroke:#bc8cff,color:#fff")
    lines.append("    style cat_artillery fill:#da3633,stroke:#f85149,color:#fff")
    lines.append("```")
    return "\n".join(lines)


def main():
    categories = source.kategorien()
    weapons = source.waffen()
    munitions = source.munitionen()
    tuning = source.tuning()
    config = source.config_defaults()
    weapon_table = "\n".join(weapon_rows(weapons, munitions))
    ammunition_table = "\n".join(ammunition_rows(munitions))
    output = f"""# Weapons and ammunition

[← back to the overview](../README.md)

This reference is source-derived. Run `python3 tools/catalogue.py` after changing
the weapon enums or their defaults; the tables below are not maintained by hand.

{category_diagram(categories, munitions)}

## Weapon reference

The speed and gravity columns use the units in `config.yml`: blocks per tick and
blocks per tick squared. Cooldowns are also source values in ticks; the seconds
value is the direct conversion using the server's twenty ticks per second.

| ID | Mode | Speed | Gravity | Power | Cooldown | Projectiles | Spread | Standard ammo |
|---|---|---:|---:|---:|---:|---:|---:|---|
{weapon_table}

`projectiles` and `spread` are the configured defaults. Payloads can override
these for the slug and breaching rounds, and demolition doubles the selected
weapon's cooldown at fire time.

## Ammunition reference

Compatibility is read from each `MunitionType` declaration. `yes` means that the
round is the platform's default payload when the offhand is empty.

| ID | Display title | Category | Compatible weapons | Standard | Model token |
|---|---|---|---|---|---|
{ammunition_table}

## Other source values

The parser currently sees {len(categories)} ammo categories, {len(weapons)} weapons,
{len(munitions)} ammunition types, {len(tuning)} tunable defaults, and
{len(config)} shipped config keys. These counts are emitted by the generator and
are covered by the provenance notes in [measurement](measurement.md).

## Runtime effect notes

The table above intentionally reports identifiers and constants, not guessed
descriptions. The runtime maps those identifiers to behavior in
`ArsenalPlugin.Shot`: penetration, airburst, cluster, smoke, illumination,
incendiary, marker, redstone disruption, sticky fuse, depth charge, and shatter
branches are documented in [ballistics](ballistics.md).

[← back to the overview](../README.md)
"""
    destination = ROOT / "docs" / "items-and-ammunition.md"
    destination.parent.mkdir(exist_ok=True)
    destination.write_text(output, encoding="utf-8")
    print(
        "source counts: categories=%d weapons=%d munitions=%d "
        "tuning_keys=%d config_keys=%d"
        % (len(categories), len(weapons), len(munitions), len(tuning), len(config))
    )
    print(f"wrote {destination.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
