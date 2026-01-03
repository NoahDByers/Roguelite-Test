package io.github.noahdbyers.roguelite;

public class WFCCell {
    public boolean collapsed = false;
    public int chosenIndex = -1;

    public boolean[] possible;
    public WFCCell(int templateCount) {
        possible = new boolean[templateCount];
        for(int i = 0; i < templateCount; i++) possible[i] = true;
    }

    public int countPossible() {
        int c = 0;
        for(boolean b : possible) if (b) c++;
        return c;
    }
}
