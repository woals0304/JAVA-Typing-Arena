package typingarena.minigames.tugofwar;

import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 줄다리기 전용 사운드 매니저.
 * LandGrab에서 추가된 패턴을 그대로 가져와 AudioClip 캐싱, BGM 루프, 재생/정지 토글을 관리한다.
 */
public class TugOfWarSoundManager {

    private static TugOfWarSoundManager instance;

    // 현재 리소스는 landgrab 사운드를 재사용한다. 추후 tugofwar 전용 리소스를 두면 경로만 교체하면 됨.
    private static final String SFX_PATH = "/sounds/landgrab/sfx/";
    private static final String BGM_PATH = "/sounds/landgrab/bgm/";

    private final Map<String, AudioClip> soundCache = new HashMap<>();
    private AudioClip bgmPlayer;
    private boolean muted = false;

    private TugOfWarSoundManager() {}

    public static TugOfWarSoundManager getInstance() {
        if (instance == null) instance = new TugOfWarSoundManager();
        return instance;
    }

    public void load(String fileName) {
        if (soundCache.containsKey(fileName)) return;
        try {
            URL res = getClass().getResource(SFX_PATH + fileName);
            if (res != null) {
                soundCache.put(fileName, new AudioClip(res.toExternalForm()));
            } else {
                System.err.println("[TugOfWar] 효과음 파일 없음: " + SFX_PATH + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play(String fileName) {
        if (muted) return;
        AudioClip clip = soundCache.get(fileName);
        if (clip == null) {
            load(fileName);
            clip = soundCache.get(fileName);
        }
        if (clip != null) {
            if (clip.isPlaying()) clip.stop();
            clip.play();
        }
    }

    public void playBgm(String fileName) {
        stopBgm();
        try {
            URL res = getClass().getResource(BGM_PATH + fileName);
            if (res != null) {
                bgmPlayer = new AudioClip(res.toExternalForm());
                bgmPlayer.setCycleCount(AudioClip.INDEFINITE);
                if (!muted) bgmPlayer.play();
            } else {
                System.err.println("[TugOfWar] BGM 파일 없음: " + BGM_PATH + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer = null;
        }
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (bgmPlayer != null) {
            if (muted) bgmPlayer.stop();
            else bgmPlayer.play();
        }
    }
}
