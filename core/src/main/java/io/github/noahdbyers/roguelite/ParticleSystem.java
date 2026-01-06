package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Random;

/**
 * Lightweight, code-driven particle system (no external .pfx assets needed).
 *
 * Effects included:
 * - Hit sparks (on enemy hit)
 * - Death burst (on enemy death)
 * - Chest sparkle burst (on chest open)
 */
public class ParticleSystem {

    private static class Particle {
        float x, y;
        float vx, vy;
        float life, age;
        float size0, size1;
        float rot, rotSpd;

        float r0, g0, b0;
        float r1, g1, b1;

        float drag;   // 0..1 per second-ish
        float gravity;
    }

    private final ArrayList<Particle> particles = new ArrayList<>();
    private final Random rng;

    private final Texture pixel;
    private final Color tmp = new Color();

    public ParticleSystem(Random rng) {
        this.rng = (rng != null) ? rng : new Random();

        Pixmap pm = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
    }

    public void dispose() {
        pixel.dispose();
    }

    public void update(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.age += dt;
            if (p.age >= p.life) {
                particles.remove(i);
                continue;
            }

            // Integrate
            p.vx *= Math.max(0f, 1f - p.drag * dt);
            p.vy *= Math.max(0f, 1f - p.drag * dt);
            p.vy += p.gravity * dt;

            p.x += p.vx * dt;
            p.y += p.vy * dt;

            p.rot += p.rotSpd * dt;
        }
    }

    public void draw(SpriteBatch batch) {
        if (particles.isEmpty()) return;

        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            float t = p.age / p.life;

            float r = MathUtils.lerp(p.r0, p.r1, t);
            float g = MathUtils.lerp(p.g0, p.g1, t);
            float b = MathUtils.lerp(p.b0, p.b1, t);
            float a = 1f - t;

            float size = MathUtils.lerp(p.size0, p.size1, t);

            tmp.set(r, g, b, a);
            batch.setColor(tmp);
            batch.draw(
                pixel,
                p.x - size * 0.5f, p.y - size * 0.5f,
                size * 0.5f, size * 0.5f,
                size, size,
                1f, 1f,
                p.rot,
                0, 0, 2, 2,
                false, false
            );
        }

        batch.setColor(Color.WHITE);
    }

    // ------------------------------------------------------------
    // Spawners
    // ------------------------------------------------------------

    public void spawnHit(float x, float y, float dirX, float dirY, boolean crit, DamageType type) {
        float baseAngle = MathUtils.atan2(dirY, dirX);
        int count = crit ? 18 : 12;

        ColorSet cs = colorForType(type);

        for (int i = 0; i < count; i++) {
            float spread = MathUtils.degreesToRadians * (crit ? 95f : 80f);
            float a = baseAngle + randRange(-spread, spread);

            float speed = randRange(110f, crit ? 340f : 260f);
            float vx = MathUtils.cos(a) * speed + randRange(-35f, 35f);
            float vy = MathUtils.sin(a) * speed + randRange(-35f, 35f);

            Particle p = new Particle();
            p.x = x + randRange(-3f, 3f);
            p.y = y + randRange(-3f, 3f);
            p.vx = vx;
            p.vy = vy;

            p.life = randRange(0.12f, crit ? 0.28f : 0.22f);
            p.age = 0f;

            p.size0 = randRange(3.5f, crit ? 8.0f : 6.0f);
            p.size1 = 0.5f;

            p.rot = randRange(0f, 360f);
            p.rotSpd = randRange(-720f, 720f);

            p.r0 = cs.r0; p.g0 = cs.g0; p.b0 = cs.b0;
            p.r1 = cs.r1; p.g1 = cs.g1; p.b1 = cs.b1;

            p.drag = 2.2f;
            p.gravity = randRange(-60f, -180f);

            particles.add(p);
        }
    }

    public void spawnEnemyDeath(float x, float y, DamageType type) {
        // A core burst + some drifting embers.
        ColorSet cs = colorForType(type);
        int count = 28;

        for (int i = 0; i < count; i++) {
            float a = randRange(0f, MathUtils.PI2);
            float speed = randRange(60f, 240f);

            Particle p = new Particle();
            p.x = x + randRange(-6f, 6f);
            p.y = y + randRange(-6f, 6f);
            p.vx = MathUtils.cos(a) * speed * randRange(0.7f, 1.1f);
            p.vy = MathUtils.sin(a) * speed * randRange(0.7f, 1.1f) + randRange(40f, 140f);

            p.life = randRange(0.25f, 0.55f);
            p.age = 0f;

            p.size0 = randRange(5.0f, 10.5f);
            p.size1 = randRange(0.5f, 2.0f);

            p.rot = randRange(0f, 360f);
            p.rotSpd = randRange(-420f, 420f);

            // Slightly darker end for "smoke out"
            p.r0 = cs.r0; p.g0 = cs.g0; p.b0 = cs.b0;
            p.r1 = cs.r1 * 0.55f; p.g1 = cs.g1 * 0.55f; p.b1 = cs.b1 * 0.55f;

            p.drag = 1.2f;
            p.gravity = randRange(-120f, -220f);

            particles.add(p);
        }
    }

    public void spawnChestOpen(float x, float y) {
        // Golden sparkles.
        int count = 22;
        for (int i = 0; i < count; i++) {
            float a = randRange(-MathUtils.PI / 2f - 1.3f, -MathUtils.PI / 2f + 1.3f); // mostly upward
            float speed = randRange(90f, 260f);

            Particle p = new Particle();
            p.x = x + randRange(-6f, 6f);
            p.y = y + randRange(-4f, 4f);
            p.vx = MathUtils.cos(a) * speed + randRange(-35f, 35f);
            p.vy = MathUtils.sin(a) * speed + randRange(120f, 220f);

            p.life = randRange(0.35f, 0.75f);
            p.age = 0f;

            p.size0 = randRange(4.0f, 9.0f);
            p.size1 = randRange(0.5f, 2.5f);

            p.rot = randRange(0f, 360f);
            p.rotSpd = randRange(-540f, 540f);

            // Gold -> pale gold
            p.r0 = 1.00f; p.g0 = 0.88f; p.b0 = 0.25f;
            p.r1 = 1.00f; p.g1 = 0.95f; p.b1 = 0.70f;

            p.drag = 0.9f;
            p.gravity = randRange(-100f, -170f);

            particles.add(p);
        }
    }

    // ------------------------------------------------------------
    // Color mapping
    // ------------------------------------------------------------

    private static class ColorSet {
        float r0, g0, b0;
        float r1, g1, b1;
        ColorSet(float r0, float g0, float b0, float r1, float g1, float b1) {
            this.r0 = r0; this.g0 = g0; this.b0 = b0;
            this.r1 = r1; this.g1 = g1; this.b1 = b1;
        }
    }

    private ColorSet colorForType(DamageType type) {
        if (type == null) return new ColorSet(0.95f, 0.25f, 0.20f, 0.80f, 0.10f, 0.10f); // default red
        switch (type) {
            case FIRE:
                return new ColorSet(1.00f, 0.45f, 0.10f, 0.70f, 0.15f, 0.05f);
            case LIGHTNING:
                return new ColorSet(1.00f, 0.95f, 0.35f, 0.85f, 0.75f, 0.10f);
            case HOLY:
                return new ColorSet(0.95f, 0.95f, 1.00f, 0.75f, 0.85f, 1.00f);
            case DARK:
                return new ColorSet(0.65f, 0.25f, 0.90f, 0.25f, 0.10f, 0.35f);
            case RADIANT:
                return new ColorSet(0.85f, 1.00f, 0.65f, 0.40f, 0.65f, 0.20f);
            case TRUE:
                return new ColorSet(1.00f, 1.00f, 1.00f, 0.75f, 0.75f, 0.75f);
            case PHYSICAL:
            default:
                return new ColorSet(0.95f, 0.25f, 0.20f, 0.80f, 0.10f, 0.10f);
        }
    }

    private float randRange(float lo, float hi) {
        return lo + rng.nextFloat() * (hi - lo);
    }
}
