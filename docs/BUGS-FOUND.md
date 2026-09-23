# Bugs found during the documentation pass

[← back to the overview](../README.md)

This file records tracked-source issues found while documenting the repository.
No tracked source or resource file was changed.

## Shared artillery custom-model tokens do not match the selector

Files and lines:

- `src/main/java/ch/bissbert/arsenal/ArsenalPlugin.java:112-123` declares seven
  shared artillery ammunition model tokens as `arsenal:shell_*`.
- `src/main/java/ch/bissbert/arsenal/ArsenalPlugin.java:276` writes that token
  to the firework-star item's custom-model-data component.
- `resource-pack/assets/minecraft/items/firework_star.json:30-36` selects the
  ammunition IDs `arsenal:artillery_*`, `arsenal:rocket_salvo`, and
  `arsenal:aa_proximity` instead.

What happens: the source IDs and selector cases cover the same ammunition IDs,
but the model token written into seven shared artillery rounds is not a selector
case. Those rounds can therefore take the firework-star fallback instead of
their custom model. The repository also contains the seven `shell_*.png`
texture names, but the checked-in selector path does not reach them through the
tokens emitted by the plugin.

How to reproduce the static mismatch:

```sh
python3 tools/packcheck.py
```

The command reports `source model tokens not in selector cases` followed by
`shell_airburst`, `shell_ap`, `shell_cluster`, `shell_flare`, `shell_he`,
`shell_incendiary`, and `shell_smoke`, then exits nonzero. A live reproduction
would issue one of those rounds, put it in the offhand, fire its compatible
weapon, and inspect the client model with the pack enabled; that server/client
run was not available for this pass.

The fix I would make, but did not apply under the documentation-only contract,
is to make each enum token match its existing selector case:

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

Until that is addressed, the mismatch remains a known limitation in the
[resource-pack write-up](resource-pack.md) and the root README.

[← back to the overview](../README.md)
