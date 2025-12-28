package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;

public class GameWorld {
    // Room / world
    private final Room room;

    // Entities
    private Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Bullet> bullets = new ArrayList<>();

    // Run stats / progression
    private boolean gameOver = false;

    private int enemiesKilled = 0;

    private int wave = 1;
    private boolean waveActive = false;

    // Upgrades
    private boolean choosingUpgrade = false;
    private final Upgrade[] offeredUpgrades = new Upgrade[3];

    // Difficulty
    private float spawnTimer = 0f;
    private float difficultyTimer = 0f;

    private final float startSpawnInterval = 1.0f;
    private final float startMinSpawnInterval = 0.5f;
    private final int startMaxEnemies = 20;

    private float spawnInterval = startSpawnInterval;
    private float minSpawnInterval = startMinSpawnInterval;
    private int maxEnemies = startMaxEnemies;

    // Weapon / auto-fire
    private final Weapon starterWeapon = new Weapon("Starter", 0f, 0.25f);

    public GameWorld(Room room) {
        this.room = room;
        restart();
    }

    // -------------------- Public getters (Main renders from these) --------------------
    public Room getRoom() { return room; }
    public Player getPlayer() { return player; }
    public ArrayList<Enemy> getEnemies() { return enemies; }
    public ArrayList<Bullet> getBullets() { return bullets; }

    public boolean isGameOver() { return gameOver; }
    public boolean isChoosingUpgrade() { return choosingUpgrade; }
    public Upgrade[] getOfferedUpgrades() { return offeredUpgrades; }

    public int getEnemiesKilled() { return enemiesKilled; }
    public int getWave() { return wave; }

    // -------------------- Update loop --------------------
    public void update(float delta) {
        // Restart works anytime
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restart();
            return;
        }

        if (gameOver) return;

        // If we’re choosing upgrades, pause simulation until selection
        if (choosingUpgrade) {
            handleUpgradeInput();
            return;
        }

        // Normal simulation
        player.update(room, room.getTileSize());          // use your existing player update that includes collision
        player.updateTimers();

        for (Enemy e : enemies) {
            e.update(player, room, room.getTileSize());
        }

        // Contact damage
        handlePlayerEnemyContact();

        // Auto-fire
        tryShootAutoAim(delta);

        // Bullets update / collisions
        updateBullets();

        // Waves + upgrades hook
        if (!waveActive) {
            startWave();
        }

        if (enemies.isEmpty()) {
            // wave cleared → show upgrade choices
            offerUpgrades();
            wave++;
            waveActive = false;
        }

        // Game over condition
        if (player.getHealth() <= 0) {
            gameOver = true;
        }

        // If you still want “spawn over time” inside waves, keep this:
        // updateSpawning(delta);
    }

    // -------------------- Restart --------------------
    public void restart() {
        gameOver = false;

        // Player
        player = new Player(100, 100, 200, 32, 32);

        // Enemies
        enemies.clear();
        enemies.add(new Enemy(200, 200, 100, 28, 3));
        enemies.add(new Enemy(400, 300, 120, 28, 3));

        // Bullets / weapon cooldown
        bullets.clear();
        starterWeapon.setAttackCooldown(0f);

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
    }

    // -------------------- Combat / collisions --------------------
    private void handlePlayerEnemyContact() {
        for (Enemy enemy : enemies) {
            boolean hit = overlaps(
                player.getX(), player.getY(), player.getWidth(), player.getHeight(),
                enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight()
            );

            if (hit && !player.isInvulnerable()) {
                player.takeDamage(1);

                // Optional: small knockback (keeps you from sticking)
                float px = player.getX() + player.getWidth() / 2f;
                float py = player.getY() + player.getHeight() / 2f;
                float ex = enemy.getX() + enemy.getWidth() / 2f;
                float ey = enemy.getY() + enemy.getHeight() / 2f;

                float dx = px - ex;
                float dy = py - ey;
                float len = (float)Math.sqrt(dx * dx + dy * dy);
                if (len != 0) { dx /= len; dy /= len; }

                player.clampToScreen(); // keep inside screen / room
            }
        }
    }

    private void tryShootAutoAim(float delta) {
        // Cooldown ticking
        if (starterWeapon.getAttackCooldown() > 0f) {
            starterWeapon.setAttackCooldown(starterWeapon.getAttackCooldown() - delta);
            if (starterWeapon.getAttackCooldown() < 0f) starterWeapon.setAttackCooldown(0f);
        }

        if (starterWeapon.getAttackCooldown() > 0f) return;

        Enemy target = getNearestEnemy();
        if (target == null) return;

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;

        float tx = target.getX() + target.getWidth() / 2f;
        float ty = target.getY() + target.getHeight() / 2f;

        float dirX = tx - px;
        float dirY = ty - py;

        float bulletSpeed = 450f;
        float bulletSize = 8f;

        bullets.add(new Bullet(px - bulletSize / 2f, py - bulletSize / 2f, dirX, dirY, bulletSpeed, bulletSize));
        starterWeapon.setAttackCooldown(starterWeapon.getAttackCooldownTime());
    }

    private void updateBullets() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update();

            if (b.isOffScreen() || b.collidesWithRoom(room.getRoom(), room.getTileSize())) {
                bullets.remove(i);
                continue;
            }

            boolean hitEnemy = false;

            for (Enemy e : enemies) {
                if (overlaps(
                    b.getX(), b.getY(), b.getWidth(), b.getHeight(),
                    e.getX(), e.getY(), e.getWidth(), e.getHeight()
                )) {
                    e.takeDamage(1);
                    hitEnemy = true;
                    break;
                }
            }

            if (hitEnemy) {
                bullets.remove(i);
                // Count & remove dead
                for (int e = enemies.size() - 1; e >= 0; e--) {
                    if (enemies.get(e).isDead()) {
                        enemies.remove(e);
                        enemiesKilled++;
                    }
                }
            }
        }
    }

    // -------------------- Waves / spawning --------------------
    private void startWave() {
        enemies.clear();

        int toSpawn = 2 + wave;
        float baseSpeed = 80f + wave * 8f;

        for (int i = 0; i < toSpawn; i++) {
            spawnEnemyWithSpeed(baseSpeed);
        }

        waveActive = true;
    }

    private void spawnEnemyWithSpeed(float speed) {
        if (enemies.size() >= maxEnemies) return;

        for (int attempt = 0; attempt < 50; attempt++) {
            int tx = MathUtils.random(0, room.getRoomWidth() - 1);
            int ty = MathUtils.random(0, room.getRoomHeight() - 1);

            if (room.getTile(tx, ty) != 0) continue; // must be floor

            float size = 28f;
            float x = tx * room.getTileSize() + (room.getTileSize() - size) / 2f;
            float y = ty * room.getTileSize() + (room.getTileSize() - size) / 2f;

            // don’t spawn on top of player
            float px = player.getX() + player.getWidth() / 2f;
            float py = player.getY() + player.getHeight() / 2f;
            float ex = x + size / 2f;
            float ey = y + size / 2f;
            float dx = ex - px;
            float dy = ey - py;

            float minDist = 120f;
            if (dx * dx + dy * dy < minDist * minDist) continue;

            enemies.add(new Enemy(x, y, speed, size, 3));
            return;
        }
    }

    // (Optional) If you still want “spawns speed up over time”, keep this and call it from update()
    @SuppressWarnings("unused")
    private void updateSpawning(float delta) {
        difficultyTimer += delta;
        if (difficultyTimer >= 5f) {
            difficultyTimer = 0f;
            spawnInterval = Math.max(minSpawnInterval, spawnInterval - 0.1f);
        }

        spawnTimer += delta;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnEnemyWithSpeed(100f);
        }
    }

    // -------------------- Upgrades --------------------
    private void offerUpgrades() {
        choosingUpgrade = true;
        for (int i = 0; i < 3; i++) {
            offeredUpgrades[i] = randomUpgrade();
        }
    }

    private void handleUpgradeInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) applyUpgrade(offeredUpgrades[0]);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) applyUpgrade(offeredUpgrades[1]);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) applyUpgrade(offeredUpgrades[2]);
    }

    private Upgrade randomUpgrade() {
        int r = MathUtils.random(0, 4);
        if (r == 0) return new Upgrade("Rapid Fire", "Fire rate +20%");
        if (r == 1) return new Upgrade("Heavy Bullets", "Bullet damage +1");
        if (r == 2) return new Upgrade("Hot Rounds", "Bullet speed +20%");
        if (r == 3) return new Upgrade("Runner", "Move speed +15%");
        return new Upgrade("Vitality", "Max HP +1 and heal 1");
    }

    private void applyUpgrade(Upgrade u) {
        if (u == null) return;

        if (u.name.equals("Rapid Fire")) {
            starterWeapon.setAttackCooldownTime(Math.max(0.05f, starterWeapon.getAttackCooldownTime() * 0.8f));
        } else if (u.name.equals("Runner")) {
            player.setSpeed(player.getSpeed() * 1.15f);
        } else if (u.name.equals("Vitality")) {
            player.increaseMaxHealth(1);
            player.heal(1);
        }

        // (You can wire bullet damage/speed into Bullet/Weapon next)

        choosingUpgrade = false;
    }

    // -------------------- Helpers --------------------
    private Enemy getNearestEnemy() {
        if (enemies.isEmpty()) return null;

        float px = player.getX() + player.getWidth() / 2f;
        float py = player.getY() + player.getHeight() / 2f;

        Enemy best = null;
        float bestDist2 = Float.MAX_VALUE;

        for (Enemy e : enemies) {
            float ex = e.getX() + e.getWidth() / 2f;
            float ey = e.getY() + e.getHeight() / 2f;

            float dx = ex - px;
            float dy = ey - py;
            float dist2 = dx * dx + dy * dy;

            if (dist2 < bestDist2) {
                bestDist2 = dist2;
                best = e;
            }
        }
        return best;
    }

    private boolean overlaps(
        float ax, float ay, float aw, float ah,
        float bx, float by, float bw, float bh) {

        return ax < bx + bw &&
            ax + aw > bx &&
            ay < by + bh &&
            ay + ah > by;
    }
}
