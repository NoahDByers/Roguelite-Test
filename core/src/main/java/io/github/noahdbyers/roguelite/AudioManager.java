package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Collections;

public class AudioManager {
    private Sound sfxShoot;
    private Sound sfxHit;
    private Sound sfxUIClick;
    private ArrayList<Sound> swordSwingMiss = new ArrayList<>();
    private ArrayList<Sound> swordSwingHit = new ArrayList<>();

    private Music musicMain;
    private Music musicCemetery;

    private float sfxVolume = 0.2f;
    private float musicVolume = 0.3f;
    private boolean muted = false;

    public void load() {
        // SFX
        sfxShoot   = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/combatSFX/shoot_pcm.wav"));
        sfxHit     = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/combatSFX/hit_pcm.wav"));
        sfxUIClick = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/uiSFX/Minimalist3_pcm.wav"));

        // Music
        musicMain = Gdx.audio.newMusic(Gdx.files.internal("audio/music/main_theme_pcm.wav"));
        musicMain.setLooping(true);
        musicMain.setVolume(muted ? 0f : musicVolume);
        musicCemetery = Gdx.audio.newMusic(Gdx.files.internal("audio/music/cemetery_pcm.wav"));
        musicCemetery.setLooping(true);
        musicCemetery.setVolume(muted ? 0f : musicVolume);

        Collections.addAll(swordSwingMiss,
            Gdx.audio.newSound(Gdx.files.internal("audio/sfx/combatSFX/sword_miss_1_pcm.wav")),
            Gdx.audio.newSound(Gdx.files.internal("audio/sfx/combatSFX/sword_miss_2_pcm.wav")),
            Gdx.audio.newSound(Gdx.files.internal("audio/sfx/combatSFX/sword_miss_3_pcm.wav"))
        );

        Collections.addAll(swordSwingHit,
            Gdx.audio.newSound(Gdx.files.internal("audio/sfx/combatSFX/sword_hit_1_pcm.wav")),
            Gdx.audio.newSound(Gdx.files.internal("audio/sfx/combatSFX/sword_hit_2_pcm.wav")),
            Gdx.audio.newSound(Gdx.files.internal("audio/sfx/combatSFX/sword_hit_3_pcm.wav"))
        );
    }

    // ---- Play helpers ----
    public void playSwordMiss() {
        int value = MathUtils.random(0, 2);
        playSfx(swordSwingMiss.get(value));
    }
    public void playSwordHit() {
        int value = MathUtils.random(0,2);
        playSfx(swordSwingHit.get(value));
    }
    public void playShoot()   { playSfx(sfxShoot); }
    public void playHit()     { playSfx(sfxHit); }
    public void playUIClick() { playSfx(sfxUIClick); }

    private void playSfx(Sound sfx) {
        if (sfx == null || muted) return;
        sfx.play(sfxVolume);
    }

    public void startMainMusic() {
        if (musicMain == null) return;
        musicMain.setVolume(muted ? 0f : musicVolume);
        if (!musicMain.isPlaying()) musicMain.play();
    }

    public void startGameMusic() {
        if (musicCemetery == null) return;
        musicCemetery.setVolume(muted ? 0f : (musicVolume - (float)0.2));
        if (!musicCemetery.isPlaying()) musicCemetery.play();
    }
    public void stopMainMusic() {
        if (musicMain != null) musicMain.stop();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (musicMain != null) musicMain.setVolume(muted ? 0f : musicVolume);
    }

    public boolean isMuted() { return muted; }

    public void setSfxVolume(float v) { sfxVolume = clamp01(v); }
    public void setMusicVolume(float v) {
        musicVolume = clamp01(v);
        if (musicMain != null && !muted) musicMain.setVolume(musicVolume);
    }

    private float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    public void dispose() {
        if (sfxShoot != null) sfxShoot.dispose();
        if (sfxHit != null) sfxHit.dispose();
        if (sfxUIClick != null) sfxUIClick.dispose();
        if (musicMain != null) musicMain.dispose();
        if (musicCemetery != null)  { musicCemetery.stop();  musicCemetery.dispose();  musicCemetery = null; }
        for (Sound e : swordSwingMiss) {
            e.dispose();
        }

        System.out.println("Audio dispose called");
    }
}
