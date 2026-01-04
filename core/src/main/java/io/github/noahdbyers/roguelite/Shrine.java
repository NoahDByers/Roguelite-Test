package io.github.noahdbyers.roguelite;

/**
 * Simple shop/shrine that offers a few upgrades for a soul cost.
 *
 * Notes:
 * - Kept your original constructor signature for compatibility.
 * - Added per-slot costs so the UI can display prices and GameWorld can charge souls.
 */
public class Shrine {
    public float x, y;
    public float w = 32f, h = 32f;

    public Upgrade[] stock = new Upgrade[3];

    /** Per-slot costs (same indices as stock). */
    private final int[] costs = new int[3];

    public Shrine(float x, float y, Upgrade[] stock) {
        this(x, y, stock, null);
    }

    public Shrine(float x, float y, Upgrade[] stock, int[] costs) {
        this.x = x;
        this.y = y;

        if (stock != null) {
            int n = Math.min(this.stock.length, stock.length);
            for (int i = 0; i < n; i++) this.stock[i] = stock[i];
        }

        // Default any missing costs to 0 (GameWorld may still override via costFor(...))
        for (int i = 0; i < this.costs.length; i++) this.costs[i] = 0;
        if (costs != null) {
            int n = Math.min(this.costs.length, costs.length);
            for (int i = 0; i < n; i++) this.costs[i] = Math.max(0, costs[i]);
        }
    }

    public int getCost(int index) {
        if (index < 0 || index >= costs.length) return 0;
        return costs[index];
    }

    public void setCost(int index, int cost) {
        if (index < 0 || index >= costs.length) return;
        costs[index] = Math.max(0, cost);
    }
}
