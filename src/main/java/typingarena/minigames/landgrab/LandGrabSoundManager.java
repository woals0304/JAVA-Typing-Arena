package typingarena.minigames.landgrab;

import javafx.scene.media.AudioClip;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * LandGrab 미니게임 전용 사운드 매니저
 * 팀 프로젝트 충돌 방지를 위해 독립적으로 관리됨
 */
public class LandGrabSoundManager {

    private static LandGrabSoundManager instance;

    // [중요] LandGrab 전용 폴더 경로
    private static final String SFX_PATH = "/sounds/landgrab/sfx/";
    private static final String BGM_PATH = "/sounds/landgrab/bgm/";

    // 효과음 캐시
    private final Map<String, AudioClip> soundCache = new HashMap<>();

    // 배경음악 (WAV 반복 재생용)
    private AudioClip bgmPlayer;

    private boolean isMuted = false;

    private LandGrabSoundManager() {}

    public static LandGrabSoundManager getInstance() {
        if (instance == null) { instance = new LandGrabSoundManager(); }
        return instance;
    }

    // 효과음 미리 로딩 (게임 시작 시 호출)
    public void loadSound(String fileName) {
        if (soundCache.containsKey(fileName)) return;
        try {
            // 경로가 맞는지 확인하기 위해 null 체크
            URL res = getClass().getResource(SFX_PATH + fileName);
            if (res != null) {
                soundCache.put(fileName, new AudioClip(res.toExternalForm()));
            } else {
                System.err.println("[LandGrab] 효과음 파일 없음: " + SFX_PATH + fileName);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 효과음 재생
    public void play(String fileName) {
        if (isMuted) return;

        AudioClip clip = soundCache.get(fileName);
        if (clip == null) {
            loadSound(fileName);
            clip = soundCache.get(fileName);
        }

        if (clip != null) {
            if (clip.isPlaying()) clip.stop(); // 겹침 방지 (깔끔한 소리를 위해)
            clip.play();
        }
    }

    // 배경음악 재생
    public void playBgm(String fileName) {
        stopBgm(); // 기존 음악 정지

        try {
            URL res = getClass().getResource(BGM_PATH + fileName);
            if (res != null) {
                bgmPlayer = new AudioClip(res.toExternalForm());
                bgmPlayer.setCycleCount(AudioClip.INDEFINITE); // 무한 반복
                if (!isMuted) bgmPlayer.play();
            } else {
                System.err.println("[LandGrab] BGM 파일 없음: " + BGM_PATH + fileName);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void stopBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer = null;
        }
    }

    public void setMute(boolean mute) {
        this.isMuted = mute;
        if (bgmPlayer != null) {
            if (mute) bgmPlayer.stop();
            else bgmPlayer.play();
        }
    }
}