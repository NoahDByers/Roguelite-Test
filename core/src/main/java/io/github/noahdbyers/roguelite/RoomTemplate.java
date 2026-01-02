package io.github.noahdbyers.roguelite;

public class RoomTemplate {
    // True means the side is OPEN / has a door connection
    public final boolean up, down, left, right;

    // 0=UP,1=DOWN,2=LEFT,3=RIGHT
    public final boolean[] sides;

    public RoomTemplate(boolean up, boolean down, boolean left, boolean right) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;

        this.sides = new boolean[]{ up, down, left, right };
    }

    /** Returns whether this template has an opening on a side. 0=UP,1=DOWN,2=LEFT,3=RIGHT */
    public boolean side(int dir) {
        if (dir < 0 || dir > 3) return false;
        return sides[dir];
    }

    /** Compatibility check: neighbors must agree on the shared edge. */
    public static boolean compatible(RoomTemplate a, RoomTemplate b, int dir) {
        switch (dir) {
            case 0: return a.side(0) == b.side(1); // a.UP matches b.DOWN
            case 1: return a.side(1) == b.side(0); // a.DOWN matches b.UP
            case 2: return a.side(2) == b.side(3); // a.LEFT matches b.RIGHT
            case 3: return a.side(3) == b.side(2); // a.RIGHT matches b.LEFT
            default: return false;
        }
    }

    public boolean openUp()    { return sides[0]; }
    public boolean openDown()  { return sides[1]; }
    public boolean openLeft()  { return sides[2]; }
    public boolean openRight() { return sides[3]; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomTemplate)) return false;
        RoomTemplate other = (RoomTemplate) o;
        return up == other.up && down == other.down && left == other.left && right == other.right;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + (up ? 1 : 0);
        h = 31 * h + (down ? 1 : 0);
        h = 31 * h + (left ? 1 : 0);
        h = 31 * h + (right ? 1 : 0);
        return h;
    }
}
