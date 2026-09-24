# -*- coding: utf-8 -*-
"""Reads the weapon, ammunition and tuning constants straight out of the source.

Nothing in this module is hand-typed data. Every value is parsed from
`src/main/java/ch/bissbert/arsenal/ArsenalPlugin.java` or from
`src/main/resources/config.yml`, so the generated tables and charts cannot
drift away from the code.

Used by `catalogue.py`, `ballistik.py` and `packcheck.py`.
"""
import os
import re

HIER = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HIER)
JAVA = os.path.join(REPO, "src", "main", "java", "ch", "bissbert", "arsenal", "ArsenalPlugin.java")
CONFIG = os.path.join(REPO, "src", "main", "resources", "config.yml")
PACK = os.path.join(REPO, "resource-pack")


def quelle():
    with open(JAVA, encoding="utf-8") as fh:
        return fh.read()


def _enum_block(text, name):
    """Returns the body of `private enum <name> { ... }` up to its first method."""
    start = text.index("private enum %s {" % name)
    tiefe, i = 0, text.index("{", start)
    for j in range(i, len(text)):
        if text[j] == "{":
            tiefe += 1
        elif text[j] == "}":
            tiefe -= 1
            if tiefe == 0:
                return text[i + 1:j]
    raise ValueError("unbalanced braces in enum %s" % name)


# --------------------------------------------------------------------------
# WeaponType: ID("id", "Title", "arsenal:model", NamedTextColor.X)
# --------------------------------------------------------------------------
WAFFE_RE = re.compile(
    r'^\s*([A-Z_]+)\("([a-z_]+)",\s*"([^"]+)",\s*"([^"]+)",\s*NamedTextColor\.([A-Z_]+)\)',
    re.M)

# MunitionType: ID("id", "Title", "arsenal:model", WeaponType.X, NamedTextColor.Y[, WeaponType.Z...])
MUNITION_RE = re.compile(
    r'^\s*([A-Z_]+)\("([a-z_]+)",\s*"([^"]+)",\s*"([^"]+)",\s*WeaponType\.([A-Z_]+),\s*'
    r'NamedTextColor\.([A-Z_]+)((?:,\s*WeaponType\.[A-Z_]+)*)\)',
    re.M | re.S)

# weapons.put(WeaponType.X, weaponSettings("id", speed, gravity, power, cooldown, pellets, spread));
SETTINGS_RE = re.compile(
    r'weapons\.put\(WeaponType\.([A-Z_]+),\s*weaponSettings\("([a-z-]+)",\s*'
    r'([0-9.]+),\s*\.?([0-9.]+),\s*([0-9.]+),\s*([0-9]+),\s*([0-9]+),\s*([0-9.]+)\)\)')

# number("key", fallback, min, max) / (float) number(...) / (int) number(...)
NUMBER_RE = re.compile(r'number\("([a-z.\-]+)",\s*([0-9.]+),\s*([0-9.]+),\s*([0-9.]+)\)')

INDIREKT_RE = re.compile(r'return this == ([^;]+);')
SKALA_RE = re.compile(r'case ([A-Z_, ]+) -> ([0-9.]+)f;')
KATEGORIE_RE = re.compile(
    r'^\s*([A-Z_]+)\("([a-z_]+)",\s*"([^"]+)",\s*NamedTextColor\.([A-Z_]+)\)', re.M)


def _f(text):
    return float(text if not text.startswith(".") else "0" + text)


def kategorien(text=None):
    text = text or quelle()
    body = _enum_block(text, "AmmoCategory")
    return [dict(konstante=m.group(1), id=m.group(2), titel=m.group(3), farbe=m.group(4))
            for m in KATEGORIE_RE.finditer(body)]


def waffen(text=None):
    """Every WeaponType with its title, model id, default tuning and fire mode."""
    text = text or quelle()
    body = _enum_block(text, "WeaponType")
    liste = [dict(konstante=m.group(1), id=m.group(2), titel=m.group(3),
                  modell=m.group(4), farbe=m.group(5))
             for m in WAFFE_RE.finditer(body)]

    # readSettings() carries the compiled-in defaults, keyed by the enum constant.
    werte = {}
    for m in SETTINGS_RE.finditer(text):
        werte[m.group(1)] = dict(
            konfig_id=m.group(2), speed=float(m.group(3)), gravity=_f("." + m.group(4)),
            power=float(m.group(5)), cooldown=int(m.group(6)),
            projectiles=int(m.group(7)), spread=float(m.group(8)))

    # WeaponType.indirect() enumerates the target-lock platforms.
    indirekt = set()
    treffer = INDIREKT_RE.search(_enum_block(text, "WeaponType"))
    if treffer:
        indirekt = set(re.findall(r'([A-Z_]+)', treffer.group(1))) - {"this"}

    # Shot.impactScale() scales shared artillery rounds per platform.
    skala = {}
    scale_body = text[text.index("private float impactScale()"):]
    scale_body = scale_body[:scale_body.index("}")]
    for m in SKALA_RE.finditer(scale_body):
        for name in (n.strip() for n in m.group(1).split(",")):
            skala[name] = float(m.group(2))

    for w in liste:
        w.update(werte[w["konstante"]])
        w["indirekt"] = w["konstante"] in indirekt
        w["impact_scale"] = skala.get(w["konstante"], 1.0)
        # Displayed in the item lore as blocks/second.
        w["speed_bps"] = w["speed"] * 20
        w["cooldown_s"] = w["cooldown"] / 20.0
    return liste


def munitionen(text=None):
    """Every MunitionType with its primary weapon and full compatibility set."""
    text = text or quelle()
    body = _enum_block(text, "MunitionType")
    liste = []
    for m in MUNITION_RE.finditer(body):
        weitere = re.findall(r'WeaponType\.([A-Z_]+)', m.group(7) or "")
        liste.append(dict(konstante=m.group(1), id=m.group(2), titel=m.group(3),
                          modell=m.group(4), waffe=m.group(5), farbe=m.group(6),
                          kompatibel=[m.group(5)] + weitere))

    # MunitionType.standard(weapon) marks the payload used with an empty offhand.
    std_body = text[text.index("static MunitionType standard(WeaponType weapon)"):]
    std_body = std_body[:std_body.index("}; }")]
    standard = set()
    for m in re.finditer(r'->\s*([A-Z_]+);', std_body):
        standard.add(m.group(1))

    # MunitionType.category() may single out munitions before falling back to
    # AmmoCategory.forWeapon, e.g. `this == ENTITY_ROUND ? AmmoCategory.UNIVERSAL : ...`.
    cat_body = text[text.index("AmmoCategory category()"):]
    cat_body = cat_body[:cat_body.index("}")]
    ausnahmen = dict(re.findall(r'this == ([A-Z_]+) \? AmmoCategory\.([A-Z_]+)', cat_body))

    fuer_waffe = {w["konstante"]: w for w in waffen(text)}
    for mun in liste:
        mun["standard"] = mun["konstante"] in standard
        mun["kategorie"] = ausnahmen.get(mun["konstante"]) or _kategorie_fuer(fuer_waffe[mun["waffe"]]["konstante"], text)
    return liste


def _kategorie_fuer(waffe, text):
    """Mirrors AmmoCategory.forWeapon(WeaponType)."""
    body = text[text.index("static AmmoCategory forWeapon(WeaponType weapon)"):]
    body = body[:body.index("};")]
    for m in re.finditer(r'case ([A-Z_, ]+) ->\s*([A-Z_]+);', body):
        if waffe in [n.strip() for n in m.group(1).split(",")]:
            return m.group(2)
    return None


def tuning(text=None):
    """The `number(...)` defaults outside the per-weapon block: key -> (default, min, max)."""
    text = text or quelle()
    body = text[text.index("private Settings readSettings()"):text.index("private WeaponSettings weaponSettings")]
    return {m.group(1): (float(m.group(2)), float(m.group(3)), float(m.group(4)))
            for m in NUMBER_RE.finditer(body)}


def config_defaults():
    """Flattened `key.path -> value` from the shipped config.yml (no YAML dependency)."""
    werte, stapel = {}, []
    with open(CONFIG, encoding="utf-8") as fh:
        for zeile in fh:
            if not zeile.strip() or zeile.lstrip().startswith("#"):
                continue
            tiefe = (len(zeile) - len(zeile.lstrip())) // 2
            schluessel, _, rest = zeile.strip().partition(":")
            stapel = stapel[:tiefe] + [schluessel]
            rest = rest.split("#")[0].strip()
            if rest:
                werte[".".join(stapel)] = rest
    return werte


if __name__ == "__main__":
    print("%d categories, %d weapons, %d munitions, %d tuning keys, %d config keys"
          % (len(kategorien()), len(waffen()), len(munitionen()),
             len(tuning()), len(config_defaults())))
