package io.github.noahdbyers.roguelite;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

public class MapCreator {
    private static final int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3;
    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {1, -1, 0, 0};
    private static final int[] OPP = {DOWN, UP, RIGHT, LEFT}; // (kept; may be useful later)

    private final int W = 10;
    private final int H = 10;

    private final ArrayList<RoomTemplate> templates;
    private final int T;

    // compat[t][dir] = BitSet of templates allowed next to template t in direction dir
    private final BitSet[][] compat;

    // domain[y][x] = which templates are still possible for this cell
    private final BitSet[][] domain;

    private final Random rng = new Random();

    public MapCreator(ArrayList<RoomTemplate> templates) {
        this.templates = templates;
        this.T = templates.size();

        if (T <= 0) {
            throw new IllegalArgumentException("MapCreator: templates list is empty.");
        }

        this.compat = new BitSet[T][4];
        this.domain = new BitSet[H][W];

        buildCompatMasks();          // Step 2
        initDomainsAllPossible();    // Step 3
        applyBorderConstraintsClosedWorld(); // Optional initial prune
    }

    // ----------------------------
    // Step 2: Precompute compatibility masks
    // ----------------------------
    private void buildCompatMasks() {
        // Initialize all BitSets
        for (int t = 0; t < T; t++) {
            for (int dir = 0; dir < 4; dir++) {
                compat[t][dir] = new BitSet(T);
            }
        }

        // Fill them
        for (int t = 0; t < T; t++) {
            RoomTemplate a = templates.get(t); // FIX: use t, not T

            for (int n = 0; n < T; n++) {
                RoomTemplate b = templates.get(n);

                // If template b can be adjacent to a in a given dir, set bit n
                if (a.up == b.down)   compat[t][UP].set(n);
                if (a.down == b.up)   compat[t][DOWN].set(n);
                if (a.left == b.right)  compat[t][LEFT].set(n);
                if (a.right == b.left)  compat[t][RIGHT].set(n); // FIX: RIGHT not LEFT
            }
        }
    }

    // ----------------------------
    // Initialize every cell domain = "all templates allowed"
    // ----------------------------
    private void initDomainsAllPossible() {
        BitSet all = new BitSet(T);
        all.set(0, T);

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                domain[y][x] = (BitSet) all.clone();
            }
        }
    }

    // Prevent doors that lead off the map (closed borders)
    private void applyBorderConstraintsClosedWorld() {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {

                boolean needUpClosed = (y == H - 1);
                boolean needDownClosed = (y == 0);
                boolean needLeftClosed = (x == 0);
                boolean needRightClosed = (x == W - 1);

                BitSet d = domain[y][x];

                for (int t = d.nextSetBit(0); t >= 0; t = d.nextSetBit(t + 1)) {
                    RoomTemplate rt = templates.get(t);

                    if (needUpClosed && rt.up) { d.clear(t); continue; }
                    if (needDownClosed && rt.down) { d.clear(t); continue; }
                    if (needLeftClosed && rt.left) { d.clear(t); continue; }
                    if (needRightClosed && rt.right) { d.clear(t); continue; }
                }

                if (d.isEmpty()) {
                    throw new IllegalStateException(
                        "No templates fit border constraints at (" + x + ", " + y + ")"
                    );
                }
            }
        }
    }

    // ----------------------------
    // Main WFC generate
    // ----------------------------
    public int[][] generate() {
        // Re-apply border constraints (safe even if already applied)
        applyBorderConstraintsClosedWorld();

        // Initial propagation should be allowed to fail early
        if (!propagateAll()) {
            throw new IllegalStateException("WFC contradiction after initial border constraints.");
        }

        int[][] result = new int[H][W];

        while (true) {
            int[] cell = pickCellWithLowestEntropy();
            if (cell == null) break; // all cells collapsed

            int cx = cell[0];
            int cy = cell[1];

            collapseCellRandom(cx, cy);

            if (!propagateFrom(cx, cy)) {
                throw new IllegalStateException(
                    "WFC contradiction after collapsing (" + cx + "," + cy + ")"
                );
            }
        }

        // Fill result with the chosen single template index per cell
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                result[y][x] = singleIndex(domain[y][x]);
            }
        }

        return result;
    }

    // Pick a non-collapsed cell with lowest entropy (fewest possibilities)
    private int[] pickCellWithLowestEntropy() {
        int bestCount = Integer.MAX_VALUE;
        int bestX = -1, bestY = -1;

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int c = domain[y][x].cardinality();

                if (c == 0) {
                    // This is already a contradiction state.
                    throw new IllegalStateException("Domain empty at (" + x + "," + y + ")");
                }

                if (c <= 1) continue; // collapsed

                if (c < bestCount) {
                    bestCount = c;
                    bestX = x;
                    bestY = y;
                } else if (c == bestCount) {
                    // random tie-break
                    if (rng.nextBoolean()) {
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }

        return (bestX == -1) ? null : new int[]{bestX, bestY};
    }

    // Collapse a cell to a single random bit
    private void collapseCellRandom(int x, int y) {
        BitSet d = domain[y][x];
        int count = d.cardinality();
        if (count <= 1) return;

        int pick = rng.nextInt(count);

        int chosen = -1;
        int idx = 0;
        for (int t = d.nextSetBit(0); t >= 0; t = d.nextSetBit(t + 1)) {
            if (idx == pick) { chosen = t; break; }
            idx++;
        }

        if (chosen < 0) {
            throw new IllegalStateException("Failed to choose a template at (" + x + "," + y + ")");
        }

        d.clear();
        d.set(chosen);
    }

    // Propagate constraints starting from a cell
    private boolean propagateFrom(int startX, int startY) {
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{startX, startY});

        while (!q.isEmpty()) {
            int[] c = q.removeFirst();
            int x = c[0], y = c[1];

            BitSet here = domain[y][x];

            for (int dir = 0; dir < 4; dir++) {
                int nx = x + DX[dir];
                int ny = y + DY[dir];

                if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;

                BitSet neigh = domain[ny][nx];

                // Allowed neighbor templates given our current domain
                BitSet allowed = computeAllowedNeighbor(here, dir);

                // neigh &= allowed
                BitSet before = (BitSet) neigh.clone();
                neigh.and(allowed);

                if (neigh.isEmpty()) return false; // contradiction

                if (!neigh.equals(before)) {
                    q.add(new int[]{nx, ny});
                }
            }
        }

        return true;
    }

    // Compute a BitSet of all templates allowed in neighbor given hereDomain and direction
    private BitSet computeAllowedNeighbor(BitSet hereDomain, int dir) {
        BitSet allowed = new BitSet(T);
        for (int t = hereDomain.nextSetBit(0); t >= 0; t = hereDomain.nextSetBit(t + 1)) {
            allowed.or(compat[t][dir]);
        }
        return allowed;
    }

    // Propagate everything once (returns false instead of throwing)
    private boolean propagateAll() {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (!propagateFrom(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    // Get the single chosen template index
    private static int singleIndex(BitSet d) {
        int idx = d.nextSetBit(0);
        if (idx < 0 || d.cardinality() != 1) {
            throw new IllegalStateException("Cell not collapsed to single value.");
        }
        return idx;
    }

    // Debug helper
    public BitSet getCompat(int templateIndex, int dir) {
        return compat[templateIndex][dir];
    }
}
