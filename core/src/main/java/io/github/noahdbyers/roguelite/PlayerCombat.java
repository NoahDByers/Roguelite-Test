package io.github.noahdbyers.roguelite;

public class PlayerCombat {

    private CombatState state = CombatState.FREE;
    private float stateTime = 0f;

    // --------------------
    // Input buffering
    // --------------------
    private boolean attackBuffered = false;
    private float attackBufferTimer = 0f;
    private static final float ATTACK_BUFFER_TIME = 0.12f;

    // --------------------
    // Attack execution
    // --------------------
    private AttackData currentAttack = null;
    private boolean hitboxFired = false;

    // ✅ Locked aim/direction for the whole swing
    private float lockedAimX = 0f;
    private float lockedAimY = 0f;
    private float lockedDirX = 1f;
    private float lockedDirY = 0f;

    // --------------------
    // Combo tracking
    // --------------------
    private int comboStep = 0;
    private final int maxCombo = 3;

    public boolean isFree() {
        return state == CombatState.FREE;
    }

    /** Call once per frame */
    public void update(float dt, GameWorld world) {
        // Buffer timer
        if (attackBuffered) {
            attackBufferTimer -= dt;
            if (attackBufferTimer <= 0f) {
                attackBuffered = false;
                attackBufferTimer = 0f;
            }
        }

        stateTime += dt;

        switch (state) {
            case FREE: {
                // Consume buffer if possible
                if (attackBuffered && canStartAttack(world)) {
                    attackBuffered = false;
                    startAttack(world);
                }
                break;
            }

            case ATTACK_STARTUP: {
                if (currentAttack == null) {
                    enter(CombatState.FREE);
                    break;
                }

                if (stateTime >= currentAttack.startup) {
                    enter(CombatState.ATTACK_ACTIVE);
                }
                break;
            }

            case ATTACK_ACTIVE: {
                if (currentAttack == null) {
                    enter(CombatState.FREE);
                    break;
                }

                // Fire hitbox once at hitboxTime (time since ACTIVE began)
                if (!hitboxFired && stateTime >= currentAttack.hitboxTime) {
                    hitboxFired = true;

                    float strength = 1f + 0.35f * comboStep; // step0=1.0, step1=1.35, step2=1.7

                    // ✅ Use LOCKED direction (consistent swing + consistent hitbox/knockback)
                    world.performMeleeAttackDir(lockedDirX, lockedDirY, strength);
                }

                // Leave ACTIVE after active duration
                if (stateTime >= currentAttack.active) {
                    enter(CombatState.ATTACK_RECOVERY);
                }
                break;
            }

            case ATTACK_RECOVERY: {
                if (currentAttack == null) {
                    enter(CombatState.FREE);
                    break;
                }

                // Let chaining happen during recovery if buffered and weapon ready
                if (attackBuffered && canChainAttack(world)) {
                    attackBuffered = false;
                    chainAttack(world);
                    break;
                }

                // Finish recovery and return to FREE (combo resets here)
                if (stateTime >= currentAttack.recovery) {
                    comboStep = 0;
                    enter(CombatState.FREE);
                }
                break;
            }

            case HITSTUN:
                // TODO later
                break;

            case DEAD:
                break;
        }
    }

    private void enter(CombatState next) {
        state = next;
        stateTime = 0f;
    }

    public void bufferAttack() {
        attackBuffered = true;
        attackBufferTimer = ATTACK_BUFFER_TIME;
    }

    private boolean canStartAttack(GameWorld world) {
        Weapon w = world.getWeapon();
        return w != null && w.isReady();
    }

    private void startAttack(GameWorld world) {
        Weapon w = world.getWeapon();
        Player p = world.getPlayer();
        if (w == null || p == null) return;

        // Pick attack data based on combo step
        currentAttack = getAttackForCombo(comboStep);
        hitboxFired = false;

        // ✅ Snapshot aim at attack start (so swing doesn't drift)
        lockedAimX = world.getAimWorldX();
        lockedAimY = world.getAimWorldY();

        // Compute locked direction from player center -> locked aim
        float px = p.getX() + p.getWidth() * 0.5f;
        float py = p.getY() + p.getHeight() * 0.5f;

        float dx = lockedAimX - px;
        float dy = lockedAimY - py;

        float len2 = dx * dx + dy * dy;
        if (len2 < 0.0001f) {
            dx = 1f;
            dy = 0f;
            len2 = 1f;
        }
        float invLen = (float) (1.0 / Math.sqrt(len2));
        lockedDirX = dx * invLen;
        lockedDirY = dy * invLen;

        // ✅ Tell weapon to lock its swing direction too
        // NOTE: this assumes your new Weapon.startAttack signature is:
        // startAttack(playerX, playerY, playerW, playerH, aimX, aimY)
        w.startAttack(p.getX(), p.getY(), p.getWidth(), p.getHeight(), lockedAimX, lockedAimY);

        enter(CombatState.ATTACK_STARTUP);
    }

    private boolean canChainAttack(GameWorld world) {
        // Chain only if we have another step available
        if (comboStep >= maxCombo - 1) return false;

        Weapon w = world.getWeapon();
        return w != null && w.isReady();
    }

    private void chainAttack(GameWorld world) {
        comboStep++;
        startAttack(world);
    }

    private AttackData getAttackForCombo(int step) {
        // startup, active, recovery, hitboxTime (relative to ACTIVE start)
        if (step == 0) return new AttackData(0.08f, 0.06f, 0.16f, 0.00f);
        if (step == 1) return new AttackData(0.06f, 0.06f, 0.20f, 0.00f);
        return new AttackData(0.08f, 0.06f, 0.20f, 0.00f);
    }
}
