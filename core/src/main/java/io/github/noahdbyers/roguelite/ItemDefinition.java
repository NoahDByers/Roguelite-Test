package io.github.noahdbyers.roguelite;

/**
 * Static data describing an item: name, lore blurb, and effect summary.
 */
public class ItemDefinition {
    public final ItemId id;
    public final String name;
    public final int iconTileIndex;
    public final String lore;
    public final String effect;
    public final boolean unique;


    public ItemDefinition(ItemId id, String name, String lore, String effect, int iconTileIndex, boolean unique) {
        this.id = id;
        this.name = name;
        this.lore = lore;
        this.effect = effect;
        this.unique = unique;
        this.iconTileIndex = iconTileIndex - 1;
    }
}
