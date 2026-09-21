package ch.bissbert.arsenal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.*;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public final class ArsenalPlugin extends JavaPlugin implements Listener, TabExecutor {
    private enum AmmoCategory {
        LAUNCHER("launcher", "Launcher", NamedTextColor.RED),
        SHOTGUN("shotgun", "Shotgun", NamedTextColor.GOLD),
        RIFLE("rifle", "Rifle", NamedTextColor.GREEN),
        SNIPER("sniper", "Sniper", NamedTextColor.AQUA),
        MORTAR("mortar", "Mortar", NamedTextColor.DARK_PURPLE),
        ARTILLERY("artillery", "Artillery", NamedTextColor.DARK_RED);
        final String id, title;
        final NamedTextColor color;
        AmmoCategory(String id, String title, NamedTextColor color) {
            this.id = id; this.title = title; this.color = color;
        }
        static AmmoCategory parse(String value) {
            if (value == null) return null;
            String normalized = value.replace('-', '_');
            for (AmmoCategory category : values()) if (category.id.equalsIgnoreCase(normalized)) return category;
            return null;
        }
        static AmmoCategory forWeapon(WeaponType weapon) {
            return switch (weapon) {
                case LAUNCHER -> LAUNCHER;
                case SHOTGUN -> SHOTGUN;
                case RIFLE -> RIFLE;
                case SNIPER -> SNIPER;
                case MORTAR -> MORTAR;
                case FIELD_CANNON, HOWITZER, ROCKET_ARTILLERY, AA_CANNON -> ARTILLERY;
            };
        }
    }

    private enum WeaponType {
        LAUNCHER("launcher", "Launcher", "arsenal:launcher", NamedTextColor.RED),
        SHOTGUN("shotgun", "Shotgun", "arsenal:shotgun", NamedTextColor.GOLD),
        RIFLE("rifle", "Rifle", "arsenal:rifle", NamedTextColor.GREEN),
        SNIPER("sniper", "Sniper Rifle", "arsenal:sniper", NamedTextColor.AQUA),
        MORTAR("mortar", "TNT Mortar", "arsenal:mortar", NamedTextColor.DARK_PURPLE),
        FIELD_CANNON("field_cannon", "Field Cannon", "arsenal:field_cannon", NamedTextColor.GRAY),
        HOWITZER("howitzer", "Howitzer", "arsenal:howitzer", NamedTextColor.DARK_GREEN),
        ROCKET_ARTILLERY("rocket_artillery", "Rocket Artillery", "arsenal:rocket_artillery", NamedTextColor.DARK_RED),
        AA_CANNON("aa_cannon", "Anti-Air Cannon", "arsenal:aa_cannon", NamedTextColor.AQUA);
        final String id, title, model;
        final NamedTextColor color;
        WeaponType(String id, String title, String model, NamedTextColor color) {
            this.id = id; this.title = title; this.model = model; this.color = color;
        }
        static WeaponType parse(String value) {
            if (value == null) return null;
            String normalized = value.replace('-', '_');
            for (WeaponType type : values()) if (type.id.equalsIgnoreCase(normalized)) return type;
            return null;
        }
        boolean indirect() {
            return this == MORTAR || this == FIELD_CANNON || this == HOWITZER || this == ROCKET_ARTILLERY;
        }
    }
    private enum MunitionType {
        HE_ROCKET("he_rocket", "HE Rocket", "arsenal:he_rocket", WeaponType.LAUNCHER, NamedTextColor.RED),
        DEMOLITION("demolition", "Demolition Rocket", "arsenal:demolition", WeaponType.LAUNCHER, NamedTextColor.DARK_RED),
        STICKY("sticky", "Sticky Charge", "arsenal:sticky", WeaponType.LAUNCHER, NamedTextColor.YELLOW),
        BUCKSHOT("buckshot", "Buckshot", "arsenal:buckshot", WeaponType.SHOTGUN, NamedTextColor.RED),
        BREACHING("breaching", "Breaching Shell", "arsenal:breaching", WeaponType.SHOTGUN, NamedTextColor.GOLD),
        SLUG("slug", "Explosive Slug", "arsenal:slug", WeaponType.SHOTGUN, NamedTextColor.WHITE),
        INCENDIARY_SHOT("incendiary_shot", "Incendiary Shot", "arsenal:incendiary_shot", WeaponType.SHOTGUN, NamedTextColor.DARK_RED),
        STANDARD("standard", "Standard Rifle Round", "arsenal:standard", WeaponType.RIFLE, NamedTextColor.GRAY),
        ARMOR_PIERCING("armor_piercing", "Armor-Piercing Rifle Round", "arsenal:armor_piercing", WeaponType.RIFLE, NamedTextColor.GOLD),
        TRACER("tracer", "Tracer Round", "arsenal:tracer", WeaponType.RIFLE, NamedTextColor.YELLOW),
        DISRUPTOR("disruptor", "Redstone Disruptor", "arsenal:disruptor", WeaponType.RIFLE, NamedTextColor.AQUA),
        PRECISION("precision", "Precision Sniper Round", "arsenal:precision", WeaponType.SNIPER, NamedTextColor.WHITE),
        ANTI_MATERIEL("anti_materiel", "Anti-Materiel Round", "arsenal:anti_materiel", WeaponType.SNIPER, NamedTextColor.DARK_RED),
        MARKER("marker", "Target Marker Round", "arsenal:marker", WeaponType.SNIPER, NamedTextColor.GREEN),
        SHATTER("shatter", "Shatter Round", "arsenal:shatter", WeaponType.SNIPER, NamedTextColor.LIGHT_PURPLE),
        HE("he", "HE Mortar Grenade", "arsenal:he", WeaponType.MORTAR, NamedTextColor.RED),
        PENETRATING("penetrating", "Penetrating Mortar Grenade", "arsenal:penetrating", WeaponType.MORTAR, NamedTextColor.GOLD),
        DEPTH_CHARGE("depth_charge", "THE DEPTH CHARGE", "arsenal:depth_charge", WeaponType.MORTAR, NamedTextColor.DARK_PURPLE),
        AIRBURST("airburst", "Airburst Mortar Grenade", "arsenal:airburst", WeaponType.MORTAR, NamedTextColor.AQUA),
        CLUSTER("cluster", "Cluster Mortar Grenade", "arsenal:cluster", WeaponType.MORTAR, NamedTextColor.LIGHT_PURPLE),
        INCENDIARY_GRENADE("incendiary_grenade", "Incendiary Mortar Grenade", "arsenal:incendiary_grenade", WeaponType.MORTAR, NamedTextColor.DARK_RED),
        SMOKE("smoke", "Smoke Mortar Grenade", "arsenal:smoke", WeaponType.MORTAR, NamedTextColor.GRAY),
        ILLUMINATION("illumination", "Illumination Flare", "arsenal:illumination", WeaponType.MORTAR, NamedTextColor.YELLOW),
        ARTILLERY_HE("artillery_he", "Artillery HE Shell", "arsenal:shell_he", WeaponType.FIELD_CANNON, NamedTextColor.RED,
                WeaponType.HOWITZER, WeaponType.ROCKET_ARTILLERY),
        ARTILLERY_AP("artillery_ap", "Artillery AP Shell", "arsenal:shell_ap", WeaponType.FIELD_CANNON, NamedTextColor.GOLD,
                WeaponType.HOWITZER),
        ARTILLERY_SMOKE("artillery_smoke", "Artillery Smoke Shell", "arsenal:shell_smoke", WeaponType.FIELD_CANNON, NamedTextColor.GRAY,
                WeaponType.HOWITZER, WeaponType.ROCKET_ARTILLERY),
        ARTILLERY_ILLUMINATION("artillery_illumination", "Artillery Illumination Shell", "arsenal:shell_flare", WeaponType.FIELD_CANNON, NamedTextColor.YELLOW,
                WeaponType.HOWITZER),
        ROCKET_SALVO("rocket_salvo", "Rocket Salvo", "arsenal:shell_cluster", WeaponType.ROCKET_ARTILLERY, NamedTextColor.LIGHT_PURPLE),
        INCENDIARY_SHELL("incendiary_shell", "Incendiary Artillery Shell", "arsenal:shell_incendiary", WeaponType.FIELD_CANNON, NamedTextColor.DARK_RED,
                WeaponType.HOWITZER, WeaponType.ROCKET_ARTILLERY),
        AA_PROXIMITY("aa_proximity", "Proximity Anti-Air Shell", "arsenal:shell_airburst", WeaponType.AA_CANNON, NamedTextColor.AQUA);
        final String id, title, model; final WeaponType weapon; final EnumSet<WeaponType> compatible; final NamedTextColor color;
        MunitionType(String id, String title, String model, WeaponType weapon, NamedTextColor color, WeaponType... additional) {
            this.id = id; this.title = title; this.model = model; this.weapon = weapon; this.color = color;
            this.compatible = EnumSet.of(weapon, additional);
        }
        static MunitionType parse(String value) {
            if (value == null) return null;
            String normalized = value.replace('-', '_');
            for (MunitionType type : values()) if (type.id.equalsIgnoreCase(normalized)) return type;
            return null;
        }
        AmmoCategory category() {
            return AmmoCategory.forWeapon(weapon);
        }
        static MunitionType standard(WeaponType weapon) { return switch (weapon) {
            case LAUNCHER -> HE_ROCKET; case SHOTGUN -> BUCKSHOT; case RIFLE -> STANDARD;
            case SNIPER -> PRECISION; case MORTAR -> HE; case FIELD_CANNON, HOWITZER -> ARTILLERY_HE;
            case ROCKET_ARTILLERY -> ROCKET_SALVO; case AA_CANNON -> AA_PROXIMITY;
        }; }
    }
    private NamespacedKey gunKey;
    private NamespacedKey projectileKey;
    private NamespacedKey grenadeKey;
    private final Map<UUID, Shot> shots = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Location> mortarTargets = new HashMap<>();
    private final Set<Block> activeLights = new HashSet<>();
    private final Map<Block, Integer> lightReferences = new HashMap<>();
    private final Map<Block, BlockData> lightOriginal = new HashMap<>();
    private final Set<BukkitTask> effectTasks = new HashSet<>();
    private final Map<ChunkPos, Integer> chunkReferences = new HashMap<>();
    private Settings settings;

    @Override public void onEnable() {
        gunKey = new NamespacedKey(this, "gun");
        projectileKey = new NamespacedKey(this, "projectile");
        grenadeKey = new NamespacedKey(this, "grenade");
        saveDefaultConfig();
        settings = readSettings();
        Objects.requireNonNull(getCommand("arsenal")).setExecutor(this);
        Objects.requireNonNull(getCommand("arsenal")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        for (World world : getServer().getWorlds()) {
            for (BlockDisplay display : world.getEntitiesByClass(BlockDisplay.class)) cleanupStale(display);
        }
        getServer().getScheduler().runTaskTimer(this, this::tick, 1, 1);
        getLogger().info("BKK Arsenal ready; weapons=" + settings.weapons.size()
                + ", block-damage=" + settings.blockDamage);
    }

    @Override public void onDisable() {
        for (Shot shot : shots.values()) shot.cleanup();
        shots.clear();
        cooldowns.clear();
        mortarTargets.clear();
        for (Block block : List.copyOf(activeLights)) releaseLight(block);
        activeLights.clear();
        effectTasks.forEach(BukkitTask::cancel);
        effectTasks.clear();
    }

    private Settings readSettings() {
        Material ammo = Material.matchMaterial(getConfig().getString("ammo-material", "TNT"));
        if (ammo == null || !ammo.isItem() || ammo.isAir()) {
            getLogger().warning("Invalid ammo-material; using TNT.");
            ammo = Material.TNT;
        }
        EnumMap<WeaponType, WeaponSettings> weapons = new EnumMap<>(WeaponType.class);
        weapons.put(WeaponType.LAUNCHER, weaponSettings("launcher", 3, .03, 4, 30, 1, 0));
        weapons.put(WeaponType.SHOTGUN, weaponSettings("shotgun", 3.5, .035, 1.5, 20, 7, 7));
        weapons.put(WeaponType.RIFLE, weaponSettings("rifle", 45.25, .0245, 2.5, 3, 1, 0));
        weapons.put(WeaponType.SNIPER, weaponSettings("sniper", 60, .018, 5, 40, 1, 0));
        weapons.put(WeaponType.MORTAR, weaponSettings("mortar", 2, .01, 8, 100, 1, 0));
        weapons.put(WeaponType.FIELD_CANNON, weaponSettings("field-cannon", 3.0, .012, 7, 60, 1, 0));
        weapons.put(WeaponType.HOWITZER, weaponSettings("howitzer", 2.2, .01, 9, 100, 1, 0));
        weapons.put(WeaponType.ROCKET_ARTILLERY, weaponSettings("rocket-artillery", 2.8, .015, 5, 160, 6, 5));
        weapons.put(WeaponType.AA_CANNON, weaponSettings("aa-cannon", 12, .02, 3, 8, 1, 0));
        return new Settings(ammo, (int) number("ammo-per-shot", 1, 1, 64),
                getConfig().getBoolean("block-damage", true), getConfig().getBoolean("set-fire", false),
                (int) number("max-active-projectiles", 100, 1, 500), weapons,
                number("mortar-grenades.penetration-depth", 8, 0, 16),
                (float) number("mortar-grenades.penetrating-power", 8, 0, 8),
                number("mortar-grenades.airburst-height", 8, 1, 32),
                (float) number("mortar-grenades.airburst-power", 7, 0, 8),
                number("mortar-targeting.max-distance", 512, 16, 2048),
                number("mortar-targeting.arc-clearance", 64, 8, 256),
                number("smoke.radius", 6, 1, 24), number("smoke.height", 6, 1, 16),
                (int) number("smoke.density", 240, 40, 800),
                (int) number("smoke.duration-ticks", 400, 20, 6000),
                (int) number("incendiary.fire-radius", 6, 1, 16),
                (int) number("incendiary.terrain-radius", 2, 0, 8));
    }

    private WeaponSettings weaponSettings(String id, double speed, double gravity, double power,
                                          int cooldown, int pellets, double spread) {
        String p = "weapons." + id + ".";
        return new WeaponSettings(number(weaponKey(id, "speed"), speed, .1, 100),
                number(weaponKey(id, "gravity"), gravity, 0, .5),
                (float) number(weaponKey(id, "explosion-power"), power, 0, 16),
                (int) number(weaponKey(id, "cooldown-ticks"), cooldown, 1, 1200),
                (int) number(p + "projectiles", pellets, 1, 16), number(p + "spread-degrees", spread, 0, 30));
    }

    private String weaponKey(String id, String key) {
        String nested = "weapons." + id + "." + key;
        // Preserve each old launcher setting independently while allowing an
        // in-game command to write and override a new nested setting.
        if (getConfig().contains(nested)) return nested;
        return id.equals("launcher") && getConfig().contains(key) ? key : nested;
    }

    private double number(String key, double fallback, double min, double max) {
        double value = getConfig().getDouble(key, fallback);
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }

    private ItemStack gun(WeaponType type) {
        WeaponSettings weapon = settings.weapons.get(type);
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.title, type.color).decoration(TextDecoration.ITALIC, false));
        String controls = type.indirect() ? "Right-click: lock target. Left-click: fire." : "Right-click to fire where you look.";
        meta.lore(List.of(lore(controls), lore("Explodes only on impact with blocks or entities."),
                lore(weapon.projectiles > 1 ? weapon.projectiles + " projectiles, " + weapon.spread + "° spread."
                        : "Velocity: " + weapon.speed * 20 + " blocks/second."),
                lore("Ammo: " + settings.ammoPerShot + " " + settings.ammo.name() + " per shot."),
                lore("Cooldown: " + weapon.cooldown / 20.0 + " seconds. No artificial range limit.")));
        meta.getPersistentDataContainer().set(gunKey, PersistentDataType.STRING, type.id);
        var model = meta.getCustomModelDataComponent();
        model.setStrings(List.of(type.model));
        meta.setCustomModelDataComponent(model);
        meta.setMaxStackSize(1);
        item.setItemMeta(meta);
        return item;
    }

    private Component lore(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack munition(MunitionType type, int amount) {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR, amount);
        ItemMeta meta = item.getItemMeta();
        Component category = Component.text("[" + type.category().title + "] ", type.category().color)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(category.append(Component.text(type.title, type.color)
                .decoration(TextDecoration.ITALIC, false)));
        meta.lore(List.of(lore("Category: " + type.category().title),
                lore(type.weapon.title + " ammunition."), lore("Hold in offhand to select this round.")));
        meta.getPersistentDataContainer().set(grenadeKey, PersistentDataType.STRING, type.id);
        var model = meta.getCustomModelDataComponent(); model.setStrings(List.of(type.model)); meta.setCustomModelDataComponent(model);
        item.setItemMeta(meta);
        return item;
    }

    private MunitionType munitionType(ItemStack item) {
        if (item == null || item.getType() != Material.FIREWORK_STAR || !item.hasItemMeta()) return null;
        return MunitionType.parse(item.getItemMeta().getPersistentDataContainer().get(grenadeKey, PersistentDataType.STRING));
    }

    private WeaponType weaponType(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return null;
        var data = item.getItemMeta().getPersistentDataContainer();
        String id = data.get(gunKey, PersistentDataType.STRING);
        if (id != null) return WeaponType.parse(id);
        return data.has(gunKey, PersistentDataType.BYTE) ? WeaponType.LAUNCHER : null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void interact(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        WeaponType type = weaponType(event.getItem());
        if (type == null) return;
        if (type.indirect()) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true); selectIndirectTarget(event.getPlayer(), type);
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true); fireIndirect(event.getPlayer(), type);
            }
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // Right-click-air is pre-cancelled by vanilla for non-usable items. Only
        // honor explicit denial for block interactions, where protection matters.
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.useItemInHand() == Event.Result.DENY) return;
        event.setCancelled(true);
        fire(event.getPlayer(), type, null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void interactEntity(PlayerInteractEntityEvent event) {
        WeaponType type = weaponType(event.getPlayer().getInventory().getItemInMainHand());
        if (event.getHand() != EquipmentSlot.HAND || type == null) return;
        event.setCancelled(true);
        if (type.indirect()) {
            Location target = event.getRightClicked().getLocation().add(0, event.getRightClicked().getHeight() * .5, 0);
            mortarTargets.put(event.getPlayer().getUniqueId(), target);
            showTarget(event.getPlayer(), target, type);
        } else fire(event.getPlayer(), type, null);
    }

    private void message(Player player, String text) {
        player.sendActionBar(Component.text(text, NamedTextColor.GOLD));
    }

    private void selectIndirectTarget(Player player, WeaponType type) {
        RayTraceResult trace = player.rayTraceBlocks(settings.mortarTargetDistance, FluidCollisionMode.ALWAYS);
        if (trace == null || trace.getHitPosition() == null) {
            message(player, "No target in sight within " + (int) settings.mortarTargetDistance + " blocks."); return;
        }
        Location target = trace.getHitPosition().toLocation(player.getWorld());
        mortarTargets.put(player.getUniqueId(), target);
        showTarget(player, target, type);
    }

    private void showTarget(Player player, Location target, WeaponType type) {
        target.getWorld().spawnParticle(Particle.END_ROD, target.clone().add(0, .5, 0), 20, .5, .5, .5, .03);
        message(player, type.title + " target locked: " + target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ()
                + " (" + (int) player.getLocation().distance(target) + "m)");
    }

    private void fireIndirect(Player player, WeaponType type) {
        Location target = mortarTargets.get(player.getUniqueId());
        if (target == null || !target.getWorld().equals(player.getWorld())) {
            message(player, "Right-click a visible target before firing the " + type.title + "."); return;
        }
        Location start = player.getEyeLocation();
        double gravity = settings.weapons.get(type).gravity;
        double clearance = switch (type) {
            case MORTAR -> settings.mortarArcClearance;
            case FIELD_CANNON -> 24;
            case HOWITZER -> 96;
            case ROCKET_ARTILLERY -> 64;
            default -> settings.mortarArcClearance;
        };
        double apexY = Math.max(start.getY(), target.getY()) + clearance;
        double up = Math.sqrt(2 * gravity * Math.max(0, apexY - start.getY()));
        double upTime = up / gravity;
        double downTime = Math.sqrt(2 * Math.max(0, apexY - target.getY()) / gravity);
        int flightTicks = Math.max(1, (int) Math.ceil(upTime + downTime));
        // Exact for the plugin's discrete per-tick gravity integration:
        // displacement = v0*t - gravity*t*(t-1)/2.
        double verticalVelocity = (target.getY() - start.getY()
                + gravity * flightTicks * (flightTicks - 1) / 2) / flightTicks;
        Vector velocity = new Vector((target.getX() - start.getX()) / flightTicks, verticalVelocity,
                (target.getZ() - start.getZ()) / flightTicks);
        fire(player, type, velocity);
    }

    private void fire(Player player, WeaponType type, Vector targetedVelocity) {
        WeaponSettings weapon = settings.weapons.get(type);
        MunitionType selected = munitionType(player.getInventory().getItemInOffHand());
        if (selected != null && !selected.compatible.contains(type)) {
            message(player, selected.title + " is not compatible with the " + type.title + "."); return;
        }
        MunitionType payload = selected == null ? MunitionType.standard(type) : selected;
        int projectileCount = switch (payload) { case SLUG -> 1; case BREACHING -> 3; default -> weapon.projectiles; };
        double shotSpread = switch (payload) { case SLUG -> 0; case BREACHING -> 2; default -> weapon.spread; };
        if (!player.hasPermission("arsenal.use")) { message(player, "You do not have permission to use Arsenal weapons."); return; }
        if (player.isDead() || (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.CREATIVE)) return;
        long now = System.nanoTime();
        if (now < cooldowns.getOrDefault(player.getUniqueId(), 0L)) return;
        if (shots.size() + projectileCount > settings.maxActive) { message(player, "Too many projectiles in flight. Try again shortly."); return; }
        boolean free = player.getGameMode() == GameMode.CREATIVE || player.hasPermission("arsenal.infiniteammo");
        if (!free && selected != null && player.getInventory().getItemInOffHand().getAmount() < settings.ammoPerShot) {
            message(player, "Requires " + settings.ammoPerShot + " selected round(s) in your offhand."); return;
        }
        if (!free && selected == null && !hasAmmo(player)) { message(player, "Requires " + settings.ammoPerShot + " " + settings.ammo.name() + " in your inventory."); return; }
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        if (!free && selected != null) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            offhand.setAmount(offhand.getAmount() - settings.ammoPerShot);
            player.getInventory().setItemInOffHand(offhand.getAmount() == 0 ? null : offhand);
        } else if (!free) consumeAmmo(player);
        int effectiveCooldown = payload == MunitionType.DEMOLITION ? weapon.cooldown * 2 : weapon.cooldown;
        cooldowns.put(player.getUniqueId(), now + effectiveCooldown * 50_000_000L);
        for (int pellet = 0; pellet < projectileCount; pellet++) {
            Vector shotDirection = spread(direction, shotSpread);
            BlockDisplay display = start.getWorld().spawn(start, BlockDisplay.class, entity -> {
            entity.setBlock(Material.TNT.createBlockData());
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            entity.setSilent(true);
            entity.setTeleportDuration(1);
            entity.setViewRange(1.5f);
            entity.setTransformation(new Transformation(new Vector3f(-.225f, -.225f, -.225f),
                    new Quaternionf(), new Vector3f(.45f), new Quaternionf()));
            entity.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
            });
            Vector projectileVelocity = targetedVelocity == null ? shotDirection.multiply(weapon.speed) : targetedVelocity.clone();
            shots.put(display.getUniqueId(), new Shot(player.getUniqueId(), display, start.clone(),
                    projectileVelocity, type, weapon, payload));
        }
        start.getWorld().playSound(start, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, .8f, .65f);
        player.swingMainHand();
    }

    private Vector spread(Vector forward, double degrees) {
        if (degrees == 0) return forward.clone();
        double radius = Math.tan(Math.toRadians(degrees)) * Math.sqrt(Math.random());
        double angle = Math.random() * Math.PI * 2;
        Vector up = Math.abs(forward.getY()) < .99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector right = forward.clone().crossProduct(up).normalize();
        up = right.clone().crossProduct(forward).normalize();
        return forward.clone().add(right.multiply(Math.cos(angle) * radius))
                .add(up.multiply(Math.sin(angle) * radius)).normalize();
    }

    private void spawnSmoke(Location origin) {
        World world = origin.getWorld();
        int[] age = {0};
        BukkitTask[] holder = {null};
        holder[0] = getServer().getScheduler().runTaskTimer(this, () -> {
            if (!world.equals(origin.getWorld()) || !world.isChunkLoaded(origin.getBlockX() >> 4, origin.getBlockZ() >> 4)
                    || age[0] >= settings.smokeDuration) {
                if (holder[0] != null) { holder[0].cancel(); effectTasks.remove(holder[0]); }
                return;
            }
            double drift = age[0] * .025;
            Location base = origin.clone().add(drift, 0, drift * .35);
            int layers = Math.max(3, (int) Math.ceil(settings.smokeHeight / 1.25));
            int perLayer = Math.max(8, (int) Math.ceil(settings.smokeDensity / (double) layers));
            for (int layer = 0; layer < layers; layer++) {
                double fraction = (layer + .5) / layers;
                Location center = base.clone().add(0, settings.smokeHeight * fraction, 0);
                double radius = settings.smokeRadius * (.88 + .12 * fraction);
                double layerHeight = Math.max(.65, settings.smokeHeight / layers * .75);
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, perLayer,
                        radius * .72, layerHeight, radius * .72, .008);
                world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center, Math.max(4, perLayer / 2),
                        radius * .55, layerHeight * .8, radius * .55, .004);
            }
            world.spawnParticle(Particle.LARGE_SMOKE, base.clone().add(0, settings.smokeHeight * .45, 0),
                    Math.max(10, perLayer), settings.smokeRadius * .55, settings.smokeHeight * .45,
                    settings.smokeRadius * .55, .002);
            age[0]++;
        }, 0, 1);
        effectTasks.add(holder[0]);
    }

    private void scorchTerrain(Location origin, int configuredRadius) {
        World world = origin.getWorld();
        int radius = Math.max(1, Math.min(8, configuredRadius));
        int centerX = origin.getBlockX(), centerY = origin.getBlockY(), centerZ = origin.getBlockZ();
        int minY = Math.max(world.getMinHeight(), centerY - 3);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + 1);
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz > radius * radius) continue;
            int x = centerX + dx, z = centerZ + dz;
            for (int y = maxY; y >= minY; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (!block.getType().isSolid() || isIndestructible(block.getType())) continue;
                block.setType(Material.AIR, false);
                break;
            }
        }
    }

    private boolean acquireLight(Block block) {
        Integer references = lightReferences.get(block);
        if (references != null) {
            lightReferences.put(block, references + 1);
            activeLights.add(block);
            return true;
        }
        if (!block.getType().isAir()) return false;
        lightOriginal.put(block, block.getBlockData().clone());
        Light data = (Light) Material.LIGHT.createBlockData();
        data.setLevel(15);
        block.setBlockData(data, false);
        lightReferences.put(block, 1);
        activeLights.add(block);
        return true;
    }

    private void releaseLight(Block block) {
        Integer references = lightReferences.get(block);
        if (references == null) return;
        if (references > 1) {
            lightReferences.put(block, references - 1);
            return;
        }
        lightReferences.remove(block);
        activeLights.remove(block);
        BlockData original = lightOriginal.remove(block);
        if (original != null && block.getType() == Material.LIGHT) block.setBlockData(original, false);
    }

    private void leaveIncendiaryFire(Location origin, int configuredRadius) {
        World world = origin.getWorld();
        int radius = Math.max(1, Math.min(16, configuredRadius));
        int centerX = origin.getBlockX(), centerY = origin.getBlockY(), centerZ = origin.getBlockZ();
        int minY = Math.max(world.getMinHeight(), centerY - 8);
        int maxY = Math.min(world.getMaxHeight() - 2, centerY + 3);
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz > radius * radius) continue;
            for (int y = maxY; y >= minY; y--) {
                Block ground = world.getBlockAt(centerX + dx, y, centerZ + dz);
                Block flame = world.getBlockAt(centerX + dx, y + 1, centerZ + dz);
                if (!ground.getType().isSolid() || !flame.getType().isAir()) continue;
                flame.setType(Material.FIRE, false);
                break;
            }
        }
    }

    private boolean isIndestructible(Material material) {
        return material == Material.BEDROCK || material == Material.BARRIER || material == Material.END_PORTAL
                || material == Material.END_PORTAL_FRAME || material == Material.COMMAND_BLOCK
                || material == Material.REPEATING_COMMAND_BLOCK || material == Material.CHAIN_COMMAND_BLOCK;
    }

    private boolean hasAmmo(Player player) {
        int found = 0;
        for (ItemStack item : player.getInventory().getStorageContents())
            if (item != null && item.getType() == settings.ammo && !item.hasItemMeta()) found += item.getAmount();
        return found >= settings.ammoPerShot;
    }

    private void consumeAmmo(Player player) {
        int remaining = settings.ammoPerShot;
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() != settings.ammo || item.hasItemMeta()) continue;
            int take = Math.min(remaining, item.getAmount());
            remaining -= take;
            item.setAmount(item.getAmount() - take);
            player.getInventory().setItem(slot, item.getAmount() == 0 ? null : item);
        }
    }

    private void tick() {
        // Snapshot permits other plugins' explosion callbacks to unload a world
        // or disconnect a player without invalidating this iteration.
        for (Shot shot : List.copyOf(shots.values())) {
            if (!shots.containsKey(shot.display.getUniqueId())) continue;
            try {
                if (!shot.advance()) {
                    shots.remove(shot.display.getUniqueId());
                    shot.cleanup();
                }
            } catch (RuntimeException exception) {
                shots.remove(shot.display.getUniqueId());
                shot.cleanup();
                getLogger().log(java.util.logging.Level.WARNING, "Removed failed Arsenal projectile", exception);
            }
        }
    }

    private final class Shot {
        final UUID owner;
        final BlockDisplay display;
        final Vector velocity;
        final WeaponType weapon;
        final WeaponSettings options;
        final MunitionType munition;
        final FlightState flight;
        final Set<ChunkPos> ticketedChunks = new HashSet<>();
        int stickyFuse = -1;
        Location position;
        Shot(UUID owner, BlockDisplay display, Location position, Vector velocity, WeaponType weapon,
             WeaponSettings options, MunitionType munition) {
            this.owner = owner; this.display = display; this.position = position; this.weapon = weapon;
            this.velocity = velocity; this.options = options; this.munition = munition;
            this.flight = new FlightState();
        }
        boolean advance() {
            Player shooter = getServer().getPlayer(owner);
            World world = position.getWorld();
            if (shooter == null || shooter.isDead() || !shooter.getWorld().equals(world)) return false;
            if (stickyFuse >= 0) {
                if (stickyFuse-- == 0) return detonate(shooter, position, 6);
                world.spawnParticle(Particle.SMOKE, position, 1, 0, 0, 0, 0);
                return true;
            }
            double distance = flight.nextDistance(velocity.length());
            if (distance == 0) return false;
            Vector segment = velocity.clone();
            if (segment.lengthSquared() == 0) return false;
            if (segment.length() != distance) segment.normalize().multiply(distance);
            Vector direction = segment.clone().normalize();
            Location next = position.clone().add(segment);
            // The build ceiling is not an entity-flight ceiling. Shells may arc
            // above max build height and later descend back into the world.
            if (next.getY() < world.getMinHeight() || !world.getWorldBorder().isInside(next)) return false;
            // Keep only the already-generated chunks intersected by this swept
            // segment loaded. This makes ballistics independent of client/server
            // view distance without generating new terrain or leaking tickets.
            if (!ticketSegment(world, segment)) return false;
            double triggerHeight = switch (munition) {
                case AIRBURST, AA_PROXIMITY -> settings.airburstHeight;
                case CLUSTER, ROCKET_SALVO -> 16;
                case ILLUMINATION, ARTILLERY_ILLUMINATION -> 12;
                default -> 0;
            };
            if (triggerHeight > 0 && velocity.getY() < 0 && world.rayTraceBlocks(position,
                    new Vector(0, -1, 0), triggerHeight, FluidCollisionMode.ALWAYS, false) != null) {
                return detonate(shooter, position, options.power);
            }
            if (munition == MunitionType.AA_PROXIMITY && hasNearbyTarget(world, position, owner, 6))
                return detonate(shooter, position, options.power);
            RayTraceResult hit = world.rayTrace(position, direction, distance, FluidCollisionMode.ALWAYS,
                    false, .225, entity -> entity.isValid() && !entity.getUniqueId().equals(owner)
                            && (entity instanceof LivingEntity || entity instanceof Vehicle || entity instanceof Hanging)
                            && (!(entity instanceof Player p) || p.getGameMode() != GameMode.SPECTATOR));
            if (hit != null) {
                Location impact = hit.getHitPosition().toLocation(world).subtract(direction.clone().multiply(.04));
                if (munition == MunitionType.STICKY && hit.getHitBlock() != null) {
                    position = impact; if (display.isValid()) display.teleport(position); velocity.zero(); stickyFuse = 40; return true;
                }
                if (munition == MunitionType.DEPTH_CHARGE && hit.getHitBlock() != null) {
                    spawnDepthCharge(shooter, impact, direction);
                    finish();
                    return false;
                }
                double penetration = switch (munition) {
                    case PENETRATING -> settings.penetrationDepth; case ANTI_MATERIEL -> 6; case ARMOR_PIERCING -> 2;
                    case ARTILLERY_AP -> settings.penetrationDepth + 4;
                    case BREACHING -> 2; default -> 0;
                };
                if (penetration > 0 && hit.getHitBlock() != null) impact.add(direction.clone().multiply(penetration));
                return detonate(shooter, impact, options.power);
            }
            position = next;
            if (display.isValid()) display.teleport(position);
            world.spawnParticle(munition == MunitionType.TRACER ? Particle.END_ROD : Particle.SMOKE,
                    position, 1, 0, 0, 0, 0);
            velocity.setY(velocity.getY() - options.gravity);
            return true;
        }

        private boolean hasNearbyTarget(World world, Location center, UUID owner, double radius) {
            double r2 = radius * radius;
            for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
                if (!entity.isValid() || entity.getUniqueId().equals(owner)) continue;
                if (!(entity instanceof LivingEntity || entity instanceof Vehicle || entity instanceof Hanging)) continue;
                if (entity.getLocation().distanceSquared(center) <= r2) return true;
            }
            return false;
        }

        private boolean detonate(Player shooter, Location location, float power) {
            if (!finish()) return false;
            World world = location.getWorld();
            switch (munition) {
                case MARKER -> spawnMarker(location, "TARGET", Material.REDSTONE_BLOCK, 600);
                case DISRUPTOR -> disruptRedstone(location);
                case SMOKE, ARTILLERY_SMOKE -> spawnSmoke(location);
                case ILLUMINATION, ARTILLERY_ILLUMINATION -> spawnIllumination(location);
                case CLUSTER, ROCKET_SALVO -> spawnCluster(shooter, location);
                case SHATTER -> {
                    float shatterPower = scaledImpact(2);
                    world.createExplosion(shooter, location, shatterPower, false, settings.blockDamage, false);
                    for (int i = 0; i < 6; i++) {
                        double angle = i * Math.PI / 3;
                        world.createExplosion(shooter, location.clone().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3),
                                scaledImpact(1.2f), false, settings.blockDamage, false);
                    }
                }
                default -> {
                    float actual = switch (munition) {
                        case DEMOLITION -> scaledImpact(7); case ANTI_MATERIEL -> scaledImpact(7); case SLUG -> scaledImpact(3.5f); case BREACHING, ARMOR_PIERCING -> scaledImpact(2);
                        case TRACER -> 1; case INCENDIARY_SHOT -> 1.2f; case PENETRATING, ARTILLERY_AP -> settings.penetratingPower;
                        case AIRBURST -> settings.airburstPower; case AA_PROXIMITY -> scaledImpact(Math.min(4, power));
                        case INCENDIARY_GRENADE -> 3; case INCENDIARY_SHELL -> scaledImpact(3);
                        case ARTILLERY_HE -> scaledImpact(power); default -> power;
                    };
                    if (munition == MunitionType.PENETRATING || munition == MunitionType.ARTILLERY_AP)
                        actual = scaledImpact(actual);
                    boolean incendiary = munition == MunitionType.INCENDIARY_SHOT
                            || munition == MunitionType.INCENDIARY_GRENADE || munition == MunitionType.INCENDIARY_SHELL;
                    boolean exploded = world.createExplosion(shooter, location, actual, incendiary || settings.fire,
                            settings.blockDamage && !incendiary, false);
                    if (incendiary && exploded) {
                        scorchTerrain(location, settings.incendiaryTerrainRadius);
                        leaveIncendiaryFire(location, settings.incendiaryFireRadius);
                    }
                }
            }
            return false;
        }

        private float impactScale() {
            return switch (weapon) {
                case MORTAR -> 1.0f;
                case FIELD_CANNON -> 1.15f;
                case HOWITZER -> 1.45f;
                case ROCKET_ARTILLERY -> 0.80f;
                case AA_CANNON -> 0.65f;
                default -> 1.0f;
            };
        }

        private float scaledImpact(float base) {
            return Math.clamp(base * impactScale(), 0.0f, 16.0f);
        }

        private boolean finish() {
            if (!flight.impact()) return false;
            shots.remove(display.getUniqueId()); if (display.isValid()) display.remove();
            return true;
        }

        private void spawnDepthCharge(Player shooter, Location impact, Vector direction) {
            World world = impact.getWorld();
            Set<ChunkPos> held = new HashSet<>();
            for (int stage = 0; stage < 4; stage++) {
                int depth = 4 + stage * 2;
                Location blast = impact.clone().add(direction.clone().multiply(depth));
                ChunkPos chunk = new ChunkPos(world.getUID(), blast.getBlockX() >> 4, blast.getBlockZ() >> 4);
                if (world.isChunkGenerated(chunk.x, chunk.z) && held.add(chunk))
                    claimChunk(world, chunk);
                int power = 9 - stage;
                int delay = stage * 4;
                getServer().getScheduler().runTaskLater(ArsenalPlugin.this, () -> {
                    world.spawnParticle(Particle.EXPLOSION, blast, 3, .3, .3, .3, 0);
                    world.createExplosion(shooter, blast, power, settings.fire, settings.blockDamage, false);
                }, delay);
            }
            getServer().getScheduler().runTaskLater(ArsenalPlugin.this, () -> held.forEach(chunk -> releaseChunk(world, chunk)), 16);
        }

        private void spawnMarker(Location location, String label, Material material, int ticks) {
            BlockDisplay light = location.getWorld().spawn(location, BlockDisplay.class, d -> {
                d.setBlock(material.createBlockData()); d.setGlowing(true); d.setPersistent(false);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setTransformation(new Transformation(new Vector3f(-.25f), new Quaternionf(), new Vector3f(.5f), new Quaternionf()));
            });
            TextDisplay text = location.getWorld().spawn(location.clone().add(0, 1, 0), TextDisplay.class, d -> {
                d.text(Component.text(label, NamedTextColor.RED)); d.setBillboard(Display.Billboard.CENTER); d.setGlowing(true); d.setPersistent(false);
            });
            getServer().getScheduler().runTaskLater(ArsenalPlugin.this, () -> { light.remove(); text.remove(); }, ticks);
        }

        private void spawnIllumination(Location flareLocation) {
            World world = flareLocation.getWorld();
            RayTraceResult ground = world.rayTraceBlocks(flareLocation, new Vector(0, -1, 0), 32,
                    FluidCollisionMode.ALWAYS, false);
            Location center = ground == null ? flareLocation.clone().subtract(0, 6, 0)
                    : ground.getHitPosition().toLocation(world).add(0, 6, 0);
            List<Block> lights = new ArrayList<>();
            int[][] offsets = {{0,0}, {8,0}, {-8,0}, {0,8}, {0,-8}};
            for (int[] offset : offsets) {
                Block block = world.getBlockAt(center.getBlockX() + offset[0], center.getBlockY(), center.getBlockZ() + offset[1]);
                if (acquireLight(block)) lights.add(block);
            }
            spawnMarker(flareLocation, "FLARE", Material.SEA_LANTERN, 600);
            getServer().getScheduler().runTaskLater(ArsenalPlugin.this, () -> {
                lights.forEach(ArsenalPlugin.this::releaseLight);
            }, 600);
        }

        private void disruptRedstone(Location location) {
            Map<Block, BlockData> changed = new HashMap<>();
            World world = location.getWorld();
            for (int x = -5; x <= 5; x++) for (int y = -5; y <= 5; y++) for (int z = -5; z <= 5; z++) {
                if (x*x + y*y + z*z > 25) continue;
                Block block = world.getBlockAt(location.getBlockX()+x, location.getBlockY()+y, location.getBlockZ()+z);
                BlockData before = block.getBlockData(); BlockData disabled = before.clone();
                if (disabled instanceof Powerable p && p.isPowered()) p.setPowered(false);
                else if (disabled instanceof AnaloguePowerable a && a.getPower() > 0) a.setPower(0);
                else continue;
                changed.put(block, before); block.setBlockData(disabled, false);
            }
            world.spawnParticle(Particle.ELECTRIC_SPARK, location, 80, 3, 3, 3, .1);
            getServer().getScheduler().runTaskLater(ArsenalPlugin.this, () -> changed.forEach((b, data) -> {
                if (b.getType() == data.getMaterial()) b.setBlockData(data, true);
            }), 100);
        }

        private void spawnCluster(Player shooter, Location location) {
            World world = location.getWorld();
            float submunitionPower = munition == MunitionType.ROCKET_SALVO ? 2.4f : 1.8f;
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4; int delay = i * 2;
                Location start = location.clone().add(Math.cos(angle) * 5, 0, Math.sin(angle) * 5);
                getServer().getScheduler().runTaskLater(ArsenalPlugin.this, () -> {
                    RayTraceResult ground = world.rayTraceBlocks(start, new Vector(0, -1, 0), 32, FluidCollisionMode.ALWAYS, false);
                    Location target = ground == null ? start : ground.getHitPosition().toLocation(world);
                    world.spawnParticle(Particle.FLAME, start, 8, .2, 1, .2, .03);
                    world.createExplosion(shooter, target, scaledImpact(submunitionPower), false, settings.blockDamage, false);
                }, delay);
            }
        }
        private boolean ticketSegment(World world, Vector segment) {
            Set<ChunkPos> needed = new HashSet<>();
            int samples = Math.max(1, (int) Math.ceil(segment.length() / 8));
            for (int i = 0; i <= samples; i++) {
                Location point = position.clone().add(segment.clone().multiply((double) i / samples));
                // Include every chunk touched by the projectile's collision radius.
                for (double ox : new double[]{-.225, .225}) for (double oz : new double[]{-.225, .225})
                    needed.add(new ChunkPos(world.getUID(), ((int) Math.floor(point.getX() + ox)) >> 4,
                            ((int) Math.floor(point.getZ() + oz)) >> 4));
            }
            for (ChunkPos chunk : needed) if (!world.isChunkGenerated(chunk.x, chunk.z)) return false;
            for (ChunkPos chunk : needed) if (ticketedChunks.add(chunk)) claimChunk(world, chunk);
            for (Iterator<ChunkPos> iterator = ticketedChunks.iterator(); iterator.hasNext();) {
                ChunkPos old = iterator.next();
                if (needed.contains(old)) continue;
                releaseChunk(world, old);
                iterator.remove();
            }
            return true;
        }
        void cleanup() {
            flight.expire();
            World world = position.getWorld();
            for (ChunkPos chunk : ticketedChunks) releaseChunk(world, chunk);
            ticketedChunks.clear();
            if (display.isValid()) display.remove();
        }
    }

    private void claimChunk(World world, ChunkPos chunk) {
        int refs = chunkReferences.merge(chunk, 1, Integer::sum);
        if (refs == 1) world.getChunkAt(chunk.x, chunk.z).addPluginChunkTicket(this);
    }

    private void releaseChunk(World world, ChunkPos chunk) {
        Integer refs = chunkReferences.get(chunk);
        if (refs == null) return;
        if (refs <= 1) {
            chunkReferences.remove(chunk);
            if (world.isChunkLoaded(chunk.x, chunk.z)) world.getChunkAt(chunk.x, chunk.z).removePluginChunkTicket(this);
        } else chunkReferences.put(chunk, refs - 1);
    }

    private record ChunkPos(UUID world, int x, int z) {}

    @EventHandler public void quit(PlayerQuitEvent event) { cleanupOwner(event.getPlayer().getUniqueId()); mortarTargets.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void death(PlayerDeathEvent event) { cleanupOwner(event.getEntity().getUniqueId()); }
    @EventHandler public void changeWorld(PlayerChangedWorldEvent event) { cleanupOwner(event.getPlayer().getUniqueId()); mortarTargets.remove(event.getPlayer().getUniqueId()); }
    private void cleanupOwner(UUID owner) {
        shots.values().removeIf(shot -> { if (!shot.owner.equals(owner)) return false; shot.cleanup(); return true; });
        cooldowns.remove(owner);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void unload(WorldUnloadEvent event) {
        shots.values().removeIf(shot -> { if (!shot.position.getWorld().equals(event.getWorld())) return false; shot.cleanup(); return true; });
    }
    @EventHandler public void loadEntities(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) if (entity instanceof BlockDisplay display) cleanupStale(display);
    }
    private void cleanupStale(BlockDisplay display) {
        if (display.getPersistentDataContainer().has(projectileKey, PersistentDataType.BYTE)
                && !shots.containsKey(display.getUniqueId())) display.remove();
    }

    private List<MunitionType> munitionsFor(String scope) {
        WeaponType weapon = WeaponType.parse(scope);
        if (weapon != null) return Arrays.stream(MunitionType.values())
                .filter(munition -> munition.compatible.contains(weapon)).toList();
        AmmoCategory category = AmmoCategory.parse(scope);
        if (category != null) return Arrays.stream(MunitionType.values())
                .filter(munition -> munition.category() == category).toList();
        return List.of();
    }

    private List<MunitionType> allMunitions() {
        return Arrays.asList(MunitionType.values());
    }

    private List<String> ammoScopes() {
        List<String> scopes = new ArrayList<>();
        for (AmmoCategory category : AmmoCategory.values()) scopes.add(category.id);
        for (WeaponType weapon : WeaponType.values()) scopes.add(weapon.id);
        return scopes;
    }

    private void sendAmmoList(CommandSender sender, String scope) {
        List<MunitionType> munitions = scope == null ? allMunitions() : munitionsFor(scope);
        if (scope != null && munitions.isEmpty()) {
            sender.sendMessage("Unknown ammo category or weapon. Use tab completion to choose one.");
            return;
        }
        if (scope == null) {
            sender.sendMessage("Ammo categories: launcher, shotgun, rifle, sniper, mortar, artillery.");
            for (AmmoCategory category : AmmoCategory.values()) sendAmmoList(sender, category.id);
            return;
        }
        WeaponType weapon = WeaponType.parse(scope);
        AmmoCategory category = AmmoCategory.parse(scope);
        String title = weapon != null ? weapon.title + " ammunition" : category.title + " ammunition";
        sender.sendMessage(title + ": " + munitions.stream().map(m -> m.id).reduce((a, b) -> a + ", " + b).orElse("none"));
    }

    private ItemStack handbook() {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.setTitle("Arsenal Handbook");
        meta.setAuthor("BKK Arsenal");
        meta.setGeneration(BookMeta.Generation.ORIGINAL);
        addHandbookPages(meta, "BKK ARSENAL\n\nA field guide to the unified weapons system. Every weapon uses a named blaze rod; every custom round uses a firework star. Keep a compatible round in your offhand to select it. Without a selected round, the weapon uses its standard payload.");
        addHandbookPages(meta, "CONTROLS\n\nDirect weapons: right-click to fire where you look.\n\nIndirect weapons: right-click a block or entity to lock a target, then left-click to fire the calculated arc.\n\nThe target marker is only a lock indicator; the shell remains a server-side projectile until impact.");
        StringBuilder weapons = new StringBuilder("WEAPON PLATFORMS\n\n");
        for (WeaponType weapon : WeaponType.values()) {
            weapons.append(weapon.title).append(" — ").append(weapon.indirect() ? "target-lock arc" : "direct fire").append("\n");
        }
        addHandbookPages(meta, weapons.toString());
        for (AmmoCategory category : AmmoCategory.values()) {
            StringBuilder page = new StringBuilder(category.title.toUpperCase(Locale.ROOT)).append(" AMMUNITION\n\n");
            for (MunitionType munition : munitionsFor(category.id)) {
                page.append(munition.id).append(" — ").append(munition.title).append("\n");
            }
            page.append("\nCommand: /arsenal ammo <player> ").append(category.id).append(" <munition> [amount]");
            addHandbookPages(meta, page.toString());
        }
        addHandbookPages(meta, "AMMO LOOKUP\n\n/arsenal ammo <player> list\nShows every category.\n\n/arsenal ammo <player> list <category-or-weapon>\nShows only rounds for one platform.\n\n/arsenal ammo <player> <weapon> <munition> [amount]\nThe weapon-qualified form prevents incompatible ammunition from being issued.");
        addHandbookPages(meta, "FIELD NOTES\n\nSmoke creates a drifting volumetric cloud. Illumination rounds create temporary lights. Incendiaries keep full entity damage while leaving a smaller crater and a larger fire patch. Penetrators, airbursts, cluster rounds and THE DEPTH CHARGE have distinct effects. Projectiles have no artificial distance limit; only generated terrain is traversed.");
        item.setItemMeta(meta);
        return item;
    }

    private void addHandbookPages(BookMeta meta, String text) {
        String remaining = text;
        while (remaining.length() > 240) {
            int split = remaining.lastIndexOf('\n', 240);
            if (split < 80) split = 240;
            meta.addPage(remaining.substring(0, split));
            remaining = remaining.substring(split).stripLeading();
        }
        if (!remaining.isEmpty()) meta.addPage(remaining);
    }

    private boolean giveHandbook(CommandSender sender, String[] args) {
        if (args.length > 2) { sender.sendMessage("Usage: /arsenal handbook [player]"); return true; }
        Player target;
        if (args.length == 2) {
            if (!sender.hasPermission("arsenal.admin")) { sender.sendMessage("Only admins can give the handbook to another player."); return true; }
            target = getServer().getPlayerExact(args[1]);
        } else target = sender instanceof Player player ? player : null;
        if (target == null) { sender.sendMessage("You must be a player or specify an online player."); return true; }
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(handbook());
        if (!leftover.isEmpty()) { sender.sendMessage("The player does not have enough inventory space."); return true; }
        sender.sendMessage("Gave the Arsenal Handbook to " + target.getName() + ".");
        return true;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("handbook")) {
            if (!sender.hasPermission("arsenal.handbook") && !sender.hasPermission("arsenal.admin")) {
                sender.sendMessage("You do not have permission."); return true;
            }
            return giveHandbook(sender, args);
        }
        if (!sender.hasPermission("arsenal.admin")) { sender.sendMessage("You do not have permission."); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig(); settings = readSettings(); sender.sendMessage("Arsenal configuration reloaded."); return true;
        }
        if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("cooldown")) {
            WeaponType type = args.length == 3 ? WeaponType.parse(args[1]) : WeaponType.LAUNCHER;
            if (type == null) { sender.sendMessage("Weapon must be launcher, shotgun, rifle, sniper, or mortar."); return true; }
            double seconds;
            try { seconds = Double.parseDouble(args[args.length - 1]); }
            catch (NumberFormatException exception) { seconds = -1; }
            if (!Double.isFinite(seconds) || seconds < .05 || seconds > 60) {
                sender.sendMessage("Cooldown must be between 0.05 and 60 seconds."); return true;
            }
            int ticks = (int) Math.round(seconds * 20);
            getConfig().set("weapons." + type.id.replace('_', '-') + ".cooldown-ticks", ticks);
            saveConfig();
            settings = readSettings();
            int actual = settings.weapons.get(type).cooldown;
            sender.sendMessage(type.title + " cooldown set to " + actual / 20.0
                    + " seconds (" + actual + " ticks). This takes effect immediately.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("BKK Arsenal " + getPluginMeta().getVersion() + ": " + shots.size()
                    + " active; block damage " + settings.blockDamage + "; ammo " + settings.ammoPerShot + " " + settings.ammo + ".");
            for (WeaponType type : WeaponType.values()) {
                WeaponSettings w = settings.weapons.get(type);
                sender.sendMessage(type.id + ": speed " + w.speed + ", gravity " + w.gravity + ", power " + w.power
                        + ", cooldown " + w.cooldown + "t, projectiles " + w.projectiles + ", no range limit.");
            }
            return true;
        }
        if (args.length >= 3 && args.length <= 5 && args[0].equalsIgnoreCase("ammo")) {
            Player target = getServer().getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("That player must be online."); return true; }
            if (args[2].equalsIgnoreCase("list")) {
                if (args.length > 4) { sender.sendMessage("Usage: /arsenal ammo <player> list [category-or-weapon]"); return true; }
                sendAmmoList(sender, args.length == 4 ? args[3] : null);
                return true;
            }
            WeaponType scopeWeapon = WeaponType.parse(args[2]);
            AmmoCategory scopeCategory = AmmoCategory.parse(args[2]);
            if (args.length == 3 && (scopeWeapon != null || scopeCategory != null)) {
                sendAmmoList(sender, args[2]);
                return true;
            }
            String munitionId = scopeWeapon != null || scopeCategory != null ? args[3] : args[2];
            MunitionType grenade = MunitionType.parse(munitionId);
            if (grenade == null) { sender.sendMessage("Unknown munition. Use /arsenal ammo " + target.getName() + " list."); return true; }
            if (scopeWeapon != null && !grenade.compatible.contains(scopeWeapon)) {
                sender.sendMessage(grenade.title + " is not compatible with the " + scopeWeapon.title + "."); return true;
            }
            if (scopeCategory != null && grenade.category() != scopeCategory) {
                sender.sendMessage(grenade.title + " is not in the " + scopeCategory.title + " category."); return true;
            }
            int amount = 1;
            int amountIndex = scopeWeapon != null || scopeCategory != null ? 4 : 3;
            if (args.length > amountIndex) try { amount = Integer.parseInt(args[amountIndex]); } catch (NumberFormatException exception) { amount = 0; }
            if (args.length > amountIndex + 1) { sender.sendMessage("Too many arguments. Use /arsenal ammo <player> <munition> [amount] or /arsenal ammo <player> <weapon> <munition> [amount]."); return true; }
            if (amount < 1 || amount > 64) { sender.sendMessage("Amount must be 1-64."); return true; }
            Map<Integer, ItemStack> leftover = target.getInventory().addItem(munition(grenade, amount));
            if (!leftover.isEmpty()) { sender.sendMessage("The player does not have enough inventory space."); return true; }
            sender.sendMessage("Gave " + amount + " [" + grenade.category().title + "] " + grenade.title + "(s) to " + target.getName() + ".");
            target.sendMessage(Component.text("Put the round in your offhand while holding its matching weapon.", NamedTextColor.GOLD));
            return true;
        }
        Player target;
        WeaponType type = WeaponType.LAUNCHER;
        int count = 1;
        if (args.length == 0 && sender instanceof Player player) target = player;
        else if (args.length >= 2 && args.length <= 4 && args[0].equalsIgnoreCase("give")) {
            target = getServer().getPlayerExact(args[1]);
            if (args.length >= 3) {
                WeaponType parsed = WeaponType.parse(args[2]);
                if (parsed != null) type = parsed;
                else if (args.length == 3) try { count = Integer.parseInt(args[2]); } catch (NumberFormatException exception) { count = 0; }
                else type = null;
            }
            if (args.length == 4) try { count = Integer.parseInt(args[3]); } catch (NumberFormatException exception) { count = 0; }
        } else return false;
        if (target == null) { sender.sendMessage("That player must be online."); return true; }
        if (type == null) { sender.sendMessage("Weapon must be launcher, shotgun, rifle, sniper, or mortar."); return true; }
        if (count < 1 || count > 16) { sender.sendMessage("Amount must be 1-16."); return true; }
        int freeSlots = 0;
        for (ItemStack item : target.getInventory().getStorageContents()) if (item == null || item.getType().isAir()) freeSlots++;
        if (freeSlots < count) { sender.sendMessage("The player needs " + count + " empty inventory slots."); return true; }
        for (int i = 0; i < count; i++) target.getInventory().addItem(gun(type));
        sender.sendMessage("Gave " + count + " " + type.title + "(s) to " + target.getName() + ".");
        target.sendMessage(Component.text(type.title + " ready! Right-click to fire; hold compatible ammunition in your offhand.", NamedTextColor.GOLD));
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("arsenal.admin")) return List.of();
        List<String> choices;
        if (args.length == 1) choices = List.of("give", "ammo", "cooldown", "reload", "status", "handbook");
        else if (args.length == 2 && args[0].equalsIgnoreCase("give")) choices = getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        else if (args.length == 2 && args[0].equalsIgnoreCase("ammo")) choices = getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        else if (args.length == 2 && args[0].equalsIgnoreCase("cooldown")) choices = Arrays.stream(WeaponType.values()).map(t -> t.id).toList();
        else if (args.length == 2 && args[0].equalsIgnoreCase("handbook")) choices = getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        else if (args.length == 3 && args[0].equalsIgnoreCase("give")) choices = Arrays.stream(WeaponType.values()).map(t -> t.id).toList();
        else if (args.length == 3 && args[0].equalsIgnoreCase("ammo")) {
            choices = new ArrayList<>(); choices.add("list"); choices.addAll(ammoScopes()); choices.addAll(Arrays.stream(MunitionType.values()).map(m -> m.id).toList());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("ammo") && args[2].equalsIgnoreCase("list")) {
            choices = ammoScopes();
        } else if (args.length == 4 && args[0].equalsIgnoreCase("ammo")) {
            List<MunitionType> filtered = munitionsFor(args[2]);
            choices = filtered.isEmpty() ? List.of() : filtered.stream().map(m -> m.id).toList();
        } else if (args.length == 5 && args[0].equalsIgnoreCase("ammo")) {
            choices = List.of("1", "4", "16", "64");
        } else choices = List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
    private record WeaponSettings(double speed, double gravity, float power, int cooldown,
                                  int projectiles, double spread) {}
    private record Settings(Material ammo, int ammoPerShot, boolean blockDamage, boolean fire,
                            int maxActive, EnumMap<WeaponType, WeaponSettings> weapons,
                            double penetrationDepth, float penetratingPower,
                            double airburstHeight, float airburstPower,
                            double mortarTargetDistance, double mortarArcClearance,
                            double smokeRadius, double smokeHeight, int smokeDensity, int smokeDuration,
                            int incendiaryFireRadius, int incendiaryTerrainRadius) {}
}
