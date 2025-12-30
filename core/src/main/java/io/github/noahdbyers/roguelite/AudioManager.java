package io.github.noahdbyers.roguelite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class AudioManager {
    private Sound sfxShoot;
    private Sound sfxHit;
    private Sound sfxUIClick;

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
    }

    // ---- Play helpers ----
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
    }
}
