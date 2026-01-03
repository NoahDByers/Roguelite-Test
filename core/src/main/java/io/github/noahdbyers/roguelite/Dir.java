package io.github.noahdbyers.roguelite;

public enum Dir {
    UP(0, 1), DOWN(0, -1), LEFT(-1, 0), RIGHT(1,0);

    public final int dx, dy;
    Dir(int dx, int dy) {this.dx = dx; this.dy = dy;}

    public Dir opposite() {
        switch(this) {
            case UP : return DOWN;
            case DOWN : return UP;
            case LEFT : return RIGHT;
            case RIGHT : return LEFT;
        }
        return DOWN;
    }
}
