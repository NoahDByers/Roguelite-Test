package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.Random;

public class GameWorld {
    private final Room room;

    private SpriteBatch spriteBatch;
    private Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Bullet> bullets = new ArrayList<>();

    private boolean gameOver = false;
    private int enemiesKilled = 0;
    private int coins = 0;
    private int souls = 0;
    private int wave = 1;
    private boolean waveActive = false;

    private boolean choosingUpgrade = false;
    private final Upgrade[] offeredUpgrades = new Upgrade[3];

    // Aim point in WORLD coordinates (set from Main every frame)
    private float aimWorldX = 0f;
    private float aimWorldY = 0f;

    // Difficulty (optional; left in for future)
    private float spawnTimer = 0f;
    private float difficultyTimer = 0f;

    private final float startSpawnInterval = 1.0f;
    private final float startMinSpawnInterval = 0.5f;
    private final int startMaxEnemies = 20;

    private float spawnInterval = startSpawnInterval;
    private float minSpawnInterval = startMinSpawnInterval;
    private int maxEnemies = startMaxEnemies;

    // Fire rate
    private float attackCooldown = 0f;        // kept for compatibility (not used if weapon != null)
    private float attackCooldownTime = 0.25f; // kept for compatibility (not used if weapon != null)

    // Current Weapon
    private Weapon weapon;

    // Bullet tuning
    private float bulletSpeed = 240f;
    private float bulletSize = 8f;
    private int bulletDamage = 1;
    private final ArrayList<DamagePopup> damagePopups = new ArrayList<>();

    private final Texture cardTexture = new Texture("ui/upgrade_card.png");
    private final Random rng = new Random();

    private AudioManager audio;
    public GameWorld(Room room, Player player, SpriteBatch spriteBatch) {
        this.room = room;
        this.player = player;
        this.spriteBatch = spriteBatch;
        restart();
    }

    // -------------------- Getters --------------------
    public Room getRoom() { return room; }
    public Player getPlayer() { return player; }
    public ArrayList<Enemy> getEnemies() { return enemies; }
    public ArrayList<Bullet> getBullets() { return bullets; }

    public boolean isGameOver() { return gameOver; }
    public boolean isChoosingUpgrade() { return choosingUpgrade; }

    public int getEnemiesKilled() { return enemiesKilled; }
    public int getWave() { return wave; }

    public Upgrade[] getOfferedUpgrades() { return offeredUpgrades; }
    public ArrayList<DamagePopup> getDamagePopups() { return damagePopups; }
    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
        if (this.weapon != null) this.weapon.setAttackCooldown(0f);
    }

    public void setAudio(AudioManager audio) { this.audio = audio; }

    // -------------------- Update loop --------------------
    public void update(float delta) {
        // Restart
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restart();
            return;
        }

        if (gameOver) return;

        // Pause world while choosing upgrades
        if (choosingUpgrade) {
            handleUpgradeInput();
            return;
        }

        // Safety
        if (player == null) {
            ensurePlayer();
            if (player == null) return;
        }

        // Update player
        player.update(room, room.getTileSize());
        player.updateTimers(delta);

        // Update enemies
        for (Enemy e : enemies) {
            if (e == null) continue;
            e.update(player, room, room.getTileSize());
        }

        // Combat
        handlePlayerEnemyContact();
        tryShootTowardsCursor(delta); // ✅ cursor aim
        updateBullets();

        for (int i = damagePopups.size() - 1; i >= 0; i--) {
            DamagePopup p = damagePopups.get(i);
            p.update(delta);
            if (p.isDead()) damagePopups.remove(i);
        }

        // Wave management
        if (!waveActive) startWave();

        if (waveActive && enemies.isEmpty()) {
            waveActive = false;
            wave++;
            beginUpgradeChoice();
        }

        // Game over
        if (player.getHealth() <= 0) {
            gameOver = true;
        }
    }

    // -------------------- Restart --------------------
    public void restart() {
        gameOver = false;

        ensurePlayerFresh();

        enemies.clear();
        bullets.clear();

        // Reset combat tuning
        attackCooldown = 0f;
        attackCooldownTime = 0.25f;

        if (weapon != null) weapon.setAttackCooldown(0f);

        bulletSpeed = 240f;
        bulletSize = 8f;
        bulletDamage = 1;

        // Difficulty reset
        spawnTimer = 0f;
        difficultyTimer = 0f;
        spawnInterval = startSpawnInterval;
        minSpawnInterval = startMinSpawnInterval;
        maxEnemies = startMaxEnemies;

        // Run stats
        enemiesKilled = 0;
        wave = 1;
        waveActive = false;

        // Upgrades
        choosingUpgrade = false;
        clearOfferedUpgrades();
    }

    private void ensurePlayer() {
        if (player != null) return;
        player = new Player(60, 60, 140f, 24f, 24f);
    }

    private void ensurePlayerFresh() {
        if (player == null) {
            ensurePlayer();
            return;
        }
        float w = player.getWidth();
        float h = player.getHeight();
        float spd = player.getSpeed();
        player = new Player(60, 60, spd, w, h);
    }

    // -------------------- Waves / spawning --------------------
    private void startWave() {
        enemies.clear();

        int toSpawn = 10 + wave;
        float baseSpeed = 60f + wave * 8f;

        for (int i = 0; i < toSpawn; i++) {
            spawnZombieWithSpeed(baseSpeed);
        }

        waveActive = true;
    }

    private void spawnZombieWithSpeed(float speed) {
        if (enemies.size() >= maxEnemies) return;
        if (player == null) ensurePlayer();
        if (room == null) return;

        int tileSize = room.getTileSize();
        float roomPixelW = room.getRoomWidth() * tileSize;
        float roomPixelH = room.getRoomHeight() * tileSize;

        float size = 28f;

        for (int tries = 0; tries < 200; tries++) {
            float x = rng.nextFloat() * (roomPixelW - size);
            float y = rng.nextFloat() * (roomPixelH - size);

            float px = player.getX() + player.getWidth() / 2f;
            float py = player.getY() + player.getHeight() / 2f;
            float ex = x + size / 2f;
            float ey = y + size / 2f;

            float dx = ex - px;
            float dy = ey - py;
            float minDist = 120f;
            if (dx * dx + dy * dy < minDist * minDist) continue;

            if (rectHitsWall(x, y, size, size)) continue;

            enemies.add(new Zombie(x, y, speed, size, 3));
            return;
        }

        float[] open = findFirstOpenSpot(size, 120f);
        if (open != null) {
            enemies.add(new Zombie(open[0], open[1], speed, size, 3));
        }
    }

    // -------------------- Combat (Cursor Aim) --------------------
    private void tryShootTowardsCursor(float delta) {
        if (weapon == null || player == null || room == null) return;

        weapon.setAttackCooldown(weapon.getAttackCooldown() - delta);
        if (weapon.getAttackCooldown() > 0f) return;

        float[] book = getBookWorldPos(32f); // distance from player
        float sx = book[0];
        float sy = book[1];

        float dirX = aimWorldX - sx;
        float dirY = aimWorldY - sy;

        bullets.add(new Bullet(sx, sy, dirX, dirY, bulletSpeed, bulletSize));

        weapon.setAttackCooldown(weapon.getAttackCooldownTime());
    }


    private float[] getBookSpawnPointTowardsCursor(Player p, Room room, float distance) {
        // Player center
        float cx = p.getX() + p.getWidth() / 2f;
        float cy = p.getY() + p.getHeight() / 2f;

        // Mouse in WORLD coords
        float mx = room.mouseToWorldX();
        float my = room.mouseToWorldY();

        // Aim vector
        float dx = mx - cx;
        float dy = my - cy;

        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len == 0f) {
            // fallback if mouse is exactly on player
            return new float[]{cx, cy};
        }

        dx /= len;
        dy /= len;

        // Book sits distance units away from the player center
        float sx = cx + dx * distance;
        float sy = cy + dy * distance;

        return new float[]{sx, sy};
    }


    private void updateBullets() {
        if (room == null) return;

        int[][] grid = room.getRoom();
        int tileSize = room.getTileSize();

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            if (b == null) {
                bullets.remove(i);
                continue;
            }

            b.update();

            if (b.collidesWithRoom(grid, tileSize)) {
                bullets.remove(i);
                continue;
            }

            if (b.isOffScreen()) {
                bullets.remove(i);
                continue;
            }

            boolean hitEnemy = false;
            for (int e = enemies.size() - 1; e >= 0; e--) {
                Enemy enemy = enemies.get(e);
                if (enemy == null) {
                    enemies.remove(e);
                    continue;
                }

                if (overlaps(b.getX(), b.getY(), b.getWidth(), b.getHeight(),
                    enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {

                    enemy.takeDamage(bulletDamage);
                    if (audio != null) audio.playHit();
                    enemy.takeDamage(bulletDamage);

                    // add popup at enemy top
                    damagePopups.add(new DamagePopup(
                        enemy.getX() + enemy.getWidth() / 2f,
                        enemy.getY() + enemy.getHeight(),
                        bulletDamage
                    ));

                    hitEnemy = true;

                    if (enemy.isDead()) {
                        enemies.remove(e);
                        enemiesKilled++;
                    }
                    break;
                }
            }

            if (hitEnemy) bullets.remove(i);
        }
    }

    private void handlePlayerEnemyContact() {
        if (player == null) return;

        for (Enemy enemy : enemies) {
            if (enemy == null) continue;

            boolean hit = overlaps(
                player.getX(), player.getY(), player.getWidth(), player.getHeight(),
                enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight()
            );

            if (hit && !player.isInvulnerable()) {
                player.takeDamage(1);

                float px = player.getX() + player.getWidth() / 2f;
                float py = player.getY() + player.getHeight() / 2f;
                float ex = enemy.getX() + enemy.getWidth() / 2f;
                float ey = enemy.getY() + enemy.getHeight() / 2f;

                float dx = px - ex;
                float dy = py - ey;
                float len = (float)Math.sqrt(dx * dx + dy * dy);
                if (len != 0f) { dx /= len; dy /= len; }

                float push = 8f;
                float knockX = dx * push;
                float knockY = dy * push;

                player.clampToScreen();
            }
        }
    }

    // -------------------- Upgrades --------------------
    private void beginUpgradeChoice() {
        choosingUpgrade = true;
        generateOfferedUpgrades();
    }

    private void handleUpgradeInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) applyUpgrade(offeredUpgrades[0]);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) applyUpgrade(offeredUpgrades[1]);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) applyUpgrade(offeredUpgrades[2]);
    }

    private void generateOfferedUpgrades() {
        offeredUpgrades[0] = randomUpgrade();
        offeredUpgrades[1] = randomUpgrade();
        offeredUpgrades[2] = randomUpgrade();
    }

    private void clearOfferedUpgrades() {
        offeredUpgrades[0] = null;
        offeredUpgrades[1] = null;
        offeredUpgrades[2] = null;
    }

    private Upgrade randomUpgrade() {
        int r = rng.nextInt(5);
        if (r == 0) return new Upgrade("Rapid Fire", "Fire rate +20%", cardTexture);
        if (r == 1) return new Upgrade("Runner", "Move speed +15%", cardTexture);
        if (r == 2) return new Upgrade("Vitality", "Max HP +1 and heal 1", cardTexture);
        if (r == 3) return new Upgrade("Extra Damage", "Bullet damage +1", cardTexture);
        return new Upgrade("Projectile Speed", "Bullet speed +20%", cardTexture);
    }

    private void applyUpgrade(Upgrade u) {
        if (u == null || player == null) return;

        if (u.name.equals("Rapid Fire")) {
            // Prefer weapon cooldown time if present
            if (weapon != null) {
                weapon.setAttackCooldownTime(Math.max(0.05f, weapon.getAttackCooldownTime() * 0.8f));
            } else {
                attackCooldownTime = Math.max(0.05f, attackCooldownTime * 0.8f);
            }
        } else if (u.name.equals("Runner")) {
            player.setSpeed(player.getSpeed() * 1.15f);
        } else if (u.name.equals("Vitality")) {
            player.increaseMaxHealth(1);
            player.heal(1);
        } else if (u.name.equals("Extra Damage")) {
            bulletDamage += 1;
        } else if (u.name.equals("Projectile Speed")) {
            bulletSpeed *= 1.2f;
        }

        choosingUpgrade = false;
        clearOfferedUpgrades();
    }

    // -------------------- Helpers --------------------
    private boolean overlaps(float ax, float ay, float aw, float ah,
                             float bx, float by, float bw, float bh) {
        return ax < bx + bw &&
            ax + aw > bx &&
            ay < by + bh &&
            ay + ah > by;
    }

    private boolean rectHitsWall(float x, float y, float w, float h) {
        int[][] grid = room.getRoom();
        int tileSize = room.getTileSize();

        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        int left = clamp((int) (x / tileSize), 0, roomW - 1);
        int right = clamp((int) ((x + w - 1) / tileSize), 0, roomW - 1);
        int bottom = clamp((int) (y / tileSize), 0, roomH - 1);
        int top = clamp((int) ((y + h - 1) / tileSize), 0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            for (int tx = left; tx <= right; tx++) {
                int t = grid[ty][tx];
                if (t == 1 || t == 2 || t == 3 || t == 4 || t == 5) return true;
            }
        }
        return false;
    }

    private float[] findFirstOpenSpot(float size, float minDistFromPlayer) {
        int tileSize = room.getTileSize();
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;

        for (int ty = 1; ty < roomH - 1; ty++) {
            for (int tx = 1; tx < roomW - 1; tx++) {
                if (room.getTile(tx, ty) == 1) continue;

                float x = tx * tileSize + (tileSize - size) / 2f;
                float y = ty * tileSize + (tileSize - size) / 2f;

                if (rectHitsWall(x, y, size, size)) continue;

                float ex = x + size / 2f;
                float ey = y + size / 2f;
                float dx = ex - px;
                float dy = ey - py;

                if (dx * dx + dy * dy < minDistFromPlayer * minDistFromPlayer) continue;

                return new float[]{x, y};
            }
        }
        return null;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void applyKnockbackWithCollision(Player player, float knockX, float knockY, Room room, int tileSize) {
        if (player == null || room == null) return;

        float oldX = player.getX();
        float oldY = player.getY();

        // X axis
        player.setX(oldX + knockX);
        if (playerCollidesRoom(player, room, tileSize)) {
            player.setX(oldX);
        }

        // Y axis
        player.setY(oldY + knockY);
        if (playerCollidesRoom(player, room, tileSize)) {
            player.setY(oldY);
        }
    }

    private boolean playerCollidesRoom(Player p, Room room, int tileSize) {
        int[][] grid = room.getRoom();
        int roomW = room.getRoomWidth();
        int roomH = room.getRoomHeight();

        int left   = clamp((int)(p.getX() / tileSize), 0, roomW - 1);
        int right  = clamp((int)((p.getX() + p.getWidth() - 1) / tileSize), 0, roomW - 1);
        int bottom = clamp((int)(p.getY() / tileSize), 0, roomH - 1);
        int top    = clamp((int)((p.getY() + p.getHeight() - 1) / tileSize), 0, roomH - 1);

        for (int ty = bottom; ty <= top; ty++) {
            for (int tx = left; tx <= right; tx++) {
                if (grid[ty][tx] == 1) return true;
            }
        }
        return false;
    }

    public void setAimWorld(float x, float y) {
        this.aimWorldX = x;
        this.aimWorldY = y;
    }

    public float[] getBookWorldPos(float distance) {
        if (player == null) return new float[]{0f, 0f};

        float cx = player.getX() + player.getWidth() / 2f;
        float cy = player.getY() + player.getHeight() / 2f;

        float dx = aimWorldX - cx;
        float dy = aimWorldY - cy;

        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len == 0f) return new float[]{cx, cy};

        dx /= len;
        dy /= len;

        float sx = cx + dx * distance;
        float sy = cy + dy * distance;

        // Optional: keep the book point out of walls (tiny 4x4 probe)
        if (rectHitsWall(sx - 2, sy - 2, 4, 4)) {
            // pull it halfway back toward player until valid
            for (int i = 0; i < 6; i++) {
                sx = (sx + cx) * 0.5f;
                sy = (sy + cy) * 0.5f;
                if (!rectHitsWall(sx - 2, sy - 2, 4, 4)) break;
            }
        }

        return new float[]{sx, sy};
    }

    /** Angle the book should face (optional, for rotation). */
    public float getAimAngleDeg() {
        if (player == null) return 0f;

        float cx = player.getX() + player.getWidth() / 2f;
        float cy = player.getY() + player.getHeight() / 2f;

        float dx = aimWorldX - cx;
        float dy = aimWorldY - cy;

        return (float)Math.toDegrees(Math.atan2(dy, dx));
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public int getCoins() { return coins; }
    public int getSouls() { return souls; }

    public void dispose() {
        cardTexture.dispose();
        Zombie.disposeShared();
    }
}
