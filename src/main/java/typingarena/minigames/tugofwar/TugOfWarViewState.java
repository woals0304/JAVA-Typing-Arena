package typingarena.minigames.tugofwar;

import typingarena.core.tugofwar.GameLogic;

/**
 * RopePanel에 전달되는 최소 상태 스냅샷.
 */
public class TugOfWarViewState {
    public double pos;
    public String currentWord;
    public GameLogic.WordModifier modifier;
    public boolean blindActive;
    public boolean jamoSplitActive;

    public TugOfWarViewState() {
        this(0.0, "", GameLogic.WordModifier.NEUTRAL, false, false);
    }

    public TugOfWarViewState(double pos, String currentWord,
                             GameLogic.WordModifier modifier,
                             boolean blindActive,
                             boolean jamoSplitActive) {
        this.pos = pos;
        this.currentWord = currentWord;
        this.modifier = modifier;
        this.blindActive = blindActive;
        this.jamoSplitActive = jamoSplitActive;
    }
}
