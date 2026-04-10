import java.util.ArrayList;
import java.util.List;

public class Scanner {
    private final String source;
    private final int length;
    private final List<Token> tokens = new ArrayList<>();
    private final KeywordList keywordList = new KeywordList();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    public Scanner(String source) {
        this.source = source == null ? "" : source;
        this.length = this.source.length();
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ' ': case '\r': case '\t': case '\n':
                return;
            case '-':
                if (match('-')) {
                    skipComment();
                } else {
                    addToken(TokenType.OPERATOR, "-");
                }
                return;
            case '"':
                stringLiteral('"');
                return;
            case '\'':
                stringLiteral('\'');
                return;
            case '~':
                if (match('=')) {
                    addToken(TokenType.OPERATOR, "~=");
                } else {
                    addToken(TokenType.ERROR, "~");
                }
                return;
            case '=':
                addToken(TokenType.OPERATOR, match('=') ? "==" : "=");
                return;
            case '<':
                addToken(TokenType.OPERATOR, match('=') ? "<=" : "<");
                return;
            case '>':
                addToken(TokenType.OPERATOR, match('=') ? ">=" : ">");
                return;
            case '.':
                if (match('.')) {
                    addToken(TokenType.OPERATOR, "..");
                } else if (isDigit(peek())) {
                    scanNumber(true);
                } else {
                    addToken(TokenType.DELIMITER, ".");
                }
                return;
            case '+': case '*': case '/': case '%': case '^':
                addToken(TokenType.OPERATOR, String.valueOf(c));
                return;
            case '(': case ')': case '{': case '}': case '[': case ']': case ',': case ':': case ';':
                addToken(TokenType.DELIMITER, String.valueOf(c));
                return;
            default:
                if (isDigit(c)) {
                    scanNumber(false);
                } else if (isAlpha(c) || c == '_') {
                    scanIdentifier();
                } else {
                    addToken(TokenType.ERROR, String.valueOf(c));
                }
        }
    }

    private void skipComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private void stringLiteral(char quote) {
        while (!isAtEnd()) {
            char ch = advance();
            if (ch == '\\') {
                if (!isAtEnd()) {
                    advance();
                }
                continue;
            }
            if (ch == quote) {
                String lexeme = source.substring(start, current);
                addToken(TokenType.STRING_LITERAL, lexeme);
                return;
            }
        }
        addToken(TokenType.ERROR, source.substring(start, current));
    }

    private void scanNumber(boolean leadingDot) {
        if (!leadingDot && peekPrevious() == '0' && (peek() == 'x' || peek() == 'X')) {
            advance();
            int hexStart = current;
            while (isHexDigit(peek())) {
                advance();
            }
            if (hexStart == current) {
                addToken(TokenType.ERROR, source.substring(start, current));
            } else {
                addToken(TokenType.HEX_LITERAL, source.substring(start, current));
            }
            return;
        }

        while (isDigit(peek())) {
            advance();
        }

        boolean isFloat = leadingDot;
        if (peek() == '.' && isDigit(peekNext())) {
            isFloat = true;
            advance();
            while (isDigit(peek())) {
                advance();
            }
        }

        TokenType type = isFloat ? TokenType.FLOAT_LITERAL : TokenType.INTEGER_LITERAL;
        addToken(type, source.substring(start, current));
    }

    private void scanIdentifier() {
        while (isAlphaNumeric(peek()) || peek() == '_') {
            advance();
        }
        String text = source.substring(start, current);
        TokenType keywordType = keywordList.classify(text);
        if (keywordType != null) {
            addToken(keywordType, text);
            return;
        }
        addToken(TokenType.IDENTIFIER, text);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char advance() {
        char c = source.charAt(current);
        current++;
        if (c == '\n') {
            line++;
        }
        return c;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= length) return '\0';
        return source.charAt(current + 1);
    }

    private char peekPrevious() {
        if (current == 0) return '\0';
        return source.charAt(current - 1);
    }

    private boolean isAtEnd() {
        return current >= length;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private void addToken(TokenType type, String lexeme) {
        tokens.add(new Token(type, lexeme, line));
    }
}
