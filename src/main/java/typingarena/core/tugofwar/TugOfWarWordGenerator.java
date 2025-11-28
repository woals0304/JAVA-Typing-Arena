package typingarena.core.tugofwar;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 줄다리기 단어/모디파이어 생성기. 싱글/멀티 양쪽에서 동일한 규칙을 사용한다.
 */
public final class TugOfWarWordGenerator {

    private static final String WORD_RESOURCE = "words/ko.json";
    private static final String[] DEFAULT_WORDS = {
            "apple","note","river","korea","typing","banana","window","socket","orange","system",
            "thread","packet","object","combo","vector","method","class","random","matrix","buffer",
            "friend","music","guitar","soccer","player","winner","castle","dragon","danger","shield",
            "future","simple","mobile","attack","defense","victory","balance","energy","memory","rocket",
            "coffee","school","winter","summer","spring","autumn","family","forest","desert","thunder"
    };

    private static final List<String> WORD_POOL = loadWordPool();

    private TugOfWarWordGenerator() {}

    public static Word next(Random rnd) {
        if (WORD_POOL.isEmpty()) {
            return new Word("", GameLogic.WordModifier.NEUTRAL);
        }
        String text = WORD_POOL.get(rnd.nextInt(WORD_POOL.size()));
        GameLogic.WordModifier modifier;
        double roll = rnd.nextDouble();
        if (roll < 0.5) { // 50%
            modifier = GameLogic.WordModifier.NEUTRAL;
        } else if (roll < 0.75) { // 25%
            modifier = GameLogic.WordModifier.BUFF;
        } else { // 25%
            modifier = GameLogic.WordModifier.TRAP;
        }
        return new Word(text, modifier);
    }

    public record Word(String text, GameLogic.WordModifier modifier) {}

    private static List<String> loadWordPool() {
        List<String> words = loadFromClasspath();
        if (words.isEmpty()) {
            words = loadFromFilesystem();
        }
        if (words.isEmpty()) {
            words = Arrays.asList(DEFAULT_WORDS);
        }
        return Collections.unmodifiableList(words);
    }

    private static List<String> loadFromClasspath() {
        try (InputStream in = TugOfWarWordGenerator.class.getClassLoader().getResourceAsStream(WORD_RESOURCE)) {
            if (in == null) return Collections.emptyList();
            try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return parseWordList(reader);
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private static List<String> loadFromFilesystem() {
        Path path = Paths.get("src", "main", "resources").resolve(WORD_RESOURCE);
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parseWordList(reader);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private static List<String> parseWordList(Reader reader) throws IOException {
        Gson gson = new Gson();
        WordList data = gson.fromJson(reader, WordList.class);
        if (data == null || data.words == null) {
            return Collections.emptyList();
        }
        List<String> words = new ArrayList<>();
        for (String word : data.words) {
            if (word != null) {
                String trimmed = word.trim();
                if (!trimmed.isEmpty()) words.add(trimmed);
            }
        }
        return words;
    }

    private static final class WordList {
        List<String> words;
    }
}
