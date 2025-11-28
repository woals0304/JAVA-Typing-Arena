package typingarena.minigames.castledefense;

import javafx.scene.media.AudioClip;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CastleDefenseSoundManager {

    private static CastleDefenseSoundManager instance;

    // 경로 설정
    private static final String BGM_PATH = "/sounds/castledefense/";
    private static final String SFX_PATH = "/sounds/castledefense/sfx/";

    // 사운드 캐시 (미리 로딩된 사운드 저장소)
    private final Map<String, AudioClip> soundCache = new HashMap<>();
    
    private AudioClip bgmPlayer;
    private boolean isMuted = false;

    private CastleDefenseSoundManager() {}

    public static CastleDefenseSoundManager getInstance() {
        if (instance == null) { instance = new CastleDefenseSoundManager(); }
        return instance;
    }

    // [핵심] 사운드 미리 로딩 (LandGrab 방식)
    // 게임 시작 전에 이 메서드로 파일들을 미리 읽어옵니다.
    public void loadSound(String fileName) {
        if (soundCache.containsKey(fileName)) return; // 이미 있으면 패스

        try {
            URL res = getClass().getResource(SFX_PATH + fileName);
            if (res != null) {
                // AudioClip 생성 시점에 파일 로딩 및 디코딩이 수행됨
                soundCache.put(fileName, new AudioClip(res.toExternalForm()));
            } else {
                System.err.println("[CastleDefense] 효과음 파일 없음: " + SFX_PATH + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 효과음 재생 (볼륨 조절 가능)
    public void play(String fileName, double volume) {
        if (isMuted) return;

        // 캐시에서 가져오기
        AudioClip clip = soundCache.get(fileName);
        
        // 없으면 로딩 시도 (안전장치)
        if (clip == null) {
            loadSound(fileName);
            clip = soundCache.get(fileName);
        }

        if (clip != null) {
            if (clip.isPlaying()) clip.stop(); // 겹침 방지 (깔끔하게)
            clip.play(volume); // 설정한 볼륨으로 재생
        }
    }

    // 배경음악 재생
    public void playBgm(String fileName, double volume) {
        stopBgm(); 

        if (isMuted) return;

        try {
            URL res = getClass().getResource(BGM_PATH + fileName);
            if (res != null) {
                bgmPlayer = new AudioClip(res.toExternalForm());
                bgmPlayer.setCycleCount(AudioClip.INDEFINITE); 
                bgmPlayer.play(volume); 
            } else {
                System.err.println("BGM 파일 없음: " + BGM_PATH + fileName);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void stopBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer = null;
        }
    }
}