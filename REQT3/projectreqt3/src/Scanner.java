import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scanner {

    // ── Compiled regex patterns (tried in priority order) ────────────────────
    private static final Pattern PAT_WHITESPACE = Pattern.compile("[ \\t\\r\\n]+");
    private static final Pattern PAT_COMMENT    = Pattern.compile("--[^\\n]*");
    private static final Pattern PAT_STR_DQ     = Pattern.compile("\"(?:[^\"\\\\\\n]|\\\\.)*\"");
    private static final Pattern PAT_ERR_DQ     = Pattern.compile("\"(?:[^\"\\\\\\n]|\\\\.)*");
    private static final Pattern PAT_STR_SQ     = Pattern.compile("'(?:[^'\\\\\\n]|\\\\.)*'");
    private static final Pattern PAT_ERR_SQ     = Pattern.compile("'(?:[^'\\\\\\n]|\\\\.)*");
    private static final Pattern PAT_HEX        = Pattern.compile("0[xX][0-9a-fA-F]*");
    private static final Pattern PAT_FLOAT      = Pattern.compile("\\d+\\.\\d+|\\.\\d+");
    private static final Pattern PAT_INT        = Pattern.compile("\\d+");
    private static final Pattern PAT_IDENT      = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern PAT_OP2        = Pattern.compile("==|~=|<=|>=|\\.\\.");
    private static final Pattern PAT_OP1        = Pattern.compile("[-+*/%^=<>]");
    private static final Pattern PAT_TILDE      = Pattern.compile("~");
    private static final Pattern PAT_DELIM      = Pattern.compile("[(){}\\[\\],.;:]");

    private final String source;
    private final int length;
    private final List<Token> tokens = new ArrayList<>();
    private final KeywordList keywordList = new KeywordList();
    private int current = 0;
    private int line    = 1;

    public Scanner(String source) {
        this.source = source == null ? "" : source;
        this.length = this.source.length();
    }

    public List<Token> scanTokens() {
        // Single Matcher instance reused across all patterns via usePattern() / region()
        Matcher m = PAT_WHITESPACE.matcher(source);

        while (current < length) {

            // 1. Whitespace – skip and count newlines for line tracking
            if (at(m, PAT_WHITESPACE)) {
                for (char c : m.group().toCharArray()) {
                    if (c == '\n') line++;
                }
                current = m.end();
                continue;
            }

            // 2. Line comment  -- ...
            if (at(m, PAT_COMMENT)) {
                current = m.end();
                continue;
            }

            char ch = source.charAt(current);

            // 3. Double-quoted string  "..."
            if (ch == '"') {
                m.usePattern(PAT_STR_DQ).region(current, length);
                if (m.lookingAt()) {
                    addToken(TokenType.STRING_LITERAL, m.group());
                } else {
                    // Unclosed string: consume to end-of-line / EOF
                    m.usePattern(PAT_ERR_DQ).region(current, length);
                    m.lookingAt(); // always succeeds (matches at minimum the opening ")
                    addToken(TokenType.ERROR, m.group());
                }
                current = m.end();
                continue;
            }

            // 4. Single-quoted string  '...'
            if (ch == '\'') {
                m.usePattern(PAT_STR_SQ).region(current, length);
                if (m.lookingAt()) {
                    addToken(TokenType.STRING_LITERAL, m.group());
                } else {
                    m.usePattern(PAT_ERR_SQ).region(current, length);
                    m.lookingAt(); // always succeeds
                    addToken(TokenType.ERROR, m.group());
                }
                current = m.end();
                continue;
            }

            // 5. Hex literal  0xFF  (checked before integer to avoid partial match)
            if (at(m, PAT_HEX)) {
                String lex = m.group();
                current = m.end();
                // "0x" or "0X" with no trailing hex digits is an error
                addToken(lex.length() > 2 ? TokenType.HEX_LITERAL : TokenType.ERROR, lex);
                continue;
            }

            // 6. Float literal  3.14  |  .5
            if (at(m, PAT_FLOAT)) {
                current = m.end();
                addToken(TokenType.FLOAT_LITERAL, m.group());
                continue;
            }

            // 7. Integer literal
            if (at(m, PAT_INT)) {
                current = m.end();
                addToken(TokenType.INTEGER_LITERAL, m.group());
                continue;
            }

            // 8. Identifier / keyword / boolean / nil / logical operator
            if (at(m, PAT_IDENT)) {
                String text = m.group();
                current = m.end();
                TokenType type = keywordList.classify(text);
                addToken(type != null ? type : TokenType.IDENTIFIER, text);
                continue;
            }

            // 9. Two-character operators:  ==  ~=  <=  >=  ..
            if (at(m, PAT_OP2)) {
                current = m.end();
                addToken(TokenType.OPERATOR, m.group());
                continue;
            }

            // 10. Single-character operators:  +  -  *  /  %  ^  =  <  >
            if (at(m, PAT_OP1)) {
                current = m.end();
                addToken(TokenType.OPERATOR, m.group());
                continue;
            }

            // 11. Lone  ~  (not followed by  =)  → lexical error token
            if (at(m, PAT_TILDE)) {
                current = m.end();
                addToken(TokenType.ERROR, m.group());
                continue;
            }

            // 12. Delimiters:  (  )  {  }  [  ]  ,  .  ;  :
            if (at(m, PAT_DELIM)) {
                current = m.end();
                addToken(TokenType.DELIMITER, m.group());
                continue;
            }

            // 13. Unrecognised character → lexical error
            addToken(TokenType.ERROR, String.valueOf(ch));
            current++;
        }

        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    /**
     * Anchors pattern {@code p} at the current scan position using
     * {@link Matcher#region} and returns true if {@link Matcher#lookingAt} succeeds.
     */
    private boolean at(Matcher m, Pattern p) {
        m.usePattern(p).region(current, length);
        return m.lookingAt();
    }

    private void addToken(TokenType type, String lexeme) {
        tokens.add(new Token(type, lexeme, line));
    }
}
