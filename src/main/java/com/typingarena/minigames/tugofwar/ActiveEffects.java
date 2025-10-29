package com.typingarena.minigames.tugofwar;

/**
 * 아이템 효과들의 지속시간을 관리하는 클래스.
 * now < ...Until 이면 해당 효과가 활성 중이라고 본다.
 */
public class ActiveEffects {
    long powerGripUntil = 0L; // 파워 그립: 정답 밀 힘 2배
    long anchorUntil    = 0L; // 앵커: 거의 안 밀림
    long blindUntil     = 0L; // 먹물: 단어 부분 가리기

    public boolean isPowerGripActive() {
        return System.currentTimeMillis() < powerGripUntil;
    }

    public boolean isAnchorActive() {
        return System.currentTimeMillis() < anchorUntil;
    }

    public boolean isBlindActive() {
        return System.currentTimeMillis() < blindUntil;
    }

    public void clearAll() {
        powerGripUntil = 0L;
        anchorUntil = 0L;
        blindUntil = 0L;
    }

    // HUD 표시용 문자열
    public String describeEffects() {
        StringBuilder sb = new StringBuilder();
        if (isPowerGripActive()) sb.append("[파워그립] ");
        if (isAnchorActive())    sb.append("[앵커] ");
        if (isBlindActive())     sb.append("[먹물] ");
        if (sb.length() == 0) return "효과: 없음";
        return "효과: " + sb.toString().trim();
    }
}
