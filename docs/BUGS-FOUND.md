# Bugs found during the documentation pass

[← back to the overview](../README.md)

One issue was found while documenting the repository. It has been fixed on
`main`.

| Issue | Status |
|---|---|
| [Shared artillery model tokens did not match the selector](#shared-artillery-custom-model-tokens-did-not-match-the-selector) | Fixed in [`48eccff`](https://github.com/Bissbert/bkk-arsenal/commit/48eccff) |

## Shared artillery custom-model tokens did not match the selector

**Status: fixed in `48eccff`.** The selector gained a case for each
`arsenal:shell_*` token; see [What was changed](#what-was-changed).

Files and lines, as of `main` today:

- `src/main/java/ch/bissbert/arsenal/ArsenalPlugin.java:116-127` declares seven
  shared artillery ammunition model tokens as `arsenal:shell_*`.
- `src/main/java/ch/bissbert/arsenal/ArsenalPlugin.java:281` writes that token
  to the firework-star item's custom-model-data component.
- `resource-pack/assets/minecraft/items/firework_star.json:38-44` selects the
  ammunition IDs `arsenal:artillery_*`, `arsenal:rocket_salvo`, and
  `arsenal:aa_proximity`. Before the fix these were the only cases; lines
  31-37 now add the `arsenal:shell_*` spellings.

What happened: the source IDs and selector cases cover the same ammunition IDs,
but the model token written into seven shared artillery rounds is not a selector
case. Those rounds therefore took the firework-star fallback instead of
their custom model.

How the mismatch was reproduced before the fix:

```sh
python3 tools/packcheck.py
```

The command reported `source model tokens not in selector cases` followed by
`shell_airburst`, `shell_ap`, `shell_cluster`, `shell_flare`, `shell_he`,
`shell_incendiary`, and `shell_smoke`, then exited nonzero. A live reproduction
would issue one of those rounds, put it in the offhand, fire its compatible
weapon, and inspect the client model with the pack enabled; that server/client
run was not available, and still has not been done.

The diff first proposed here changed the seven enum tokens to match the
existing selector cases:

```diff
diff --git a/src/main/java/ch/bissbert/arsenal/ArsenalPlugin.java b/src/main/java/ch/bissbert/arsenal/ArsenalPlugin.java
@@
-        ARTILLERY_HE("artillery_he", "Artillery HE Shell", "arsenal:shell_he", WeaponType.FIELD_CANNON, NamedTextColor.RED,
+        ARTILLERY_HE("artillery_he", "Artillery HE Shell", "arsenal:artillery_he", WeaponType.FIELD_CANNON, NamedTextColor.RED,
@@
-        ARTILLERY_AP("artillery_ap", "Artillery AP Shell", "arsenal:shell_ap", WeaponType.FIELD_CANNON, NamedTextColor.GOLD,
+        ARTILLERY_AP("artillery_ap", "Artillery AP Shell", "arsenal:artillery_ap", WeaponType.FIELD_CANNON, NamedTextColor.GOLD,
@@
-        ARTILLERY_SMOKE("artillery_smoke", "Artillery Smoke Shell", "arsenal:shell_smoke", WeaponType.FIELD_CANNON, NamedTextColor.GRAY,
+        ARTILLERY_SMOKE("artillery_smoke", "Artillery Smoke Shell", "arsenal:artillery_smoke", WeaponType.FIELD_CANNON, NamedTextColor.GRAY,
@@
-        ARTILLERY_ILLUMINATION("artillery_illumination", "Artillery Illumination Shell", "arsenal:shell_flare", WeaponType.FIELD_CANNON, NamedTextColor.YELLOW,
+        ARTILLERY_ILLUMINATION("artillery_illumination", "Artillery Illumination Shell", "arsenal:artillery_illumination", WeaponType.FIELD_CANNON, NamedTextColor.YELLOW,
@@
-        ROCKET_SALVO("rocket_salvo", "Rocket Salvo", "arsenal:shell_cluster", WeaponType.ROCKET_ARTILLERY, NamedTextColor.LIGHT_PURPLE),
+        ROCKET_SALVO("rocket_salvo", "Rocket Salvo", "arsenal:rocket_salvo", WeaponType.ROCKET_ARTILLERY, NamedTextColor.LIGHT_PURPLE),
@@
-        INCENDIARY_SHELL("incendiary_shell", "Incendiary Artillery Shell", "arsenal:shell_incendiary", WeaponType.FIELD_CANNON, NamedTextColor.DARK_RED,
+        INCENDIARY_SHELL("incendiary_shell", "Incendiary Artillery Shell", "arsenal:incendiary_shell", WeaponType.FIELD_CANNON, NamedTextColor.DARK_RED,
@@
-        AA_PROXIMITY("aa_proximity", "Proximity Anti-Air Shell", "arsenal:shell_airburst", WeaponType.AA_CANNON, NamedTextColor.AQUA);
+        AA_PROXIMITY("aa_proximity", "Proximity Anti-Air Shell", "arsenal:aa_proximity", WeaponType.AA_CANNON, NamedTextColor.AQUA);
```

## What was changed

`48eccff` took the other route: it left the Java tokens alone and added seven
cases to `resource-pack/assets/minecraft/items/firework_star.json`, each mapping
an `arsenal:shell_*` token to the existing artillery model. Changing the tokens
would have left rounds already issued on a server pointing at a case that no
longer exists; adding cases keeps both spellings working.

`tools/packcheck.py` now expects a selector case for every munition ID and for
every model token, and exits 0 on `main`. Run in a Linux container
([capture](../media/captures/linux-run.txt)):

```text
selector cases missing source IDs: none
selector cases not in source IDs: none
source model tokens not in selector cases: none
selector target model files missing: none
exit=0
```

[← back to the overview](../README.md)
