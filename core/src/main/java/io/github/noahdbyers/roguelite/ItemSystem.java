package io.github.noahdbyers.roguelite;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Random;

/**
 * Runtime state for item effects.
 *
 * This is intentionally lightweight: it owns counters (hit count, stacks) and
 * exposes small hooks that GameWorld calls at key moments.
 */
public class ItemSystem {
    private final EnumMap<ItemId, Boolean> owned = new EnumMap<>(ItemId.class);
    private final ArrayList<ItemId> ownedList = new ArrayList<>();

    // Stacks / counters
    private int scrollStacks = 0;
    private int hitCounter = 0;
    private boolean twinSunsToggle = false;

    // Tear of the Veiled Saint
    private int soulHealAccum = 0;

    // Jester's Dice: roll a new minor buff each wave (non-stacking)
    private float diceDamageMult = 1f;

    // Cached multipliers
    private float soulMult = 1f;
    private float damageTakenMult = 1f;

    public ItemSystem() {
        for (ItemId id : ItemId.values()) owned.put(id, false);
        recalcCaches();
    }

    public ArrayList<ItemId> getOwnedList() {
        return ownedList;
    }

    public boolean has(ItemId id) {
        if (id == null) return false;
        Boolean b = owned.get(id);
        return b != null && b;
    }

    /**
     * Attempt to add an item. Returns true if actually acquired.
     * Unique items that you already own return false.
     */
    public boolean addItem(ItemId id, GameWorld world) {
        if (id == null) return false;
        ItemDefinition def = ItemRegistry.get(id);
        boolean unique = def == null || def.unique;

        if (unique && has(id)) return false;

        owned.put(id, true);
        if (!ownedList.contains(id)) ownedList.add(id);

        // Immediate one-time stat changes
        Player p = (world != null) ? world.getPlayer() : null;
        if (p != null) {
            if (id == ItemId.MANTLE_OF_THE_SWIFT_HUNTRESS) {
                p.setSpeed(p.getSpeed() * 1.15f);
            } else if (id == ItemId.CROWN_OF_THE_FALLEN_KING) {
                int add = Math.max(1, Math.round(p.getMaxHealth() * 0.10f));
                p.increaseMaxHealth(add);
            }
        }

        recalcCaches();
        return true;
    }

    private void recalcCaches() {
        soulMult = 1f;
        if (has(ItemId.HOLY_PENDANT)) soulMult *= 1.5f;

        damageTakenMult = 1f;
        if (has(ItemId.INCENSE_OF_THE_QUIET_SPIRIT)) damageTakenMult *= 0.9f;
    }

    public float getSoulMultiplier() {
        return soulMult;
    }

    public float getDamageTakenMultiplier() {
        return damageTakenMult;
    }

    /** Called when a wave ends (room cleared). */
    public void onWaveCleared(GameWorld world) {
        if (world == null) return;

        Random rng = world.getRng();
        Player p = world.getPlayer();

        // Banana deterioration
        if (has(ItemId.BANANA)) {
            if (rng.nextInt(100) == 0) {
                // Deteriorates: remove the banana
                owned.put(ItemId.BANANA, false);
                ownedList.remove(ItemId.BANANA);
                if (world != null) world.showToast("Banana deteriorated...", 2.0f);
                recalcCaches();
            }
        }

        if(has(ItemId.TRUE_GODS_BLESSING)) {

        }
        // Scroll stacking buff
        if (has(ItemId.SCROLL_OF_THE_FALLEN_HEROES)) {
            scrollStacks = Math.min(scrollStacks + 1, 25);
            if (world != null) world.showToast("Scroll awakens (stacks: " + scrollStacks + ")", 1.5f);
        }

        // Oracle pendant: bonus drops
        if (has(ItemId.PENDANT_OF_THE_LAST_TRUE_ORACLE)) {
            int drops = 3;
            for (int i = 0; i < drops; i++) {
                world.spawnRandomSoulDrop(1);
            }
        }

        // Prayer Stone: between-waves heal
        if (has(ItemId.PRAYER_STONE) && p != null) {
            p.heal(1);
        }

        // Jester's Dice: reroll minor buff each wave
        if (has(ItemId.JESTERS_DICE)) {
            int r = rng.nextInt(3);
            if (r == 0) {
                diceDamageMult = 1.08f;
                if (world != null) world.showToast("Jester’s Dice: +8% damage", 1.5f);
            } else if (r == 1) {
                // speed buff is applied directly so it feels immediate
                if (p != null) p.setSpeed(p.getSpeed() * 1.05f);
                diceDamageMult = 1f;
                if (world != null) world.showToast("Jester’s Dice: +5% speed", 1.5f);
            } else {
                diceDamageMult = 1f;
                if (p != null) p.heal(1);
                if (world != null) world.showToast("Jester’s Dice: +1 heal", 1.5f);
            }
        } else {
            diceDamageMult = 1f;
        }
    }

    /** Convert a base soul pickup into the final amount gained. */
    public int modifySouls(int base) {
        if (base <= 0) return 0;
        return Math.max(1, Math.round(base * soulMult));
    }

    /** Called after souls are picked up (final amount). */
    public void onSoulsPicked(GameWorld world, int gained) {
        if (world == null) return;
        Player p = world.getPlayer();
        if (p == null) return;

        if (has(ItemId.TEAR_OF_THE_VEILED_SAINT)) {
            soulHealAccum += Math.max(0, gained);
            while (soulHealAccum >= 5) {
                soulHealAccum -= 5;
                p.heal(1);
            }
        }
    }

    /** Decide the main damage type for this attack and advance any toggles. */
    public DamageType nextAttackDamageType() {
        // Conversions override alternation
        if (has(ItemId.TRUE_GODS_BLESSING)) {
            return DamageType.HOLY;
        }
        if (has(ItemId.HOLY_PENDANT)){
            return DamageType.HOLY;
        }

        if (has(ItemId.GLYPH_OF_THE_TWIN_SUNS)) {
            twinSunsToggle = !twinSunsToggle;
            return twinSunsToggle ? DamageType.FIRE : DamageType.RADIANT;
        }

        return DamageType.PHYSICAL;
    }

    public int computeMainDamage(int baseDamage) {
        if (baseDamage <= 0) baseDamage = 1;

        float mult = 1f;

        if (has(ItemId.BANANA)) mult *= 10f;
        if (has(ItemId.TABLET_OF_THE_NINE_TRIALS)) mult *= 1.10f;
        if (has(ItemId.TRUE_GODS_BLESSING)) mult *= 1.25f;

        if (has(ItemId.SCROLL_OF_THE_FALLEN_HEROES)) {
            // 4% per wave stack; cap by scrollStacks
            mult *= (1f + 0.04f * scrollStacks);
        }

        mult *= diceDamageMult;

        return Math.max(1, Math.round(baseDamage * mult));
    }

    public boolean rollCrit(Random rng) {
        if (rng == null) return false;
        // Baseline crit chance; tune later
        float critChance = 0.08f;
        return rng.nextFloat() < critChance;
    }

    public int applyCritMultiplier(int damage) {
        return Math.max(1, Math.round(damage * 1.5f));
    }

    /** Called when an enemy is hit (on-hit effects). */
    public void onHitEnemy(GameWorld world, Enemy enemy, boolean wasCrit, int baseWeaponDamage) {
        if (world == null || enemy == null) return;
        Random rng = world.getRng();

        hitCounter++;

        // Tablet: every 9th hit bonus true damage
        if (has(ItemId.TABLET_OF_THE_NINE_TRIALS) && (hitCounter % 9 == 0)) {
            int bonus = Math.max(2, baseWeaponDamage);
            enemy.takeDamage(bonus, DamageType.TRUE);
            world.spawnDamagePopup(enemy, bonus);
        }

        // Spearhead: crit -> lightning
        if (wasCrit && has(ItemId.SPEARHEAD_OF_THE_SKYBREAKER)) {
            procLightning(world, enemy);
        }

        // Ember: ignite chance
        if (has(ItemId.EMBER_OF_THE_FLAMING_HERO)) {
            if (rng.nextFloat() < 0.12f) {
                enemy.applyBurn(1, 2.0f, 0.5f, DamageType.HOLY);
            }
        }

        // Blessings: bonus element damage
        if (has(ItemId.MOON_GODS_BLESSING)) {
            int bonus = Math.max(1, Math.round(baseWeaponDamage * 0.35f));
            enemy.takeDamage(bonus, DamageType.DARK);
        }
        if (has(ItemId.SUN_GODS_BLESSING)) {
            int bonus = Math.max(1, Math.round(baseWeaponDamage * 0.35f));
            enemy.takeDamage(bonus, DamageType.FIRE);
        }

        if(has(ItemId.TRUE_GODS_BLESSING)) {
            int bonus = 1000;
            enemy.takeDamage(bonus, DamageType.HOLY);
        }
    }

    private void procLightning(GameWorld world, Enemy primary) {
        if (world == null || primary == null) return;

        // Tiny lightning: hit nearby enemies for small damage
        float px = primary.getX() + primary.getWidth() * 0.5f;
        float py = primary.getY() + primary.getHeight() * 0.5f;

        int bolts = 2;
        float radius = 90f;
        int dmg = 1;

        for (int i = 0; i < bolts; i++) {
            Enemy e = world.findNearestEnemy(px, py, radius, primary);
            if (e == null) break;
            e.takeDamage(dmg, DamageType.LIGHTNING);
            world.spawnDamagePopup(e, dmg);
        }
    }
}
