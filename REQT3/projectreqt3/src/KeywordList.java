// REQT4 — Keyword classification
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class KeywordList {
    private final Set<String> keywords;

    public KeywordList() {
        Set<String> set = new HashSet<>();
        Collections.addAll(set,
            "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
            "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return",
            "then", "true", "until", "while"
        );
        this.keywords = Collections.unmodifiableSet(set);
    }

    public boolean isKeyword(String lexeme) {
        return lexeme != null && keywords.contains(lexeme);
    }

    public TokenType classify(String lexeme) {
        if (!isKeyword(lexeme)) {
            return null;
        }
        if ("true".equals(lexeme) || "false".equals(lexeme)) {
            return TokenType.BOOLEAN_LITERAL;
        }
        if ("nil".equals(lexeme)) {
            return TokenType.NIL;
        }
        if ("and".equals(lexeme) || "or".equals(lexeme) || "not".equals(lexeme)) {
            return TokenType.LOGICAL_OP;
        }
        return TokenType.KEYWORD;
    }
}
