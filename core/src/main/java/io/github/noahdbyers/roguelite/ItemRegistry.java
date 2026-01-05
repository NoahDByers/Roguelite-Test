package io.github.noahdbyers.roguelite;

import java.util.EnumMap;
import java.util.Random;

/**
 * Central catalog + random roll helper.
 */
public final class ItemRegistry {
    private static final EnumMap<ItemId, ItemDefinition> defs = new EnumMap<>(ItemId.class);

    static {
        // NOTE: effect magnitudes are intentionally conservative; tune as you play.
        add(new ItemDefinition(
            ItemId.BANANA,
            "Banana",
            "It looks like a normal banana, but it resonates with great power.",
            "x10 damage. After each completed wave: 1/10 chance to deteriorate.",
            1,
            true
        ));
        add(new ItemDefinition(
            ItemId.HOLY_PENDANT,
            "Holy Pendant",
            "A charm once worn by wandering monks who walked between worlds. It reshapes your strikes into pure sanctified force, and souls gather to it like moths to a lantern.",
            "+50% souls collected. Converts all damage to Holy.",
            2,
            true
        ));
        add(new ItemDefinition(
            ItemId.CROWN_OF_THE_FALLEN_KING,
            "Crown of the Fallen King",
            "The last remnant of a ruler whose name was erased from history. Though his kingdom crumbled, his vitality lingers in the gilded thorns of his crown.",
            "+10% max HP.",
            3,
            true
        ));
        add(new ItemDefinition(
            ItemId.MANTLE_OF_THE_SWIFT_HUNTRESS,
            "Mantle of the Swift Huntress",
            "Woven from the wind-threads of a forgotten goddess, this cloak carries the memory of a hunt that never ended. Those who wear it move as though chased by destiny itself.",
            "+15% movement speed.",
            4,
            true
        ));
        add(new ItemDefinition(
            ItemId.SPEARHEAD_OF_THE_SKYBREAKER,
            "Spearhead of the Skybreaker",
            "A shard of the weapon said to have pierced the heavens. Even in its broken state, it calls lightning to punish those struck by its bearer.",
            "Critical hits call down tiny lightning bolts.",
            5,
            true
        ));
        add(new ItemDefinition(
            ItemId.PENDANT_OF_THE_LAST_TRUE_ORACLE,
            "Pendant of the Last True Oracle",
            "The final oracle foresaw her own silence and sealed her last visions within this pendant. It reveals what others overlook — treasures hidden between moments.",
            "Reveals hidden drops at the end of each wave.",
            6,
            true
        ));
        add(new ItemDefinition(
            ItemId.SCROLL_OF_THE_FALLEN_HEROES,
            "Scroll of the Fallen Heroes",
            "Ink made from the ashes of forgotten champions fills this scroll. Each wave of battle awakens another echo of their strength.",
            "Gain a stacking damage buff each wave.",
            7,
            true
        ));
        add(new ItemDefinition(
            ItemId.TABLET_OF_THE_NINE_TRIALS,
            "Tablet of the Nine Trials",
            "Carved by a hero who survived nine impossible ordeals. The tablet hums with their resolve, striking hardest when the rhythm of battle aligns with its sacred count.",
            "+10% damage. Every 9th hit deals bonus True damage.",
            8,
            true
        ));
        add(new ItemDefinition(
            ItemId.GLYPH_OF_THE_TWIN_SUNS,
            "Glyph of the Twin Suns",
            "A sigil representing the celestial twins who danced across the sky in ages past. Their opposing flames still flicker through every strike.",
            "Attacks alternate between Fire and Radiant damage.",
            9,
            true
        ));
        add(new ItemDefinition(
            ItemId.INK_OF_THE_FORGOTTEN_SCRIBE,
            "Ink of the Forgotten Scribe",
            "This ink was used to record the earliest runes — and the scribe’s devotion lingers. Any rune touched by it grows sharper, louder, more alive.",
            "Runes are stronger.",
            10,
            true
        ));
        add(new ItemDefinition(
            ItemId.ASTRAL_ANCHOR,
            "Astral Anchor",
            "A weight forged from starlight, paradoxically grounding its bearer. No force, mortal or cosmic, can push aside one who carries the Anchor.",
            "You cannot be knocked back.",
            11,
            true
        ));
        add(new ItemDefinition(
            ItemId.INCENSE_OF_THE_QUIET_SPIRIT,
            "Incense of the Quiet Spirit",
            "Burned in temples where even gods whispered, this incense calms the soul and hardens the mind. Its smoke softens the world’s cruelty.",
            "Reduce all damage taken by 10%.",
            12,
            true
        ));
        add(new ItemDefinition(
            ItemId.BLESSED_ASH,
            "Blessed Ash",
            "The remains of a sacred pyre that once purified an entire battlefield. Enemies who step near its lingering warmth falter, as though judged.",
            "Enemies sometimes spawn weakened.",
            13,
            true
        ));
        add(new ItemDefinition(
            ItemId.PRAYER_STONE,
            "Prayer Stone",
            "A smooth stone worn down by centuries of hopeful hands. It answers each completed battle with a gentle, restorative warmth.",
            "Heal a small amount between waves.",
            14,
            true
        ));
        add(new ItemDefinition(
            ItemId.JESTERS_DICE,
            "Jester’s Dice",
            "Said to belong to a trickster spirit who gambled with fate itself. Each wave, the dice roll — and fortune tilts in your favor, though never the same way twice.",
            "Each wave grants a random minor buff.",
            15,
            true
        ));
        add(new ItemDefinition(
            ItemId.EMBER_OF_THE_FLAMING_HERO,
            "Ember of the Flaming Hero",
            "A coal taken from the pyre of a hero who burned with righteous fury. It still sparks with holy flame, eager to leap onto your foes.",
            "Attacks occasionally ignite enemies with holy flame.",
            16,
            true
        ));
        add(new ItemDefinition(
            ItemId.TEAR_OF_THE_VEILED_SAINT,
            "Tear of the Veiled Saint",
            "A single, moonlit tear sealed in glass. It mends flesh each time greed pulls you toward the dead.",
            "Heal a small amount whenever you pick up souls.",
            17,
            true
        ));
        add(new ItemDefinition(
            ItemId.MOON_GODS_BLESSING,
            "Moon God’s Blessing",
            "A cold blessing that stains the edge of every strike.",
            "Deal bonus Dark damage.",
            18,
            true
        ));
        add(new ItemDefinition(
            ItemId.SUN_GODS_BLESSING,
            "Sun God’s Blessing",
            "A burning blessing that brightens the edge of every strike.",
            "Deal bonus Fire damage.",
            19,
            true
        ));
        add(new ItemDefinition(
            ItemId.TRUE_GODS_BLESSING,
            "True God’s Blessing",
            "The old light returns — not warm, but absolute.",
            "All damage is converted to Holy and increased by 25%.",
            20,
            true
        ));
    }

    private static void add(ItemDefinition d) {
        defs.put(d.id, d);
    }

    public static ItemDefinition get(ItemId id) {
        return defs.get(id);
    }

    /**
     * Roll a random item from the catalog.
     * If you want rarities later, this is the place to add weights.
     */
    public static ItemId rollRandom(Random rng) {
        ItemId[] all = ItemId.values();
        return all[rng.nextInt(all.length)];
    }
}
